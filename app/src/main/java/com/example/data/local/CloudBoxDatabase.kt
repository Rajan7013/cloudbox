package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.AccessRole
import com.example.model.CloudFile
import com.example.model.FileCategory
import com.example.model.OfflineSyncStatus
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_files")
data class CachedFileEntity(
  @PrimaryKey val id: String,
  val ownerUid: String,
  val name: String,
  val mimeType: String,
  val categoryName: String,
  val sizeBytes: Long,
  val modifiedTimeEpochMs: Long,
  val createdTimeEpochMs: Long,
  val trashedTimeEpochMs: Long?,
  val parentId: String?,
  val starred: Boolean,
  val trashed: Boolean,
  val shared: Boolean,
  val ownedByMe: Boolean,
  val ownerName: String,
  val ownerEmail: String,
  val ownerPhotoUrl: String?,
  val sharingUserName: String?,
  val sharingUserEmail: String?,
  val userRoleName: String,
  val canEdit: Boolean,
  @androidx.room.ColumnInfo(defaultValue = "0") val canDownload: Boolean,
  val canShare: Boolean,
  val canDelete: Boolean,
  val webViewLink: String?,
  val thumbnailLink: String?,
  val md5Checksum: String?,
  val fileExtension: String?,
  val isAnyoneWithLink: Boolean,
  val collaboratorCount: Int,
  val isSharedWithMe: Boolean,
)

@Entity(tableName = "offline_files")
data class OfflineFileEntity(
  @PrimaryKey val fileId: String,
  val ownerUid: String,
  val name: String,
  val mimeType: String,
  val categoryName: String,
  val sizeBytes: Long,
  val localPath: String,
  val statusName: String,
  val syncedAtEpochMs: Long,
  val remoteModifiedAtMs: Long,
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
  @PrimaryKey val queryKey: String,
  val ownerUid: String,
  val queryText: String,
  val searchedAtMs: Long,
)

@Entity(tableName = "pdf_bookmarks")
data class PdfBookmarkEntity(
  @PrimaryKey val bookmarkKey: String, // "${ownerUid}_${fileId}_${pageIndex}"
  val ownerUid: String,
  val fileId: String,
  val pageIndex: Int,
  val createdAtMs: Long,
)

@Dao
interface CachedFileDao {
  @Query("SELECT * FROM cached_files WHERE ownerUid = :uid AND trashed = 0 AND parentId = :parentId ORDER BY CASE WHEN categoryName = 'FOLDER' THEN 0 ELSE 1 END, modifiedTimeEpochMs DESC")
  fun observeFolder(uid: String, parentId: String): Flow<List<CachedFileEntity>>

  @Query("SELECT * FROM cached_files WHERE ownerUid = :uid AND trashed = 0 AND isSharedWithMe = 0 ORDER BY modifiedTimeEpochMs DESC LIMIT 20")
  suspend fun getRecentFiles(uid: String): List<CachedFileEntity>

  @Query("SELECT * FROM cached_files WHERE ownerUid = :uid AND trashed = 0 AND parentId = :parentId")
  suspend fun getFolderOnce(uid: String, parentId: String): List<CachedFileEntity>

  @Query("SELECT * FROM cached_files WHERE ownerUid = :uid AND trashed = 0 AND starred = 1")
  suspend fun getStarredOnce(uid: String): List<CachedFileEntity>

  @Query("SELECT * FROM cached_files WHERE ownerUid = :uid AND trashed = 0 AND isSharedWithMe = 1")
  suspend fun getSharedWithMeOnce(uid: String): List<CachedFileEntity>

  @Query("SELECT * FROM cached_files WHERE ownerUid = :uid AND trashed = 0 AND ownedByMe = 1 AND shared = 1")
  suspend fun getSharedByMeOnce(uid: String): List<CachedFileEntity>

  @Query("SELECT * FROM cached_files WHERE ownerUid = :uid AND trashed = 1")
  suspend fun getTrashedOnce(uid: String): List<CachedFileEntity>

  @Query("SELECT * FROM cached_files WHERE ownerUid = :uid AND trashed = 0 AND name LIKE '%' || :query || '%' ORDER BY modifiedTimeEpochMs DESC")
  suspend fun searchLocal(uid: String, query: String): List<CachedFileEntity>

  @Query("SELECT * FROM cached_files WHERE ownerUid = :uid AND id = :fileId LIMIT 1")
  suspend fun getById(uid: String, fileId: String): CachedFileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<CachedFileEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: CachedFileEntity)

  @Query("DELETE FROM cached_files WHERE ownerUid = :uid AND id = :fileId")
  suspend fun deleteById(uid: String, fileId: String)

  @Query("DELETE FROM cached_files WHERE ownerUid = :uid AND trashed = 1")
  suspend fun deleteAllTrashed(uid: String)

  @Query("DELETE FROM cached_files WHERE ownerUid = :uid")
  suspend fun clearUserCache(uid: String)
}

@Dao
interface OfflineFileDao {
  @Query("SELECT * FROM offline_files WHERE ownerUid = :uid ORDER BY syncedAtEpochMs DESC")
  fun observeAll(uid: String): Flow<List<OfflineFileEntity>>

  @Query("SELECT * FROM offline_files WHERE ownerUid = :uid")
  suspend fun getAllOnce(uid: String): List<OfflineFileEntity>

