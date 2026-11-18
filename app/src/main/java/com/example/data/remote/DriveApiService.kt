package com.example.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface DriveApiService {
  companion object {
    const val FILE_FIELDS =
      "id,name,mimeType,size,modifiedTime,createdTime,trashedTime,parents,starred,trashed,shared," +
        "ownedByMe,owners(displayName,emailAddress,photoLink,me)," +
        "sharingUser(displayName,emailAddress,photoLink,me)," +
        "capabilities(canEdit,canShare,canDelete,canTrash,canRename,canMoveItemWithinDrive,canCopy,canDownload)," +
        "permissions(id,type,role,displayName,emailAddress,photoLink,allowFileDiscovery,expirationTime)," +
        "webViewLink,thumbnailLink,md5Checksum,fileExtension"
    const val LIST_FIELDS = "nextPageToken,files($FILE_FIELDS)"
  }

  @GET("drive/v3/about")
  suspend fun getAbout(
    @Query("fields") fields: String = "user(displayName,emailAddress,photoLink),storageQuota(limit,usage,usageInDrive,usageInDriveTrash)",
  ): DriveAboutResponse

  @GET("drive/v3/files")
  suspend fun listFiles(
    @Query("q") query: String,
    @Query("orderBy") orderBy: String? = null,
    @Query("pageSize") pageSize: Int = 40,
    @Query("pageToken") pageToken: String? = null,
    @Query("spaces") spaces: String = "drive",
    @Query("includeItemsFromAllDrives") includeItemsFromAllDrives: Boolean = true,
    @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
    @Query("fields") fields: String = LIST_FIELDS,
  ): DriveFileListResponse

  @GET("drive/v3/files/{fileId}")
  suspend fun getFileMetadata(
    @Path("fileId") fileId: String,
    @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
    @Query("fields") fields: String = FILE_FIELDS,
  ): DriveFileDto

  @POST("drive/v3/files")
  suspend fun createFolder(
    @Body body: DriveCreateFolderRequest,
    @Query("fields") fields: String = FILE_FIELDS,
  ): DriveFileDto

  @PATCH("drive/v3/files/{fileId}")
  suspend fun updateFileMetadata(
    @Path("fileId") fileId: String,
    @Body body: DriveUpdateMetadataRequest,
    @Query("addParents") addParents: String? = null,
    @Query("removeParents") removeParents: String? = null,
    @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
    @Query("fields") fields: String = FILE_FIELDS,
  ): DriveFileDto

  @POST("drive/v3/files/{fileId}/copy")
  suspend fun copyFile(
    @Path("fileId") fileId: String,
    @Body body: DriveCopyRequest,
    @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
    @Query("fields") fields: String = FILE_FIELDS,
  ): DriveFileDto

  @DELETE("drive/v3/files/{fileId}")
  suspend fun deleteFilePermanently(
    @Path("fileId") fileId: String,
    @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
  ): Response<Unit>

  @DELETE("drive/v3/files/trash")
  suspend fun emptyTrash(): Response<Unit>

  @GET("drive/v3/files/{fileId}/permissions")
  suspend fun listPermissions(
    @Path("fileId") fileId: String,
    @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
    @Query("fields") fields: String = "permissions(id,type,role,displayName,emailAddress,photoLink,allowFileDiscovery,expirationTime)",
  ): DrivePermissionListResponse

  @POST("drive/v3/files/{fileId}/permissions")
  suspend fun createPermission(
    @Path("fileId") fileId: String,
    @Body body: DriveCreatePermissionRequest,
    @Query("sendNotificationEmail") sendNotificationEmail: Boolean? = null,
    @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
    @Query("fields") fields: String = "id,type,role,displayName,emailAddress,photoLink,allowFileDiscovery,expirationTime",
  ): DrivePermissionDto

  @PATCH("drive/v3/files/{fileId}/permissions/{permissionId}")
  suspend fun updatePermission(
    @Path("fileId") fileId: String,
    @Path("permissionId") permissionId: String,
    @Body body: DriveUpdatePermissionRequest,
    @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
    @Query("fields") fields: String = "id,type,role,displayName,emailAddress,photoLink,allowFileDiscovery,expirationTime",
  ): DrivePermissionDto

  @DELETE("drive/v3/files/{fileId}/permissions/{permissionId}")
  suspend fun deletePermission(
    @Path("fileId") fileId: String,
    @Path("permissionId") permissionId: String,
    @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
  ): Response<Unit>

  @DELETE("drive/v3/files/{fileId}/permissions/{permissionId}")
  suspend fun deletePermissionDirect(
    @Path("fileId") fileId: String,
    @Path("permissionId") permissionId: String,
  ): Response<Unit>

  @Streaming
  @GET("drive/v3/files/{fileId}")
  suspend fun downloadFileContent(
    @Path("fileId") fileId: String,
    @Query("alt") alt: String = "media",
    @Query("supportsAllDrives") supportsAllDrives: Boolean = true,
  ): Response<ResponseBody>

  @Streaming
  @GET("drive/v3/files/{fileId}/export")
  suspend fun exportGoogleDoc(
    @Path("fileId") fileId: String,
    @Query("mimeType") exportMimeType: String,
  ): Response<ResponseBody>
}
