package com.example.model

enum class FileCategory(val label: String) {
  FOLDER("Folder"),
  DOCUMENT("Document"),
  SPREADSHEET("Sheet"),
  PDF("PDF"),
  IMAGE("Image"),
  VIDEO("Video"),
  AUDIO("Audio"),
  ARCHIVE("Archive"),
  OTHER("File");

  companion object {
    fun fromMimeType(mimeType: String, fileName: String = ""): FileCategory {
      val lowerMime = mimeType.lowercase()
      val ext = fileName.substringAfterLast('.', "").lowercase()
      return when {
        lowerMime == "application/vnd.google-apps.folder" -> FOLDER
        lowerMime == "application/pdf" || ext == "pdf" -> PDF
        lowerMime.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "bmp", "svg") -> IMAGE
        lowerMime.startsWith("video/") || ext in setOf("mp4", "mov", "mkv", "webm", "avi", "m4v") -> VIDEO
        lowerMime.startsWith("audio/") || ext in setOf("mp3", "wav", "m4a", "aac", "ogg", "flac") -> AUDIO
        lowerMime == "application/vnd.google-apps.spreadsheet" ||
          lowerMime.contains("spreadsheet") ||
          lowerMime.contains("excel") ||
          lowerMime == "text/csv" ||
          ext in setOf("csv", "tsv", "xlsx", "xls", "ods") -> SPREADSHEET
        lowerMime == "application/vnd.google-apps.document" ||
          lowerMime == "application/vnd.google-apps.presentation" ||
          lowerMime.contains("word") ||
          lowerMime.contains("document") ||
          lowerMime.startsWith("text/") ||
          lowerMime == "application/json" ||
          ext in setOf("doc", "docx", "txt", "md", "rtf", "json", "xml", "kt", "ts", "js", "py", "html") -> DOCUMENT
        lowerMime == "application/zip" ||
          lowerMime.contains("zip") ||
          lowerMime.contains("tar") ||
          lowerMime.contains("rar") ||
          lowerMime.contains("7z") ||
          ext in setOf("zip", "rar", "7z", "tar", "gz") -> ARCHIVE
        else -> OTHER
      }
    }
  }
}

enum class AccessRole(val apiRole: String, val label: String) {
  OWNER("owner", "Owner"),
  EDITOR("writer", "Editor"),
  COMMENTER("commenter", "Commenter"),
  VIEWER("reader", "Viewer");

  companion object {
    fun fromApi(role: String?): AccessRole =
      when (role?.lowercase()) {
        "owner" -> OWNER
        "writer", "organizer", "fileorganizer" -> EDITOR
        "commenter" -> COMMENTER
        else -> VIEWER
      }
  }
}

enum class OfflineSyncStatus(val label: String) {
  NONE("Online only"),
  SAVED("Saved"),
  SYNCING("Syncing"),
  UPDATE_AVAILABLE("Update available"),
  FAILED("Failed"),
}

enum class SortField(val label: String, val driveOrderBy: String) {
  MODIFIED("Modified", "folder,modifiedTime"),
  NAME("Name", "folder,name_natural"),
  SIZE("Size", "folder,quotaBytesUsed"),
  TYPE("Type", "folder,mimeType"),
}

enum class OwnerFilter(val label: String) {
  ANYONE("Anyone"),
  ME("Me"),
  SHARED_WITH_ME("Shared with me"),
}

enum class DateRangeFilter(val label: String) {
  ANY_TIME("Any time"),
  TODAY("Today"),
  LAST_7_DAYS("Last 7 days"),
  LAST_30_DAYS("Last 30 days"),
}

enum class ViewMode {
  GRID,
  LIST,
}

enum class ThemePreference(val label: String) {
  SYSTEM("System"),
  LIGHT("Light"),
  DARK("Dark"),
}

enum class AppDestination(val route: String) {
  HOME("home"),
  FILES("files"),
  SHARED("shared"),
  STARRED("starred"),
  OFFLINE("offline"),
  TRASH("trash"),
  STORAGE("storage"),
  SETTINGS("settings"),
  SEARCH("search"),
}

enum class SharedSegment(val label: String) {
  WITH_ME("Shared with me"),
  BY_ME("By me"),
}

enum class ReaderTheme(val label: String) {
  LIGHT("Light"),
  SEPIA("Sepia"),
  DARK("Dark"),
}

data class BreadcrumbSegment(
  val id: String,
  val name: String,
)

data class DriveUser(
  val uid: String,
  val displayName: String,
  val email: String,
  val photoUrl: String? = null,
)

