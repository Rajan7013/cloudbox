package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveAboutResponse(
  val user: DriveAboutUserDto? = null,
  val storageQuota: DriveStorageQuotaDto? = null,
)

@JsonClass(generateAdapter = true)
data class DriveAboutUserDto(
  val displayName: String? = null,
  val emailAddress: String? = null,
  val photoLink: String? = null,
)

@JsonClass(generateAdapter = true)
data class DriveStorageQuotaDto(
  val limit: String? = null,
  val usage: String? = null,
  val usageInDrive: String? = null,
  val usageInDriveTrash: String? = null,
)

@JsonClass(generateAdapter = true)
data class DriveFileListResponse(
  val nextPageToken: String? = null,
  val files: List<DriveFileDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DriveFileDto(
  val id: String,
  val name: String = "Untitled",
  val mimeType: String = "application/octet-stream",
  val size: String? = null,
  val modifiedTime: String? = null,
  val createdTime: String? = null,
  val trashedTime: String? = null,
  val parents: List<String>? = null,
  val starred: Boolean? = null,
  val trashed: Boolean? = null,
  val shared: Boolean? = null,
  val ownedByMe: Boolean? = null,
  val owners: List<DrivePersonDto>? = null,
  val sharingUser: DrivePersonDto? = null,
  val capabilities: DriveCapabilitiesDto? = null,
  val permissions: List<DrivePermissionDto>? = null,
  val webViewLink: String? = null,
  val thumbnailLink: String? = null,
  val md5Checksum: String? = null,
  val fileExtension: String? = null,
)

@JsonClass(generateAdapter = true)
data class DrivePersonDto(
  val displayName: String? = null,
  val emailAddress: String? = null,
  val photoLink: String? = null,
  val me: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class DriveCapabilitiesDto(
  val canEdit: Boolean? = null,
  val canShare: Boolean? = null,
  val canDelete: Boolean? = null,
  val canTrash: Boolean? = null,
  val canRename: Boolean? = null,
  val canMoveItemWithinDrive: Boolean? = null,
  val canCopy: Boolean? = null,
  val canDownload: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class DrivePermissionListResponse(
  val permissions: List<DrivePermissionDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DrivePermissionDto(
  val id: String,
  val type: String = "user",
  val role: String = "reader",
  val displayName: String? = null,
  val emailAddress: String? = null,
  val photoLink: String? = null,
  val allowFileDiscovery: Boolean? = null,
  val expirationTime: String? = null,
)

@JsonClass(generateAdapter = true)
data class DriveCreateFolderRequest(
  val name: String,
  val mimeType: String = "application/vnd.google-apps.folder",
  val parents: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class DriveUpdateMetadataRequest(
  val name: String? = null,
  val starred: Boolean? = null,
  val trashed: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class DriveCopyRequest(
  val name: String? = null,
  val parents: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class DriveCreatePermissionRequest(
  val type: String,
  val role: String,
  val emailAddress: String? = null,
  val allowFileDiscovery: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class DriveUpdatePermissionRequest(
  val role: String? = null,
  val expirationTime: String? = null,
)

@JsonClass(generateAdapter = true)
data class DriveErrorEnvelope(
  val error: DriveErrorDetail? = null,
)

@JsonClass(generateAdapter = true)
data class DriveErrorDetail(
  val code: Int? = null,
  val message: String? = null,
)
