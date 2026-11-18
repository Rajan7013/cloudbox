package com.example.ui.manage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.model.CategoryStorageBreakdown
import com.example.model.CloudBoxSettings
import com.example.model.CloudFile
import com.example.model.DriveUser
import com.example.model.FileCategory
import com.example.model.OfflineSyncStatus
import com.example.model.ScreenDataState
import com.example.model.StorageQuotaInfo
import com.example.model.ThemePreference
import com.example.model.ViewMode
import com.example.ui.components.CloudBoxEmptyState
import com.example.ui.components.CloudBoxErrorCard
import com.example.ui.components.FileListSkeleton
import com.example.ui.components.FileTypeTile
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils

@Composable
fun OfflineFilesScreen(
  offlineFiles: List<CloudFile>,
  imageLoader: ImageLoader,
  onBack: () -> Unit,
  onOpenFile: (CloudFile) -> Unit,
  onRetryOffline: (CloudFile) -> Unit,
  onRemoveOffline: (CloudFile) -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  val totalSavedBytes = offlineFiles.sumOf { it.sizeBytes }

  Column(modifier = Modifier.fillMaxSize().testTag("offline_screen")) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      Column(modifier = Modifier.weight(1f)) {
        Text("Offline files", style = MaterialTheme.typography.headlineMedium)
        Text(
          "${offlineFiles.size} files · ${FormatUtils.formatBytes(totalSavedBytes)} saved",
          style = MaterialTheme.typography.bodySmall,
          color = extra.textSecondary,
        )
      }
    }

    if (offlineFiles.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CloudBoxEmptyState(
          title = "No offline files",
          subtitle = "Use ⋮ on any file and tap Make available offline.",
          icon = Icons.Filled.CloudOff,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(offlineFiles.size, key = { idx -> offlineFiles[idx].id }) { idx ->
          val file = offlineFiles[idx]
          Surface(
            onClick = { onOpenFile(file) },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extra.border.copy(alpha = 0.7f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FileTypeTile(category = file.category, thumbnailLink = file.thumbnailLink, imageLoader = imageLoader, size = 44.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = file.name,
                  style = MaterialTheme.typography.labelLarge,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                val statusColor =
                  when (file.offlineStatus) {
                    OfflineSyncStatus.SAVED -> extra.success
                    OfflineSyncStatus.FAILED -> MaterialTheme.colorScheme.error
                    OfflineSyncStatus.UPDATE_AVAILABLE -> extra.warning
                    else -> extra.textSecondary
                  }
                Text(
                  text = "${FormatUtils.formatBytes(file.sizeBytes)} · ${file.offlineStatus.label}",
                  style = MaterialTheme.typography.bodySmall,
                  color = statusColor,
                )
              }
              if (file.offlineStatus == OfflineSyncStatus.FAILED) {
                TextButton(onClick = { onRetryOffline(file) }) { Text("Retry") }
              }
              IconButton(onClick = { onRemoveOffline(file) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove offline copy", tint = extra.textSecondary)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun TrashScreen(
  trashState: ScreenDataState<List<CloudFile>>,
  imageLoader: ImageLoader,
  onBack: () -> Unit,
  onRestore: (CloudFile) -> Unit,
  onDeleteForever: (CloudFile) -> Unit,
  onEmptyTrash: () -> Unit,
  onRetry: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  var confirmDeleteTarget by remember { mutableStateOf<CloudFile?>(null) }
  var confirmEmptyTrash by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().testTag("trash_screen")) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      Column(modifier = Modifier.weight(1f)) {
        Text("Trash", style = MaterialTheme.typography.headlineMedium)
        Text("Items are deleted permanently after 30 days", style = MaterialTheme.typography.bodySmall, color = extra.textSecondary)
      }
    }

    when (trashState) {
      is ScreenDataState.Loading -> Box(modifier = Modifier.fillMaxSize().padding(16.dp)) { FileListSkeleton(5) }
      is ScreenDataState.Error -> Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        CloudBoxErrorCard(message = trashState.message, onRetry = onRetry)
      }
      is ScreenDataState.Empty -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CloudBoxEmptyState(
            title = "Trash is empty",
            subtitle = "Files you move to trash appear here before permanent deletion.",
            icon = Icons.Filled.DeleteOutline,
          )
        }
      }
      is ScreenDataState.Success -> {
        val items = trashState.data
        Column(modifier = Modifier.fillMaxSize()) {
          LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(items.size, key = { idx -> items[idx].id }) { idx ->
              val file = items[idx]
              val daysLeft = FormatUtils.trashDaysRemaining(file.trashedTimeEpochMs, file.modifiedTimeEpochMs)
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extra.border.copy(alpha = 0.7f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  FileTypeTile(category = file.category, thumbnailLink = file.thumbnailLink, imageLoader = imageLoader, size = 44.dp)
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = file.name,
                      style = MaterialTheme.typography.labelLarge,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text("$daysLeft days left · ${FormatUtils.formatBytes(file.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = extra.textSecondary)
                  }
                  OutlinedButton(
                    onClick = { onRestore(file) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(38.dp),
                  ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                  }
                  IconButton(onClick = { confirmDeleteTarget = file }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = "Delete forever", tint = MaterialTheme.colorScheme.error)
                  }
                }
              }
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extra.border),
          ) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              Button(
                onClick = { confirmEmptyTrash = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                  contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("empty_trash_button"),
                shape = RoundedCornerShape(14.dp),
              ) {
                Text("Empty trash", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }

  confirmDeleteTarget?.let { target ->
    AlertDialog(
      onDismissRequest = { confirmDeleteTarget = null },
      title = { Text("Delete forever?") },
      text = { Text("“${target.name}” will be permanently deleted. This can’t be undone.") },
      dismissButton = { TextButton(onClick = { confirmDeleteTarget = null }) { Text("Cancel") } },
      confirmButton = {
        Button(
          onClick = {
            onDeleteForever(target)
            confirmDeleteTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Delete")
        }
      },
    )
  }

  if (confirmEmptyTrash) {
    AlertDialog(
      onDismissRequest = { confirmEmptyTrash = false },
      title = { Text("Empty trash?") },
      text = { Text("All items in Trash will be permanently deleted. This can’t be undone.") },
      dismissButton = { TextButton(onClick = { confirmEmptyTrash = false }) { Text("Cancel") } },
      confirmButton = {
        Button(
          onClick = {
            onEmptyTrash()
            confirmEmptyTrash = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Delete all")
        }
      },
    )
  }
}

@Composable
fun StorageScreen(
  quotaState: ScreenDataState<StorageQuotaInfo>,
  breakdownState: ScreenDataState<Pair<List<CategoryStorageBreakdown>, Long>>,
  onBack: () -> Unit,
  onSelectCategory: (FileCategory) -> Unit,
  onOpenTrash: () -> Unit,
  onRetry: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current

  Column(modifier = Modifier.fillMaxSize().testTag("storage_screen")) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      Text("Storage", style = MaterialTheme.typography.headlineMedium)
    }

    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      when (quotaState) {
        is ScreenDataState.Loading -> FileListSkeleton(1)
        is ScreenDataState.Error -> CloudBoxErrorCard(message = quotaState.message, onRetry = onRetry)
        is ScreenDataState.Empty -> Unit
        is ScreenDataState.Success -> {
          val quota = quotaState.data
          val gradient =
            if (quota.isAlmostFull) Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFF59E0B)))
            else Brush.linearGradient(listOf(extra.heroGradientStart, extra.heroGradientEnd))

          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
          ) {
            Box(modifier = Modifier.fillMaxWidth().background(gradient).padding(20.dp)) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "${FormatUtils.formatBytes(quota.usageBytes)} used of ${FormatUtils.formatBytes(quota.limitBytes)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                  )
                  if (quota.isAlmostFull) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White)
                  }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                  progress = { quota.usageFraction },
                  modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                  color = Color.White,
                  trackColor = Color.White.copy(alpha = 0.25f),
                )
              }
            }
          }
        }
      }

      Text("BY TYPE", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)

      when (breakdownState) {
        is ScreenDataState.Loading -> FileListSkeleton(4)
        is ScreenDataState.Error -> CloudBoxErrorCard(message = breakdownState.message, onRetry = onRetry)
        is ScreenDataState.Empty -> Unit
        is ScreenDataState.Success -> {
          val (categories, trashedBytes) = breakdownState.data
          val maxCatBytes = categories.maxOfOrNull { it.bytes }?.coerceAtLeast(1L) ?: 1L
          if (categories.isEmpty()) {
            Text(
              "No files occupying Drive space yet.",
              style = MaterialTheme.typography.bodyMedium,
              color = extra.textSecondary,
            )
          } else {
            categories.forEach { entry ->
              val catColor = extra.colorForCategory(entry.category)
              Surface(
                onClick = { onSelectCategory(entry.category) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extra.border.copy(alpha = 0.7f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  FileTypeTile(category = entry.category, size = 40.dp)
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                      Text(
                        "${entry.category.label} (${entry.fileCount})",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                      )
                      Text(FormatUtils.formatBytes(entry.bytes), style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                      progress = { (entry.bytes.toFloat() / maxCatBytes.toFloat()).coerceIn(0.03f, 1f) },
                      modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                      color = catColor,
                      trackColor = catColor.copy(alpha = 0.14f),
                    )
                  }
                }
              }
            }
          }

          if (trashedBytes > 0L) {
            OutlinedButton(
              onClick = onOpenTrash,
              modifier = Modifier.fillMaxWidth().height(48.dp),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text("Clear trash to free ${FormatUtils.formatBytes(trashedBytes)}")
            }
          }
        }
      }
    }
  }
}