data class CloudFile(
  val id: String,
  val name: String,
  val mimeType: String,
  val category: FileCategory,
  val sizeBytes: Long,
  val modifiedTimeEpochMs: Long,
  val createdTimeEpochMs: Long,
  val trashedTimeEpochMs: Long? = null,
  val parentId: String? = null,
  val starred: Boolean = false,
  val trashed: Boolean = false,
  val shared: Boolean = false,
  val ownedByMe: Boolean = true,
  val ownerName: String = "You",
  val ownerEmail: String = "",
  val ownerPhotoUrl: String? = null,
  val sharingUserName: String? = null,
  val sharingUserEmail: String? = null,
  val userRole: AccessRole = AccessRole.OWNER,
  val canEdit: Boolean = true,
  val canShare: Boolean = true,
  val canDownload: Boolean = ownedByMe,
  val canDelete: Boolean = true,
  val webViewLink: String? = null,
  val thumbnailLink: String? = null,
  val md5Checksum: String? = null,
  val fileExtension: String? = null,
  val offlineStatus: OfflineSyncStatus = OfflineSyncStatus.NONE,
  val localOfflinePath: String? = null,
  val isAnyoneWithLink: Boolean = false,
  val collaboratorCount: Int = 0,
  val isUnopened: Boolean = false,
  val pendingOfflineAction: Boolean = false,
) {
  val isFolder: Boolean
    get() = category == FileCategory.FOLDER

  val isGoogleWorkspaceDoc: Boolean
    get() = mimeType.startsWith("application/vnd.google-apps.") && !isFolder
}

data class StorageQuotaInfo(
  val limitBytes: Long,
  val usageBytes: Long,
  val usageInDriveBytes: Long,
  val usageInTrashBytes: Long,
) {
  val usageFraction: Float
    get() = if (limitBytes > 0L) (usageBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f) else 0f

  val isAlmostFull: Boolean
    get() = usageFraction >= 0.90f
}

data class CategoryStorageBreakdown(
  val category: FileCategory,
  val bytes: Long,
  val fileCount: Int,
)

data class SharePermission(
  val id: String,
  val type: String, // "user", "group", "domain", "anyone"
  val role: AccessRole,
  val displayName: String?,
  val emailAddress: String?,
  val photoLink: String?,
  val allowFileDiscovery: Boolean = false,
  val expirationTime: String? = null,
  val isInherited: Boolean = false,
  val inheritedFrom: String? = null,
)

data class FilterConfig(
  val sortField: SortField = SortField.MODIFIED,
  val ascending: Boolean = false,
  val categories: Set<FileCategory> = emptySet(),
  val ownerFilter: OwnerFilter = OwnerFilter.ANYONE,
  val dateRange: DateRangeFilter = DateRangeFilter.ANY_TIME,
) {
  val hasActiveFilters: Boolean
    get() =
      categories.isNotEmpty() ||
        ownerFilter != OwnerFilter.ANYONE ||
        dateRange != DateRangeFilter.ANY_TIME
}

enum class UploadStatus {
  QUEUED,
  WAITING_WIFI,
  UPLOADING,
  COMPLETED,
  FAILED,
  CANCELLED,
}

data class UploadTaskState(
  val id: String,
  val fileName: String,
  val mimeType: String,
  val sourceUriString: String,
  val parentFolderId: String,
  val totalBytes: Long,
  val uploadedBytes: Long,
  val status: UploadStatus,
  val errorMessage: String? = null,
) {
  val progress: Float
    get() = if (totalBytes > 0L) (uploadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class CloudBoxSettings(
  val onboardingCompleted: Boolean = false,
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val defaultViewMode: ViewMode = ViewMode.GRID,
  val defaultSortField: SortField = SortField.MODIFIED,
  val defaultSortAscending: Boolean = false,
  val wifiOnlyUploads: Boolean = false,
  val autoplayMedia: Boolean = true,
  val keepScreenOnViewer: Boolean = false,
  val notifyNewShare: Boolean = true,
  val notifyUploadFinished: Boolean = true,
  val notifyStorageAlmostFull: Boolean = true,
)

sealed interface ScreenDataState<out T> {
  data object Loading : ScreenDataState<Nothing>
  data class Success<T>(val data: T) : ScreenDataState<T>
  data object Empty : ScreenDataState<Nothing>
  data class Error(
    val message: String,
    val code: Int? = null,
    val isAuthExpired: Boolean = false,
    val isPermissionDenied: Boolean = false,
  ) : ScreenDataState<Nothing>
}

sealed interface AuthSessionState {
  data object Checking : AuthSessionState
  data object SignedOut : AuthSessionState
  data class NeedsDrivePermission(val user: DriveUser, val errorMessage: String? = null) : AuthSessionState
  data class Authenticated(val user: DriveUser) : AuthSessionState
  data class Error(val message: String) : AuthSessionState
}
