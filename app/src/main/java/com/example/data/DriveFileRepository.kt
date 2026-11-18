package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.auth.DriveAuthManager
import com.example.auth.DriveTokenAttemptResult
import com.example.data.local.CachedFileEntity
import com.example.data.local.CloudBoxDatabase
import com.example.data.local.OfflineFileEntity
import com.example.data.local.PdfBookmarkEntity
import com.example.data.local.RecentSearchEntity
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.remote.DriveApiException
import com.example.data.remote.DriveApiService
import com.example.data.remote.DriveClientProvider
import com.example.data.remote.DriveCopyRequest
import com.example.data.remote.DriveCreateFolderRequest
import com.example.data.remote.DriveCreatePermissionRequest
import com.example.data.remote.DriveFileDto
import com.example.data.remote.DrivePermissionDto
import com.example.data.remote.DriveUpdateMetadataRequest
import com.example.data.remote.DriveUpdatePermissionRequest
import com.example.model.AccessRole
import com.example.model.CategoryStorageBreakdown
import com.example.model.CloudFile
import com.example.model.DateRangeFilter
import com.example.model.DriveUser
import com.example.model.FileCategory
import com.example.model.FilterConfig
import com.example.model.OfflineSyncStatus
import com.example.model.OwnerFilter
import com.example.model.SharePermission
import com.example.model.SortField
import com.example.model.StorageQuotaInfo
import com.example.util.FormatUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