@Composable
fun SettingsScreen(
  user: DriveUser?,
  quotaState: ScreenDataState<StorageQuotaInfo>,
  settings: CloudBoxSettings,
  cacheSizeBytes: Long,
  offlineFileCount: Int,
  onBack: () -> Unit,
  onSetTheme: (ThemePreference) -> Unit,
  onSetDefaultView: (ViewMode) -> Unit,
  onSetWifiOnly: (Boolean) -> Unit,
  onSetAutoplay: (Boolean) -> Unit,
  onSetKeepScreenOn: (Boolean) -> Unit,
  onSetNotifyShare: (Boolean) -> Unit,
  onSetNotifyUpload: (Boolean) -> Unit,
  onOpenOfflineFiles: () -> Unit,
  onOpenTrash: () -> Unit,
  onClearCache: () -> Unit,
  onSignOut: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  var viewerDefaults by remember { mutableStateOf(false) }
  if (viewerDefaults) com.example.ui.viewer.ViewerDefaultsDialog(null) { viewerDefaults = false }
  var confirmSignOut by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().testTag("settings_screen")) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      Text("Settings", style = MaterialTheme.typography.headlineMedium)
    }

    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Profile Hero
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(Brush.linearGradient(listOf(extra.heroGradientStart, extra.heroGradientEnd)))
              .padding(18.dp),
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
              ) {
                if (!user?.photoUrl.isNullOrBlank()) {
                  AsyncImage(
                    model = user?.photoUrl,
                    contentDescription = user?.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                  )
                } else {
                  Text(
                    text = user?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                  )
                }
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text(user?.displayName ?: "CloudBox User", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
              }
            }
            if (quotaState is ScreenDataState.Success) {
              Spacer(modifier = Modifier.height(14.dp))
              LinearProgressIndicator(
                progress = { quotaState.data.usageFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                "${FormatUtils.formatBytes(quotaState.data.usageBytes)} / ${FormatUtils.formatBytes(quotaState.data.limitBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
              )
            }
          }
        }
      }

      // Account
      SettingsSectionHeader("ACCOUNT")
      SettingsCard {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.CloudDone, contentDescription = null, tint = extra.success)
          Spacer(modifier = Modifier.width(12.dp))
          Text("Google Drive", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
          Text("Linked", style = MaterialTheme.typography.bodySmall, color = extra.success, fontWeight = FontWeight.Bold)
        }
      }

      // Appearance
      SettingsSectionHeader("APPEARANCE")
      SettingsCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Theme", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreference.entries.forEach { pref ->
              val selected = settings.themePreference == pref
              Surface(
                onClick = { onSetTheme(pref) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                modifier = Modifier.weight(1f).height(40.dp).testTag("theme_${pref.name.lowercase()}"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    pref.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
          HorizontalDivider(color = extra.border)
          Text("Default view", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewMode.entries.forEach { mode ->
              val selected = settings.defaultViewMode == mode
              Surface(
                onClick = { onSetDefaultView(mode) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                modifier = Modifier.weight(1f).height(40.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    if (mode == ViewMode.GRID) "Grid" else "List",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }
      }

      // Uploads & Viewer
      SettingsSectionHeader("UPLOADS & VIEWER")
      SettingsCard {
        Column {
          SettingsSwitchRow(
            title = "Upload on Wi-Fi only",
            subtitle = "Pause uploads when on metered cellular data",
            checked = settings.wifiOnlyUploads,
            onCheckedChange = onSetWifiOnly,
            testTag = "setting_wifi_only",
          )
          HorizontalDivider(color = extra.border)
          SettingsSwitchRow(
            title = "Autoplay media",
            subtitle = "Start video and audio automatically in viewer",
            checked = settings.autoplayMedia,
            onCheckedChange = onSetAutoplay,
            testTag = "setting_autoplay",
          )
          HorizontalDivider(color = extra.border)
          SettingsLinkRow("Viewer settings", "Per file type") { viewerDefaults = true }
          SettingsSwitchRow(
            title = "Keep screen on in viewer",
            subtitle = "Prevent sleep while reading documents",
            checked = settings.keepScreenOnViewer,
            onCheckedChange = onSetKeepScreenOn,
            testTag = "setting_keep_screen_on",
          )
        }
      }

      // Notifications
      SettingsSectionHeader("NOTIFICATIONS")
      SettingsCard {
        Column {
          SettingsSwitchRow(
            title = "Upload finished alerts",
            checked = settings.notifyUploadFinished,
            onCheckedChange = onSetNotifyUpload,
            testTag = "setting_notify_upload",
          )
          HorizontalDivider(color = extra.border)
          SettingsSwitchRow(
            title = "New shared items",
            checked = settings.notifyNewShare,
            onCheckedChange = onSetNotifyShare,
            testTag = "setting_notify_share",
          )
        }
      }

      // Offline & Cache
      SettingsSectionHeader("OFFLINE & CACHE")
      SettingsCard {
        Column {
          SettingsLinkRow("Offline files", "$offlineFileCount saved", onClick = onOpenOfflineFiles)
          HorizontalDivider(color = extra.border)
          SettingsLinkRow("Trash", "Manage deleted files", onClick = onOpenTrash)
          HorizontalDivider(color = extra.border)
          SettingsLinkRow("Clear viewer cache", FormatUtils.formatBytes(cacheSizeBytes), onClick = onClearCache)
        }
      }

      // Sign Out
      Button(
        onClick = { confirmSignOut = true },
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
          contentColor = MaterialTheme.colorScheme.error,
        ),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_sign_out_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Text("Sign out", fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  if (confirmSignOut) {
    AlertDialog(
      onDismissRequest = { confirmSignOut = false },
      title = { Text("Sign out of CloudBox?") },
      text = { Text("You can sign back in anytime with your Google account.") },
      dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
      confirmButton = {
        Button(
          onClick = {
            confirmSignOut = false
            onSignOut()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_sign_out_button"),
        ) {
          Text("Sign out")
        }
      },
    )
  }
}

@Composable
private fun SettingsSectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelSmall,
    color = LocalCloudBoxColors.current.textSecondary,
    modifier = Modifier.padding(top = 4.dp),
  )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, LocalCloudBoxColors.current.border.copy(alpha = 0.8f)),
    content = content,
  )
}

@Composable
private fun SettingsSwitchRow(
  title: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.labelLarge)
      if (!subtitle.isNullOrBlank()) {
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalCloudBoxColors.current.textSecondary)
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}

@Composable
private fun SettingsLinkRow(title: String, value: String, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
    Text(value, style = MaterialTheme.typography.bodySmall, color = LocalCloudBoxColors.current.textSecondary)
    Spacer(modifier = Modifier.width(4.dp))
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = LocalCloudBoxColors.current.textSecondary)
  }
}
