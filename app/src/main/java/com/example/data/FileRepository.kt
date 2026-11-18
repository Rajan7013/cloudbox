package com.example.data

import android.net.Uri
import com.example.model.AccessRole
import com.example.model.CategoryStorageBreakdown
import com.example.model.CloudFile
import com.example.model.DriveUser
import com.example.model.FilterConfig
import com.example.model.SharePermission
import com.example.model.StorageQuotaInfo
import java.io.File
import kotlinx.coroutines.flow.Flow

data class PagedFilesResult(
  val files: List<CloudFile>,
  val nextPageToken: String?,
  val fromCache: Boolean = false,
)

interface FileRepository {
  suspend fun getAboutStorageAndUser(): Pair<DriveUser?, StorageQuotaInfo>

  suspend fun listFolderFiles(
    folderId: String,
    filter: FilterConfig,
    pageToken: String? = null,
    isOnline: Boolean = true,
  ): PagedFilesResult

  suspend fun listRecentFiles(isOnline: Boolean = true): List<CloudFile>

  suspend fun listAllNonTrashedForStorage(isOnline: Boolean = true): Pair<List<CategoryStorageBreakdown>, Long>

  suspend fun listStarredFiles(filter: FilterConfig, isOnline: Boolean = true): List<CloudFile>

  suspend fun listSharedWithMe(filter: FilterConfig, isOnline: Boolean = true): List<CloudFile>

  suspend fun listSharedByMe(filter: FilterConfig, isOnline: Boolean = true): List<CloudFile>

  suspend fun listTrashedFiles(isOnline: Boolean = true): List<CloudFile>

  suspend fun searchFiles(
    query: String,
    filter: FilterConfig,
    isOnline: Boolean = true,
  ): List<CloudFile>

  suspend fun createFolder(name: String, parentId: String): CloudFile

  suspend fun getOrCreateFolder(name: String, parentId: String): CloudFile

  suspend fun copyFolder(folderId: String, newName: String, parentId: String): CloudFile

  suspend fun uploadFileFromUri(
    uri: Uri,
    fileName: String,
    mimeType: String,
    parentFolderId: String,
    onProgress: (uploadedBytes: Long, totalBytes: Long) -> Unit,
    isCancelled: () -> Boolean,
  ): CloudFile

  suspend fun renameFile(fileId: String, newName: String): CloudFile

  suspend fun setStarred(fileId: String, starred: Boolean): CloudFile

  suspend fun moveFile(fileId: String, oldParentId: String?, newParentId: String): CloudFile

  suspend fun copyFile(fileId: String, newName: String?, parentId: String?): CloudFile

  suspend fun setTrashed(fileId: String, trashed: Boolean): CloudFile

  suspend fun deletePermanently(fileId: String)

  suspend fun emptyTrash()

  suspend fun listPermissions(fileId: String): List<SharePermission>

  suspend fun inviteCollaborator(fileId: String, email: String, role: AccessRole): SharePermission

  suspend fun updatePermissionRole(
    fileId: String,
    permissionId: String,
    role: AccessRole,
    parentId: String? = null,
  ): SharePermission

  suspend fun removePermission(
    fileId: String,
    permissionId: String,
    parentId: String? = null,
  )

  suspend fun setAnyoneLinkAccess(fileId: String, enabled: Boolean, role: AccessRole = AccessRole.VIEWER): List<SharePermission>

  suspend fun getFileDetails(fileId: String): CloudFile

  suspend fun downloadFileToCacheForViewer(file: CloudFile): File

  suspend fun exportFileToUri(file: CloudFile, targetUri: Uri)

  suspend fun makeAvailableOffline(file: CloudFile): CloudFile

  suspend fun removeOfflineFile(fileId: String)

  fun observeOfflineFiles(): Flow<List<CloudFile>>

  fun observeRecentSearches(): Flow<List<String>>

  suspend fun saveRecentSearch(query: String)

  suspend fun removeRecentSearch(query: String)

  fun observePdfBookmarks(fileId: String): Flow<List<Int>>

  suspend fun togglePdfBookmark(fileId: String, pageIndex: Int)

  suspend fun getViewerCacheSizeBytes(): Long

  suspend fun clearViewerCache(): Long
}