  @Query("SELECT * FROM offline_files WHERE ownerUid = :uid AND fileId = :fileId LIMIT 1")
  suspend fun getByFileId(uid: String, fileId: String): OfflineFileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: OfflineFileEntity)

  @Query("DELETE FROM offline_files WHERE ownerUid = :uid AND fileId = :fileId")
  suspend fun delete(uid: String, fileId: String)
}

@Dao
interface RecentSearchDao {
  @Query("SELECT * FROM recent_searches WHERE ownerUid = :uid ORDER BY searchedAtMs DESC LIMIT 12")
  fun observeRecent(uid: String): Flow<List<RecentSearchEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(item: RecentSearchEntity)

  @Query("DELETE FROM recent_searches WHERE queryKey = :queryKey")
  suspend fun delete(queryKey: String)

  @Query("DELETE FROM recent_searches WHERE ownerUid = :uid")
  suspend fun clearAll(uid: String)
}

@Dao
interface PdfBookmarkDao {
  @Query("SELECT pageIndex FROM pdf_bookmarks WHERE ownerUid = :uid AND fileId = :fileId ORDER BY pageIndex ASC")
  fun observeBookmarks(uid: String, fileId: String): Flow<List<Int>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: PdfBookmarkEntity)

  @Query("DELETE FROM pdf_bookmarks WHERE bookmarkKey = :key")
  suspend fun delete(key: String)
}

@Database(
  entities = [
    CachedFileEntity::class,
    OfflineFileEntity::class,
    RecentSearchEntity::class,
    PdfBookmarkEntity::class,
  ],
  version = 2,
  exportSchema = false,
)
abstract class CloudBoxDatabase : RoomDatabase() {
  abstract fun cachedFileDao(): CachedFileDao
  abstract fun offlineFileDao(): OfflineFileDao
  abstract fun recentSearchDao(): RecentSearchDao
  abstract fun pdfBookmarkDao(): PdfBookmarkDao

  companion object {
    @Volatile private var instance: CloudBoxDatabase? = null

    fun getInstance(context: Context): CloudBoxDatabase =
      instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          CloudBoxDatabase::class.java,
          "cloudbox_cache.db",
        ).addMigrations(object : androidx.room.migration.Migration(1, 2) {
          override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE cached_files ADD COLUMN canDownload INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE cached_files SET canDownload = 1 WHERE ownedByMe = 1")
          }
        }).build().also { instance = it }
      }
  }
}

fun CloudFile.toEntity(ownerUid: String, isSharedWithMe: Boolean): CachedFileEntity =
  CachedFileEntity(
    id = id,
    ownerUid = ownerUid,
    name = name,
    mimeType = mimeType,
    categoryName = category.name,
    sizeBytes = sizeBytes,
    modifiedTimeEpochMs = modifiedTimeEpochMs,
    createdTimeEpochMs = createdTimeEpochMs,
    trashedTimeEpochMs = trashedTimeEpochMs,
    parentId = parentId,
    starred = starred,
    trashed = trashed,
    shared = shared,
    ownedByMe = ownedByMe,
    ownerName = ownerName,
    ownerEmail = ownerEmail,
    ownerPhotoUrl = ownerPhotoUrl,
    sharingUserName = sharingUserName,
    sharingUserEmail = sharingUserEmail,
    userRoleName = userRole.name,
    canEdit = canEdit,
    canDownload = canDownload,
    canShare = canShare,
    canDelete = canDelete,
    webViewLink = webViewLink,
    thumbnailLink = thumbnailLink,
    md5Checksum = md5Checksum,
    fileExtension = fileExtension,
    isAnyoneWithLink = isAnyoneWithLink,
    collaboratorCount = collaboratorCount,
    isSharedWithMe = isSharedWithMe,
  )

fun CachedFileEntity.toDomain(offlineEntity: OfflineFileEntity? = null): CloudFile {
  val cat = runCatching { FileCategory.valueOf(categoryName) }.getOrDefault(FileCategory.OTHER)
  val role = runCatching { AccessRole.valueOf(userRoleName) }.getOrDefault(AccessRole.VIEWER)
  val offlineStatus =
    offlineEntity?.let {
      runCatching { OfflineSyncStatus.valueOf(it.statusName) }.getOrDefault(OfflineSyncStatus.NONE)
    } ?: OfflineSyncStatus.NONE

  return CloudFile(
    id = id,
    name = name,
    mimeType = mimeType,
    category = cat,
    sizeBytes = sizeBytes,
    modifiedTimeEpochMs = modifiedTimeEpochMs,
    createdTimeEpochMs = createdTimeEpochMs,
    trashedTimeEpochMs = trashedTimeEpochMs,
    parentId = parentId,
    starred = starred,
    trashed = trashed,
    shared = shared,
    ownedByMe = ownedByMe,
    ownerName = ownerName,
    ownerEmail = ownerEmail,
    ownerPhotoUrl = ownerPhotoUrl,
    sharingUserName = sharingUserName,
    sharingUserEmail = sharingUserEmail,
    userRole = role,
    canEdit = canEdit,
    canDownload = canDownload,
    canShare = canShare,
    canDelete = canDelete,
    webViewLink = webViewLink,
    thumbnailLink = thumbnailLink,
    md5Checksum = md5Checksum,
    fileExtension = fileExtension,
    offlineStatus = offlineStatus,
    localOfflinePath = offlineEntity?.localPath,
    isAnyoneWithLink = isAnyoneWithLink,
    collaboratorCount = collaboratorCount,
  )
}