class DriveFileRepository(
  private val context: Context,
  private val db: CloudBoxDatabase,
  private val authManager: DriveAuthManager,
  private val clientProvider: DriveClientProvider,
) : FileRepository {

  private val api: DriveApiService = clientProvider.apiService
  private val cachedDao = db.cachedFileDao()
  private val offlineDao = db.offlineFileDao()
  private val searchDao = db.recentSearchDao()
  private val bookmarkDao = db.pdfBookmarkDao()

  private fun currentUid(): String = Firebase.auth.currentUser?.uid ?: "signed_out"

  private suspend fun <T> executeDriveCall(block: suspend () -> T): T {
    if (authManager.currentTokenOrNull().isNullOrBlank()) {
      when (val res = authManager.requestDriveAuthorization(context)) {
        is DriveTokenAttemptResult.Token -> authManager.setToken(res.accessToken)
        is DriveTokenAttemptResult.NeedsConsent ->
          throw DriveApiException(401, "Google Drive permission required.", isAuthExpired = true)
        is DriveTokenAttemptResult.Error ->
          throw DriveApiException(401, res.message, isAuthExpired = true)
      }
    }
    return try {
      block()
    } catch (http: HttpException) {
      val code = http.code()
      if (code == 401) {
        authManager.clearToken()
        val refreshed = authManager.requestDriveAuthorization(context)
        if (refreshed is DriveTokenAttemptResult.Token) {
          try {
            return block()
          } catch (retryHttp: HttpException) {
            val retryBody = retryHttp.response()?.errorBody()?.string()
            val parsedMsg = clientProvider.parseErrorMessage(retryBody)
            throw DriveApiException(
              retryHttp.code(),
              parsedMsg ?: DriveClientProvider.friendlyMessageForCode(retryHttp.code()),
            )
          }
        }
        throw DriveApiException(401, "Your Google Drive session expired. Please reconnect.", isAuthExpired = true)
      }
      val errBody = http.response()?.errorBody()?.string()
      Log.w("DriveRepo", "Drive HTTP $code: $errBody")
      val parsedMsg = clientProvider.parseErrorMessage(errBody)
      throw DriveApiException(code, parsedMsg ?: DriveClientProvider.friendlyMessageForCode(code))
    } catch (e: DriveApiException) {
      throw e
    } catch (e: IOException) {
      throw DriveApiException(0, e.localizedMessage ?: "Network error while contacting Google Drive.", isAuthExpired = false)
    }
  }

  override suspend fun getAboutStorageAndUser(): Pair<DriveUser?, StorageQuotaInfo> =
    withContext(Dispatchers.IO) {
      val about = executeDriveCall { api.getAbout() }
      val uid = currentUid()
      val user =
        about.user?.let {
          DriveUser(
            uid = uid,
            displayName = it.displayName ?: Firebase.auth.currentUser?.displayName ?: "CloudBox User",
            email = it.emailAddress ?: Firebase.auth.currentUser?.email.orEmpty(),
            photoUrl = it.photoLink ?: Firebase.auth.currentUser?.photoUrl?.toString(),
          )
        }
      val quotaDto = about.storageQuota
      val quota =
        StorageQuotaInfo(
          limitBytes = quotaDto?.limit?.toLongOrNull() ?: (15L * 1024L * 1024L * 1024L),
          usageBytes = quotaDto?.usage?.toLongOrNull() ?: 0L,
          usageInDriveBytes = quotaDto?.usageInDrive?.toLongOrNull() ?: 0L,
          usageInTrashBytes = quotaDto?.usageInDriveTrash?.toLongOrNull() ?: 0L,
        )
      user to quota
    }

  override suspend fun listFolderFiles(
    folderId: String,
    filter: FilterConfig,
    pageToken: String?,
    isOnline: Boolean,
  ): PagedFilesResult =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offlineMap = offlineDao.getAllOnce(uid).associateBy { it.fileId }
      if (!isOnline) {
        val cached = cachedDao.getFolderOnce(uid, folderId).map { it.toDomain(offlineMap[it.id]) }
        return@withContext PagedFilesResult(
          files = applyClientFiltersAndSort(cached, filter),
          nextPageToken = null,
          fromCache = true,
        )
      }

      val q = "'$folderId' in parents and trashed = false"
      val orderBy = buildOrderBy(filter.sortField, filter.ascending)
      val response = executeDriveCall {
        api.listFiles(query = q, orderBy = orderBy, pageSize = 40, pageToken = pageToken)
      }
      val domainItems = response.files.map { it.toDomain(offlineMap[it.id], fallbackParentId = folderId) }
      cachedDao.upsertAll(domainItems.map { it.toEntity(uid, isSharedWithMe = false) })
      PagedFilesResult(
        files = applyClientFiltersAndSort(domainItems, filter),
        nextPageToken = response.nextPageToken,
        fromCache = false,
      )
    }

  override suspend fun listRecentFiles(isOnline: Boolean): List<CloudFile> =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offlineMap = offlineDao.getAllOnce(uid).associateBy { it.fileId }
      if (!isOnline) {
        return@withContext cachedDao.getRecentFiles(uid).map { it.toDomain(offlineMap[it.id]) }
      }
      val q = "trashed = false and mimeType != 'application/vnd.google-apps.folder'"
      val response = executeDriveCall {
        api.listFiles(query = q, orderBy = "modifiedTime desc", pageSize = 12)
      }
      val items = response.files.map { it.toDomain(offlineMap[it.id]) }
      cachedDao.upsertAll(items.map { it.toEntity(uid, isSharedWithMe = !it.ownedByMe) })
      items
    }

  override suspend fun listAllNonTrashedForStorage(
    isOnline: Boolean,
  ): Pair<List<CategoryStorageBreakdown>, Long> =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val allFiles: List<CloudFile> =
        if (!isOnline) {
          cachedDao.getRecentFiles(uid)
            .plus(cachedDao.getFolderOnce(uid, "root"))
            .distinctBy { it.id }
            .map { it.toDomain() }
        } else {
          val collected = mutableListOf<CloudFile>()
          var next: String? = null
          var pages = 0
          do {
            val resp = executeDriveCall {
              api.listFiles(
                query = "trashed = false and 'me' in owners",
                orderBy = "quotaBytesUsed desc",
                pageSize = 100,
                pageToken = next,
              )
            }
            collected += resp.files.map { it.toDomain(null) }
            next = resp.nextPageToken
            pages++
          } while (next != null && pages < 5)
          collected
        }

      val grouped =
        allFiles
          .filter { !it.isFolder && it.sizeBytes > 0L }
          .groupBy { it.category }
          .map { (cat, list) ->
            CategoryStorageBreakdown(
              category = cat,
              bytes = list.sumOf { it.sizeBytes },
              fileCount = list.size,
            )
          }
          .sortedByDescending { it.bytes }

      val trashedBytes =
        if (isOnline) {
          val trashResp = executeDriveCall {
            api.listFiles(query = "trashed = true", pageSize = 100)
          }
          trashResp.files.sumOf { it.size?.toLongOrNull() ?: 0L }
        } else {
          0L
        }

      grouped to trashedBytes
    }

  override suspend fun listStarredFiles(filter: FilterConfig, isOnline: Boolean): List<CloudFile> =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offlineMap = offlineDao.getAllOnce(uid).associateBy { it.fileId }
      if (!isOnline) {
        val cached = cachedDao.getStarredOnce(uid).map { it.toDomain(offlineMap[it.id]) }
        return@withContext applyClientFiltersAndSort(cached, filter)
      }
      val resp = executeDriveCall {
        api.listFiles(
          query = "starred = true and trashed = false",
          orderBy = buildOrderBy(filter.sortField, filter.ascending),
          pageSize = 60,
        )
      }
      val items = resp.files.map { it.toDomain(offlineMap[it.id]) }
      cachedDao.upsertAll(items.map { it.toEntity(uid, isSharedWithMe = !it.ownedByMe) })
      applyClientFiltersAndSort(items, filter)
    }

  override suspend fun listSharedWithMe(filter: FilterConfig, isOnline: Boolean): List<CloudFile> =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offlineMap = offlineDao.getAllOnce(uid).associateBy { it.fileId }
      if (!isOnline) {
        val cached = cachedDao.getSharedWithMeOnce(uid).map { it.toDomain(offlineMap[it.id]) }
        return@withContext applyClientFiltersAndSort(cached, filter)
      }
      val resp = executeDriveCall {
        try {
          api.listFiles(
            query = "sharedWithMe = true and trashed = false",
            orderBy = "sharedWithMeTime desc",
            pageSize = 60,
          )
        } catch (e: Exception) {
          api.listFiles(
            query = "trashed = false and not 'me' in owners",
            pageSize = 60,
          )
        }
      }
      val items = resp.files.map { it.toDomain(offlineMap[it.id]) }
      cachedDao.upsertAll(items.map { it.toEntity(uid, isSharedWithMe = true) })
      applyClientFiltersAndSort(items, filter)
    }

  override suspend fun listSharedByMe(filter: FilterConfig, isOnline: Boolean): List<CloudFile> =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offlineMap = offlineDao.getAllOnce(uid).associateBy { it.fileId }
      if (!isOnline) {
        val cached = cachedDao.getSharedByMeOnce(uid).map { it.toDomain(offlineMap[it.id]) }
        return@withContext applyClientFiltersAndSort(cached, filter)
      }
      val resp = executeDriveCall {
        try {
          api.listFiles(
            query = "'me' in owners and trashed = false and shared = true",
            orderBy = buildOrderBy(filter.sortField, filter.ascending),
            pageSize = 80,
          )
        } catch (e: Exception) {
          api.listFiles(
            query = "'me' in owners and trashed = false",
            orderBy = buildOrderBy(filter.sortField, filter.ascending),
            pageSize = 80,
          )
        }
      }
      val items =
        resp.files
          .filter { dto ->
            dto.shared == true ||
              (dto.permissions?.any { p -> p.type == "anyone" || p.role != "owner" } == true)
          }
          .map { it.toDomain(offlineMap[it.id]) }
      cachedDao.upsertAll(items.map { it.toEntity(uid, isSharedWithMe = false) })
      applyClientFiltersAndSort(items, filter)
    }

  override suspend fun listTrashedFiles(isOnline: Boolean): List<CloudFile> =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offlineMap = offlineDao.getAllOnce(uid).associateBy { it.fileId }
      if (!isOnline) {
        return@withContext cachedDao.getTrashedOnce(uid).map { it.toDomain(offlineMap[it.id]) }
      }
      val resp = executeDriveCall {
        api.listFiles(query = "trashed = true", orderBy = "modifiedTime desc", pageSize = 80)
      }
      val items = resp.files.map { it.toDomain(offlineMap[it.id]) }
      cachedDao.upsertAll(items.map { it.toEntity(uid, isSharedWithMe = !it.ownedByMe) })
      items
    }

  override suspend fun searchFiles(
    query: String,
    filter: FilterConfig,
    isOnline: Boolean,
  ): List<CloudFile> =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offlineMap = offlineDao.getAllOnce(uid).associateBy { it.fileId }
      val trimmed = query.trim()
      if (!isOnline) {
        val localMatches =
          if (trimmed.isEmpty()) {
            cachedDao.getRecentFiles(uid)
          } else {
            cachedDao.searchLocal(uid, trimmed)
          }
        return@withContext applyClientFiltersAndSort(localMatches.map { it.toDomain(offlineMap[it.id]) }, filter)
      }

      val escaped = trimmed.replace("\\", "\\\\").replace("'", "\\'")
      val clauses = mutableListOf("trashed = false")
      if (escaped.isNotEmpty()) {
        clauses += "name contains '$escaped'"
      }
      if (filter.ownerFilter == OwnerFilter.ME) {
        clauses += "'me' in owners"
      } else if (filter.ownerFilter == OwnerFilter.SHARED_WITH_ME) {
        clauses += "sharedWithMe = true"
      }
      val resp = executeDriveCall {
        api.listFiles(
          query = clauses.joinToString(" and "),
          orderBy = buildOrderBy(filter.sortField, filter.ascending),
          pageSize = 60,
        )
      }
      val items = resp.files.map { it.toDomain(offlineMap[it.id]) }
      applyClientFiltersAndSort(items, filter)
    }

  override suspend fun createFolder(name: String, parentId: String): CloudFile =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val existingInFolder = cachedDao.getFolderOnce(uid, parentId)
      if (existingInFolder.any { it.categoryName == FileCategory.FOLDER.name && it.name.equals(name.trim(), ignoreCase = true) }) {
        throw IllegalArgumentException("A folder with this name already exists")
      }
      val dto = executeDriveCall {
        api.createFolder(
          DriveCreateFolderRequest(
            name = name.trim(),
            parents = if (parentId == "root") null else listOf(parentId),
          )
        )
      }
      val domain = dto.toDomain(null, fallbackParentId = parentId)
      cachedDao.upsert(domain.toEntity(uid, isSharedWithMe = false))
      domain
    }

  override suspend fun getOrCreateFolder(name: String, parentId: String): CloudFile =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val trimmed = name.trim()
      val existingInFolder = cachedDao.getFolderOnce(uid, parentId)
      val match = existingInFolder.firstOrNull {
        it.categoryName == FileCategory.FOLDER.name && it.name.equals(trimmed, ignoreCase = true)
      }
      if (match != null) {
        val offline = offlineDao.getByFileId(uid, match.id)
        return@withContext match.toDomain(offline)
      }
      try {
        createFolder(trimmed, parentId)
      } catch (e: IllegalArgumentException) {
        val fallback = cachedDao.getFolderOnce(uid, parentId).firstOrNull {
          it.categoryName == FileCategory.FOLDER.name && it.name.equals(trimmed, ignoreCase = true)
        }
        fallback?.let {
          val offline = offlineDao.getByFileId(uid, it.id)
          it.toDomain(offline)
        } ?: throw e
      }
    }

  override suspend fun copyFolder(folderId: String, newName: String, parentId: String): CloudFile =
    withContext(Dispatchers.IO) {
      val targetParent = if (parentId.isBlank()) "root" else parentId
      val newFolder = createFolder(newName, targetParent)
      copyFolderContentsInternal(folderId, newFolder.id)
      newFolder
    }

  private suspend fun copyFolderContentsInternal(sourceFolderId: String, targetFolderId: String) {
    var pageToken: String? = null
    do {
      val result = listFolderFiles(sourceFolderId, FilterConfig(), pageToken = pageToken, isOnline = true)
      for (item in result.files) {
        if (item.isFolder) {
          val subFolder = createFolder(item.name, targetFolderId)
          copyFolderContentsInternal(item.id, subFolder.id)
        } else {
          copyFile(item.id, item.name, targetFolderId)
        }
      }
      pageToken = result.nextPageToken
    } while (!pageToken.isNullOrBlank())
  }



  override suspend fun uploadFileFromUri(
    uri: Uri,
    fileName: String,
    mimeType: String,
    parentFolderId: String,
    onProgress: (uploadedBytes: Long, totalBytes: Long) -> Unit,
    isCancelled: () -> Boolean,
  ): CloudFile =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val dto = executeDriveCall {
        clientProvider.uploadUriResumable(
          uri = uri,
          fileName = fileName,
          mimeType = mimeType,
          parentFolderId = parentFolderId,
          onProgress = onProgress,
          isCancelled = isCancelled,
        )
      }
      val domain = dto.toDomain(null, fallbackParentId = parentFolderId)
      cachedDao.upsert(domain.toEntity(uid, isSharedWithMe = false))
      domain
    }

  override suspend fun renameFile(fileId: String, newName: String): CloudFile =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offline = offlineDao.getByFileId(uid, fileId)
      val dto = executeDriveCall {
        api.updateFileMetadata(fileId, DriveUpdateMetadataRequest(name = newName.trim()))
      }
      val domain = dto.toDomain(offline)
      cachedDao.upsert(domain.toEntity(uid, isSharedWithMe = !domain.ownedByMe))
      domain
    }

  override suspend fun setStarred(fileId: String, starred: Boolean): CloudFile =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offline = offlineDao.getByFileId(uid, fileId)
      val dto = executeDriveCall {
        api.updateFileMetadata(fileId, DriveUpdateMetadataRequest(starred = starred))
      }
      val domain = dto.toDomain(offline)
      cachedDao.upsert(domain.toEntity(uid, isSharedWithMe = !domain.ownedByMe))
      domain
    }

  override suspend fun moveFile(fileId: String, oldParentId: String?, newParentId: String): CloudFile =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offline = offlineDao.getByFileId(uid, fileId)
      val dto = executeDriveCall {
        api.updateFileMetadata(
          fileId = fileId,
          body = DriveUpdateMetadataRequest(),
          addParents = newParentId,
          removeParents = oldParentId?.takeIf { it.isNotBlank() && it != newParentId },
        )
      }
      val domain = dto.toDomain(offline, fallbackParentId = newParentId)
      cachedDao.upsert(domain.toEntity(uid, isSharedWithMe = !domain.ownedByMe))
      domain
    }

  override suspend fun copyFile(fileId: String, newName: String?, parentId: String?): CloudFile =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val dto = executeDriveCall {
        api.copyFile(
          fileId = fileId,
          body = DriveCopyRequest(
            name = newName,
            parents = parentId?.takeIf { it.isNotBlank() }?.let { listOf(it) },
          ),
        )
      }
      val domain = dto.toDomain(null, fallbackParentId = parentId)
      cachedDao.upsert(domain.toEntity(uid, isSharedWithMe = false))
      domain
    }



  override suspend fun setTrashed(fileId: String, trashed: Boolean): CloudFile =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offline = offlineDao.getByFileId(uid, fileId)
      val dto = executeDriveCall {
        api.updateFileMetadata(fileId, DriveUpdateMetadataRequest(trashed = trashed))
      }
      val domain = dto.toDomain(offline)
      cachedDao.upsert(domain.toEntity(uid, isSharedWithMe = !domain.ownedByMe))
      domain
    }

  override suspend fun deletePermanently(fileId: String) =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      executeDriveCall {
        val resp = api.deleteFilePermanently(fileId)
        if (!resp.isSuccessful) throw HttpException(resp)
      }
      cachedDao.deleteById(uid, fileId)
      removeOfflineFile(fileId)
    }

  override suspend fun emptyTrash() =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      executeDriveCall {
        val resp = api.emptyTrash()
        if (!resp.isSuccessful) throw HttpException(resp)
      }
      cachedDao.deleteAllTrashed(uid)
    }

  override suspend fun listPermissions(fileId: String): List<SharePermission> =
    withContext(Dispatchers.IO) {
      val resp = executeDriveCall { api.listPermissions(fileId) }
      resp.permissions.map { it.toSharePermission() }
    }

  override suspend fun inviteCollaborator(fileId: String, email: String, role: AccessRole): SharePermission =
    withContext(Dispatchers.IO) {
      val dto = executeDriveCall {
        api.createPermission(
          fileId = fileId,
          body = DriveCreatePermissionRequest(
            type = "user",
            role = role.apiRole,
            emailAddress = email.trim(),
          ),
          sendNotificationEmail = true,
        )
      }
      dto.toSharePermission()
    }

  override suspend fun updatePermissionRole(
    fileId: String,
    permissionId: String,
    role: AccessRole,
    parentId: String?,
  ): SharePermission =
    withContext(Dispatchers.IO) {
      val trimmedId = permissionId.trim()
      suspend fun tryPatch(targetId: String): DrivePermissionDto {
        return try {
          api.updatePermission(
            fileId = targetId,
            permissionId = trimmedId,
            body = DriveUpdatePermissionRequest(role = role.apiRole),
            supportsAllDrives = true,
          )
        } catch (_: Exception) {
          api.updatePermissionDirect(
            fileId = targetId,
            permissionId = trimmedId,
            body = DriveUpdatePermissionRequest(role = role.apiRole),
          )
        }
      }

      val dto = executeDriveCall {
        try {
          tryPatch(fileId)
        } catch (e: Exception) {
          val errorText = when (e) {
            is HttpException -> {
              val raw = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
              clientProvider.parseErrorMessage(raw) ?: e.message()
            }
            is DriveApiException -> e.message.orEmpty()
            else -> e.localizedMessage.orEmpty()
          }

          val isInherited = errorText.contains("inherited", ignoreCase = true) ||
            errorText.contains("limited access", ignoreCase = true) ||
            errorText.contains("cannot delete the permission", ignoreCase = true) ||
            errorText.contains("cannot modify the permission", ignoreCase = true)

          if (isInherited && !parentId.isNullOrBlank() && parentId != "root") {
            try {
              return@executeDriveCall tryPatch(parentId)
            } catch (parentEx: Exception) {
              Log.w("DriveRepo", "Cascade updatePermission to parent $parentId failed", parentEx)
            }
          }

          if (isInherited) {
            throw DriveApiException(
              400,
              "This person's access is inherited from the parent folder. Please open the parent folder to change their role."
            )
          }
          throw e
        }
      }
      dto.toSharePermission()
    }

  override suspend fun removePermission(
    fileId: String,
    permissionId: String,
    parentId: String?,
  ) {
    withContext(Dispatchers.IO) {
      val trimmedId = permissionId.trim()
      executeDriveCall {
        suspend fun tryDelete(targetId: String): Response<Unit> {
          val r = try {
            api.deletePermission(targetId, trimmedId, supportsAllDrives = true)
          } catch (_: Exception) { null }
          if (r != null && r.isSuccessful) return r
          return api.deletePermissionDirect(targetId, trimmedId)
        }

        val resp = tryDelete(fileId)
        if (!resp.isSuccessful) {
          val raw = resp.errorBody()?.string()
          val msg = clientProvider.parseErrorMessage(raw) ?: DriveClientProvider.friendlyMessageForCode(resp.code())

          val isInherited = msg.contains("inherited", ignoreCase = true) ||
            msg.contains("limited access", ignoreCase = true) ||
            msg.contains("cannot delete the permission", ignoreCase = true)

          // If permission is inherited from parent folder, cascade delete to parent folder
          if (isInherited && !parentId.isNullOrBlank() && parentId != "root") {
            val parentResp = tryDelete(parentId)
            if (parentResp.isSuccessful) return@executeDriveCall
          }

          if (isInherited) {
            throw DriveApiException(
              resp.code(),
              "This person's access is inherited from the parent folder. To remove them, open the parent folder and remove access there."
            )
          }

          throw DriveApiException(resp.code(), msg)
        }
      }
    }
  }

  override suspend fun setAnyoneLinkAccess(
    fileId: String,
    enabled: Boolean,
    role: AccessRole,
  ): List<SharePermission> =
    withContext(Dispatchers.IO) {
      val current = listPermissions(fileId)
      val existingAnyone = current.firstOrNull { it.type == "anyone" }
      if (enabled) {
        if (existingAnyone == null) {
          executeDriveCall {
            api.createPermission(
              fileId = fileId,
              body = DriveCreatePermissionRequest(type = "anyone", role = role.apiRole, allowFileDiscovery = false),
            )
          }
        } else {
          updatePermissionRole(fileId, existingAnyone.id, role)
        }
      } else if (existingAnyone != null) {
        removePermission(fileId, existingAnyone.id)
      }
      listPermissions(fileId)
    }

  override suspend fun getFileDetails(fileId: String): CloudFile =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offline = offlineDao.getByFileId(uid, fileId)
      val dto = executeDriveCall { api.getFileMetadata(fileId) }
      val domain = dto.toDomain(offline)
      cachedDao.upsert(domain.toEntity(uid, isSharedWithMe = !domain.ownedByMe))
      domain
    }

  override suspend fun downloadFileToCacheForViewer(file: CloudFile): File =
    withContext(Dispatchers.IO) {
      val localOffline = file.localOfflinePath?.let { File(it) }
      if (localOffline != null && localOffline.exists() && localOffline.length() > 0L) {
        return@withContext localOffline
      }
      val cacheDir = File(context.cacheDir, "viewer_cache").apply { mkdirs() }
      val targetName = "${file.id}_${sanitizeFileName(effectiveDisplayFileName(file))}"
      val cachedFile = File(cacheDir, targetName)
      if (cachedFile.exists() && cachedFile.length() > 0L) {
        return@withContext cachedFile
      }
      streamDriveFileToTarget(file, cachedFile)
      cachedFile
    }

  override suspend fun exportFileToUri(file: CloudFile, targetUri: Uri) {
    withContext(Dispatchers.IO) {
      val cached = downloadFileToCacheForViewer(file)
      val outStream =
        context.contentResolver.openOutputStream(targetUri)
          ?: throw IOException("Could not open destination file for saving.")
      outStream.use { out ->
        cached.inputStream().use { input ->
          input.copyTo(out)
        }
      }
    }
  }

  override suspend fun makeAvailableOffline(file: CloudFile): CloudFile =
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val offlineDir = File(context.filesDir, "offline").apply { mkdirs() }
      val safeName = sanitizeFileName(effectiveDisplayFileName(file))
      val destFile = File(offlineDir, "${file.id}_$safeName")

      offlineDao.upsert(
        OfflineFileEntity(
          fileId = file.id,
          ownerUid = uid,
          name = file.name,
          mimeType = file.mimeType,
          categoryName = file.category.name,
          sizeBytes = file.sizeBytes,
          localPath = destFile.absolutePath,
          statusName = OfflineSyncStatus.SYNCING.name,
          syncedAtEpochMs = System.currentTimeMillis(),
          remoteModifiedAtMs = file.modifiedTimeEpochMs,
        )
      )

      try {
        streamDriveFileToTarget(file, destFile)
        val savedSize = destFile.length().coerceAtLeast(file.sizeBytes)
        offlineDao.upsert(
          OfflineFileEntity(
            fileId = file.id,
            ownerUid = uid,
            name = file.name,
            mimeType = file.mimeType,
            categoryName = file.category.name,
            sizeBytes = savedSize,
            localPath = destFile.absolutePath,
            statusName = OfflineSyncStatus.SAVED.name,
            syncedAtEpochMs = System.currentTimeMillis(),
            remoteModifiedAtMs = file.modifiedTimeEpochMs,
          )
        )
        file.copy(
          offlineStatus = OfflineSyncStatus.SAVED,
          localOfflinePath = destFile.absolutePath,
          sizeBytes = savedSize,
        )
      } catch (e: Exception) {
        offlineDao.upsert(
          OfflineFileEntity(
            fileId = file.id,
            ownerUid = uid,
            name = file.name,
            mimeType = file.mimeType,
            categoryName = file.category.name,
            sizeBytes = file.sizeBytes,
            localPath = destFile.absolutePath,
            statusName = OfflineSyncStatus.FAILED.name,
            syncedAtEpochMs = System.currentTimeMillis(),
            remoteModifiedAtMs = file.modifiedTimeEpochMs,
          )
        )
        throw e
      }
    }

  override suspend fun removeOfflineFile(fileId: String) {
    withContext(Dispatchers.IO) {
      val uid = currentUid()
      val existing = offlineDao.getByFileId(uid, fileId)
      existing?.localPath?.let { path ->
        runCatching { File(path).delete() }
      }
      offlineDao.delete(uid, fileId)
    }
  }

  override fun observeOfflineFiles(): Flow<List<CloudFile>> {
    val uid = currentUid()
    return offlineDao.observeAll(uid).map { list ->
      list.map { offline ->
        val cached = cachedDao.getById(uid, offline.fileId)
        val status = runCatching { OfflineSyncStatus.valueOf(offline.statusName) }.getOrDefault(OfflineSyncStatus.SAVED)
        val resolvedStatus =
          if (status == OfflineSyncStatus.SAVED && !File(offline.localPath).exists()) {
            OfflineSyncStatus.FAILED
          } else {
            status
          }
        cached?.toDomain(offline.copy(statusName = resolvedStatus.name))
          ?: CloudFile(
            id = offline.fileId,
            name = offline.name,
            mimeType = offline.mimeType,
            category = runCatching { FileCategory.valueOf(offline.categoryName) }.getOrDefault(FileCategory.OTHER),
            sizeBytes = offline.sizeBytes,
            modifiedTimeEpochMs = offline.remoteModifiedAtMs,
            createdTimeEpochMs = offline.syncedAtEpochMs,
            offlineStatus = resolvedStatus,
            localOfflinePath = offline.localPath,
          )
      }
    }
  }

  override fun observeRecentSearches(): Flow<List<String>> =
    searchDao.observeRecent(currentUid()).map { list -> list.map { it.queryText } }

  override suspend fun saveRecentSearch(query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    val uid = currentUid()
    searchDao.upsert(
      RecentSearchEntity(
        queryKey = "${uid}_${trimmed.lowercase()}",
        ownerUid = uid,
        queryText = trimmed,
        searchedAtMs = System.currentTimeMillis(),
      )
    )
  }

  override suspend fun removeRecentSearch(query: String) {
    val uid = currentUid()
    searchDao.delete("${uid}_${query.trim().lowercase()}")
  }

  override fun observePdfBookmarks(fileId: String): Flow<List<Int>> =
    bookmarkDao.observeBookmarks(currentUid(), fileId)

  override suspend fun togglePdfBookmark(fileId: String, pageIndex: Int) {
    val uid = currentUid()
    val key = "${uid}_${fileId}_$pageIndex"
    val existing = bookmarkDao.observeBookmarks(uid, fileId).firstOrNull() ?: emptyList()
    if (pageIndex in existing) {
      bookmarkDao.delete(key)
    } else {
      bookmarkDao.insert(
        PdfBookmarkEntity(
          bookmarkKey = key,
          ownerUid = uid,
          fileId = fileId,
          pageIndex = pageIndex,
          createdAtMs = System.currentTimeMillis(),
        )
      )
    }
  }

  override suspend fun getViewerCacheSizeBytes(): Long =
    withContext(Dispatchers.IO) {
      val viewerDir = File(context.cacheDir, "viewer_cache")
      val captureDir = File(context.cacheDir, "captured_photos")
      dirSize(viewerDir) + dirSize(captureDir)
    }

  override suspend fun clearViewerCache(): Long =
    withContext(Dispatchers.IO) {
      val freed = getViewerCacheSizeBytes()
      File(context.cacheDir, "viewer_cache").deleteRecursively()
      File(context.cacheDir, "captured_photos").deleteRecursively()
      freed
    }

  private fun dirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
  }

  private suspend fun streamDriveFileToTarget(file: CloudFile, targetFile: File) {
    val response = executeDriveCall {
      when {
        file.mimeType == "application/vnd.google-apps.spreadsheet" ->
          api.exportGoogleDoc(file.id, "text/csv")
        file.mimeType == "application/vnd.google-apps.document" ||
          file.mimeType == "application/vnd.google-apps.presentation" ->
          api.exportGoogleDoc(file.id, "application/pdf")
        else -> api.downloadFileContent(file.id)
      }
    }
    if (!response.isSuccessful) {
      throw HttpException(response)
    }
    val body = response.body() ?: throw IOException("Empty file stream from Google Drive.")
    targetFile.parentFile?.mkdirs()
    val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
    body.byteStream().use { input ->
      FileOutputStream(tempFile).use { output ->
        input.copyTo(output)
      }
    }
    if (targetFile.exists()) targetFile.delete()
    tempFile.renameTo(targetFile)
  }

  private fun effectiveDisplayFileName(file: CloudFile): String =
    when (file.mimeType) {
      "application/vnd.google-apps.spreadsheet" -> "${file.name}.csv"
      "application/vnd.google-apps.document", "application/vnd.google-apps.presentation" -> "${file.name}.pdf"
      else -> file.name
    }

  private fun sanitizeFileName(name: String): String =
    name.replace(Regex("[^a-zA-Z0-9._-]"), "_")

  private fun buildOrderBy(field: SortField, ascending: Boolean): String {
    val dir = if (ascending) "" else " desc"
    val primary =
      when (field) {
        SortField.MODIFIED -> "modifiedTime$dir"
        SortField.NAME -> "name_natural$dir"
        SortField.SIZE -> "quotaBytesUsed$dir"
        SortField.TYPE -> "mimeType$dir"
      }
    return "folder,$primary"
  }

  private fun applyClientFiltersAndSort(items: List<CloudFile>, filter: FilterConfig): List<CloudFile> {
    val now = System.currentTimeMillis()
    val filtered =
      items.filter { file ->
        val catOk = filter.categories.isEmpty() || file.category in filter.categories
        val ownerOk =
          when (filter.ownerFilter) {
            OwnerFilter.ANYONE -> true
            OwnerFilter.ME -> file.ownedByMe
            OwnerFilter.SHARED_WITH_ME -> !file.ownedByMe
          }
        val dateOk =
          when (filter.dateRange) {
            DateRangeFilter.ANY_TIME -> true
            DateRangeFilter.TODAY -> now - file.modifiedTimeEpochMs <= 24L * 60L * 60L * 1000L
            DateRangeFilter.LAST_7_DAYS -> now - file.modifiedTimeEpochMs <= 7L * 24L * 60L * 60L * 1000L
            DateRangeFilter.LAST_30_DAYS -> now - file.modifiedTimeEpochMs <= 30L * 24L * 60L * 60L * 1000L
          }
        catOk && ownerOk && dateOk
      }

    val comparator =
      Comparator<CloudFile> { a, b ->
        if (a.isFolder != b.isFolder) {
          return@Comparator if (a.isFolder) -1 else 1
        }
        val cmp =
          when (filter.sortField) {
            SortField.MODIFIED -> a.modifiedTimeEpochMs.compareTo(b.modifiedTimeEpochMs)
            SortField.NAME -> a.name.lowercase().compareTo(b.name.lowercase())
            SortField.SIZE -> a.sizeBytes.compareTo(b.sizeBytes)
            SortField.TYPE -> a.category.label.compareTo(b.category.label)
          }
        if (filter.ascending) cmp else -cmp
      }
    return filtered.sortedWith(comparator)
  }

  private fun DriveFileDto.toDomain(
    offlineEntity: OfflineFileEntity?,
    fallbackParentId: String? = null,
  ): CloudFile {
    val category = FileCategory.fromMimeType(mimeType, name)
    val owner = owners?.firstOrNull()
    val isOwned = ownedByMe ?: (owner?.me == true)
    val myPerms = permissions.orEmpty()
    val anyonePerm = myPerms.firstOrNull { it.type == "anyone" }
    val userRole =
      when {
        isOwned -> AccessRole.OWNER
        capabilities?.canEdit == true -> AccessRole.EDITOR
        else -> AccessRole.VIEWER
      }
    val offlineStatus =
      offlineEntity?.let {
        runCatching { OfflineSyncStatus.valueOf(it.statusName) }.getOrDefault(OfflineSyncStatus.NONE)
      } ?: OfflineSyncStatus.NONE

    return CloudFile(
      id = id,
      name = name,
      mimeType = mimeType,
      category = category,
      sizeBytes = size?.toLongOrNull() ?: 0L,
      modifiedTimeEpochMs = FormatUtils.parseIso8601ToEpochMs(modifiedTime),
      createdTimeEpochMs = FormatUtils.parseIso8601ToEpochMs(createdTime),
      trashedTimeEpochMs = FormatUtils.parseIso8601ToEpochMs(trashedTime).takeIf { it > 0L },
      parentId = parents?.firstOrNull() ?: fallbackParentId,
      starred = starred ?: false,
      trashed = trashed ?: false,
      shared = shared ?: false,
      ownedByMe = isOwned,
      ownerName = if (isOwned) "You" else (owner?.displayName ?: sharingUser?.displayName ?: "Collaborator"),
      ownerEmail = owner?.emailAddress ?: sharingUser?.emailAddress ?: "",
      ownerPhotoUrl = owner?.photoLink ?: sharingUser?.photoLink,
      sharingUserName = sharingUser?.displayName,
      sharingUserEmail = sharingUser?.emailAddress,
      userRole = userRole,
      canEdit = capabilities?.canEdit ?: isOwned,
      canShare = capabilities?.canShare ?: isOwned,
      canDelete = isOwned, // Enforce strictly: only the original owner can delete files or folders
      webViewLink = webViewLink,
      thumbnailLink = thumbnailLink,
      md5Checksum = md5Checksum,
      fileExtension = fileExtension,
      offlineStatus = offlineStatus,
      localOfflinePath = offlineEntity?.localPath,
      isAnyoneWithLink = anyonePerm != null,
      collaboratorCount = myPerms.count { it.type != "anyone" },
    )
  }

  private fun DrivePermissionDto.toSharePermission(): SharePermission {
    val inheritedDetail = permissionDetails?.firstOrNull { it.inherited == true }
    return SharePermission(
      id = id,
      type = type,
      role = AccessRole.fromApi(role),
      displayName = displayName,
      emailAddress = emailAddress,
      photoLink = photoLink,
      allowFileDiscovery = allowFileDiscovery ?: false,
      expirationTime = expirationTime,
      isInherited = inheritedDetail != null,
      inheritedFrom = inheritedDetail?.inheritedFrom,
    )
  }
}
