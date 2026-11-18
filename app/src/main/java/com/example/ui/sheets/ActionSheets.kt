package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.model.AccessRole
import com.example.model.CloudFile
import com.example.model.DateRangeFilter
import com.example.model.DriveUser
import com.example.model.FileCategory
import com.example.model.FilterConfig
import com.example.model.OfflineSyncStatus
import com.example.model.OwnerFilter
import com.example.model.ScreenDataState
import com.example.model.SharePermission
import com.example.model.SortField
import com.example.ui.components.CloudBoxErrorCard
import com.example.ui.components.FileTypeTile
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUploadSheet(
  isOnline: Boolean,
  isStorageFull: Boolean,
  onDismiss: () -> Unit,
  onNewFolder: () -> Unit,
  onUploadFiles: () -> Unit,
  onUploadFolder: () -> Unit,
  onUploadMedia: () -> Unit,
  onTakePhoto: () -> Unit,
  onScanDocument: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
      Text("Create new", style = MaterialTheme.typography.headlineSmall)
      Spacer(modifier = Modifier.height(14.dp))

      if (isStorageFull) {
        Surface(
          color = LocalCloudBoxColors.current.warning.copy(alpha = 0.16f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
          Text(
            "Not enough Google Drive space for new uploads.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      SheetActionRow(
        icon = Icons.Filled.CreateNewFolder,
        label = "New folder",
        subtitle = if (!isOnline) "Needs internet" else "Organize your files",
        enabled = isOnline,
        onClick = {
          onDismiss()
          onNewFolder()
        },
        testTag = "sheet_new_folder",
      )
      SheetActionRow(
        icon = Icons.Filled.DriveFolderUpload,
        label = "Upload folder",
        subtitle = if (!isOnline) "Needs internet" else "Pick device folder with subfolders & files",
        enabled = isOnline && !isStorageFull,
        onClick = {
          onDismiss()
          onUploadFolder()
        },
        testTag = "sheet_upload_folder",
      )
      SheetActionRow(
        icon = Icons.Filled.UploadFile,
        label = "Upload file",
        subtitle = if (!isOnline) "Needs internet" else "Documents, PDFs, audio, video, zip",
        enabled = isOnline && !isStorageFull,
        onClick = {
          onDismiss()
          onUploadFiles()
        },
        testTag = "sheet_upload_file",
      )
      SheetActionRow(
        icon = Icons.Filled.PhotoLibrary,
        label = "Upload photos or video",
        subtitle = if (!isOnline) "Needs internet" else "Pick from Android Photo Picker",
        enabled = isOnline && !isStorageFull,
        onClick = {
          onDismiss()
          onUploadMedia()
        },
        testTag = "sheet_upload_media",
      )
      SheetActionRow(
        icon = Icons.Filled.CameraAlt,
        label = "Take photo",
        subtitle = if (!isOnline) "Needs internet" else "Capture with camera and upload",
        enabled = isOnline && !isStorageFull,
        onClick = {
          onDismiss()
          onTakePhoto()
        },
        testTag = "sheet_take_photo",
      )
      SheetActionRow(
        icon = Icons.Filled.DocumentScanner,
        label = "Scan document",
        subtitle = if (!isOnline) "Needs internet" else "Capture and enhance to crisp PDF",
        enabled = isOnline && !isStorageFull,
        onClick = {
          onDismiss()
          onScanDocument()
        },
        testTag = "sheet_scan_document",
      )
    }
  }
}

@Composable
fun NewFolderDialog(
  onDismiss: () -> Unit,
  onCreate: suspend (String) -> Result<CloudFile>,
) {
  var name by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New folder") },
    text = {
      Column {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            error = null
          },
          label = { Text("Folder name") },
          singleLine = true,
          isError = error != null,
          supportingText = error?.let { msg -> { Text(msg) } },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("new_folder_input"),
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
    confirmButton = {
      Button(
        enabled = name.isNotBlank() && !busy,
        onClick = {
          scope.launch {
            busy = true
            val res = onCreate(name.trim())
            busy = false
            res.fold(
              onSuccess = { onDismiss() },
              onFailure = { err -> error = err.localizedMessage ?: "Couldn't create folder" },
            )
          }
        },
        modifier = Modifier.testTag("new_folder_create_button"),
      ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        else Text("Create")
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SortAndFilterSheet(
  currentFilter: FilterConfig,
  onDismiss: () -> Unit,
  onApply: (FilterConfig) -> Unit,
) {
  var draft by remember(currentFilter) { mutableStateOf(currentFilter) }
  val extra = LocalCloudBoxColors.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
    ) {
      Text("Sort by", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SortField.entries.forEach { field ->
          FilterChip(
            selected = draft.sortField == field,
            onClick = { draft = draft.copy(sortField = field) },
            label = { Text(field.label) },
            shape = RoundedCornerShape(20.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
          )
        }
        FilterChip(
          selected = true,
          onClick = { draft = draft.copy(ascending = !draft.ascending) },
          leadingIcon = { Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp)) },
          label = { Text(if (draft.ascending) "Ascending" else "Descending") },
          shape = RoundedCornerShape(20.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
      Text("Filter by type", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(10.dp))
      val typeOptions =
        listOf(
          FileCategory.FOLDER,
          FileCategory.DOCUMENT,
          FileCategory.SPREADSHEET,
          FileCategory.PDF,
          FileCategory.IMAGE,
          FileCategory.VIDEO,
          FileCategory.AUDIO,
          FileCategory.ARCHIVE,
        )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
          selected = draft.categories.isEmpty(),
          onClick = { draft = draft.copy(categories = emptySet()) },
          label = { Text("All") },
          shape = RoundedCornerShape(20.dp),
        )
        typeOptions.forEach { cat ->
          val active = cat in draft.categories
          FilterChip(
            selected = active,
            onClick = {
              val next = if (active) draft.categories - cat else draft.categories + cat
              draft = draft.copy(categories = next)
            },
            label = { Text(cat.label) },
            shape = RoundedCornerShape(20.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      Text("Owner", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OwnerFilter.entries.forEach { owner ->
          FilterChip(
            selected = draft.ownerFilter == owner,
            onClick = { draft = draft.copy(ownerFilter = owner) },
            label = { Text(owner.label) },
            shape = RoundedCornerShape(20.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      Text("Date modified", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        DateRangeFilter.entries.forEach { range ->
          FilterChip(
            selected = draft.dateRange == range,
            onClick = { draft = draft.copy(dateRange = range) },
            label = { Text(range.label) },
            shape = RoundedCornerShape(20.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(22.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
          onClick = {
            draft = FilterConfig()
            onApply(FilterConfig())
            onDismiss()
          },
          modifier = Modifier.weight(1f).height(48.dp),
          shape = RoundedCornerShape(14.dp),
        ) {
          Text("Reset")
        }
        Button(
          onClick = {
            onApply(draft)
            onDismiss()
          },
          modifier = Modifier.weight(1f).height(48.dp).testTag("apply_filter_button"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Text("Apply")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionsSheet(
  file: CloudFile,
  isOnline: Boolean,
  imageLoader: ImageLoader,
  onDismiss: () -> Unit,
  onShare: () -> Unit,
  onDownload: () -> Unit,
  onToggleStar: () -> Unit,
  onToggleOffline: () -> Unit,
  onRename: () -> Unit,
  onMove: () -> Unit,
  onMakeCopy: () -> Unit,
  onDetails: () -> Unit,
  onMoveToTrash: () -> Unit,
  onRestoreFromTrash: () -> Unit = {},
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        FileTypeTile(category = file.category, thumbnailLink = file.thumbnailLink, imageLoader = imageLoader, size = 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
          Text(
            "${file.category.label} · ${if (file.isFolder) "Folder" else FormatUtils.formatBytes(file.sizeBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = LocalCloudBoxColors.current.textSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = LocalCloudBoxColors.current.border)
      Spacer(modifier = Modifier.height(8.dp))

      if (file.trashed) {
        SheetActionRow(
          icon = Icons.Filled.Restore,
          label = "Restore",
          enabled = isOnline,
          onClick = {
            onDismiss()
            onRestoreFromTrash()
          },
          testTag = "action_restore",
        )
        SheetActionRow(
          icon = Icons.Filled.Info,
          label = "Details",
          onClick = {
            onDismiss()
            onDetails()
          },
          testTag = "action_details",
        )
      } else {
        if (file.canShare) {
          SheetActionRow(
            icon = Icons.Filled.Share,
            label = "Share",
            subtitle = if (!isOnline) "Needs internet" else null,
            enabled = isOnline,
            onClick = {
              onDismiss()
              onShare()
            },
            testTag = "action_share",
          )
        }
        if (!file.isFolder && file.canDownload) {
          SheetActionRow(
            icon = Icons.Filled.Download,
            label = "Download to device",
            enabled = isOnline || file.offlineStatus == OfflineSyncStatus.SAVED,
            onClick = {
              onDismiss()
              onDownload()
            },
            testTag = "action_download",
          )
        }
        SheetActionRow(
          icon = if (file.starred) Icons.Filled.Star else Icons.Filled.StarOutline,
          label = if (file.starred) "Remove star" else "Add star",
          enabled = isOnline,
          onClick = {
            onDismiss()
            onToggleStar()
          },
          testTag = "action_star",
        )
        if (!file.isFolder && (file.canDownload || file.offlineStatus == OfflineSyncStatus.SAVED)) {
          val isOffline = file.offlineStatus == OfflineSyncStatus.SAVED
          SheetActionRow(
            icon = Icons.Filled.OfflinePin,
            label = if (isOffline) "Remove offline copy" else "Make available offline",
            enabled = isOffline || isOnline,
            onClick = {
              onDismiss()
              onToggleOffline()
            },
            testTag = "action_offline",
          )
        }
        if (file.canEdit) {
          SheetActionRow(
            icon = Icons.Filled.DriveFileRenameOutline,
            label = "Rename",
            enabled = isOnline,
            onClick = {
              onDismiss()
              onRename()
            },
            testTag = "action_rename",
          )
          if (file.ownedByMe) {
            SheetActionRow(
              icon = Icons.AutoMirrored.Filled.DriveFileMove,
              label = "Move to…",
              enabled = isOnline,
              onClick = {
                onDismiss()
                onMove()
              },
              testTag = "action_move",
            )
          }
        }
        SheetActionRow(
          icon = Icons.Filled.ContentCopy,
          label = "Make a copy",
          enabled = isOnline && file.canDownload,
          onClick = {
            onDismiss()
            onMakeCopy()
          },
          testTag = "action_copy",
        )
        SheetActionRow(
          icon = Icons.Filled.Info,
          label = "Details",
          onClick = {
            onDismiss()
            onDetails()
          },
          testTag = "action_details",
        )
        if (file.ownedByMe) {
          HorizontalDivider(color = LocalCloudBoxColors.current.border, modifier = Modifier.padding(vertical = 6.dp))
          SheetActionRow(
            icon = Icons.Filled.DeleteOutline,
            label = "Move to trash",
            enabled = isOnline,
            isDestructive = true,
            onClick = {
              onDismiss()
              onMoveToTrash()
            },
            testTag = "action_trash",
          )
        }
      }
    }
  }
}

@Composable
fun RenameFileDialog(
  file: CloudFile,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by remember(file) { mutableStateOf(file.name) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Rename") },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        singleLine = true,
        label = { Text("Name") },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("rename_input"),
      )
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    confirmButton = {
      Button(
        enabled = name.isNotBlank() && name.trim() != file.name,
        onClick = {
          onConfirm(name.trim())
          onDismiss()
        },
        modifier = Modifier.testTag("rename_confirm_button"),
      ) {
        Text("Rename")
      }
    },
  )
}

@Composable
fun MoveToFolderDialog(
  file: CloudFile,
  folders: List<CloudFile>,
  onDismiss: () -> Unit,
  onMoveTo: (targetFolderId: String, targetFolderName: String) -> Unit,
) {
  val candidates = remember(folders, file.id, file.parentId) {
    folders.filter { it.isFolder && it.id != file.id && it.id != file.parentId }
  }
  val canMoveToRoot = file.parentId != "root"
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Move “${file.name}”",
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
        if (canMoveToRoot) {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  onMoveTo("root", "My Files")
                  onDismiss()
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = LocalCloudBoxColors.current.colorForCategory(FileCategory.FOLDER))
            Spacer(modifier = Modifier.width(12.dp))
            Text("My Files (Root)", style = MaterialTheme.typography.bodyLarge)
          }
        }
        if (candidates.isEmpty() && !canMoveToRoot) {
          Text(
            "No other folders available yet. Create a folder in My Files first.",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalCloudBoxColors.current.textSecondary,
            modifier = Modifier.padding(vertical = 8.dp),
          )
        } else {
          candidates.forEach { folder ->
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    onMoveTo(folder.id, folder.name)
                    onDismiss()
                  }
                  .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Folder, contentDescription = null, tint = LocalCloudBoxColors.current.colorForCategory(FileCategory.FOLDER))
              Spacer(modifier = Modifier.width(12.dp))
              Text(folder.name, style = MaterialTheme.typography.bodyLarge)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAndPermissionsSheet(
  file: CloudFile,
  currentUser: DriveUser?,
  permissionsState: ScreenDataState<List<SharePermission>>,
  isBusy: Boolean,
  isOnline: Boolean,
  onDismiss: () -> Unit,
  onInvite: (emails: List<String>, role: AccessRole) -> Unit,
  onChangeRole: (permissionId: String, role: AccessRole) -> Unit,
  onOpenFolderSharing: (String) -> Unit,
  onRemovePermission: (permissionId: String) -> Unit,
  onSetAnyoneLink: (enabled: Boolean, role: AccessRole) -> Unit,
  onCopyLink: (String) -> Unit,
  onRetryLoad: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  var emailInput by remember(file.id) { mutableStateOf("") }
  var inviteRole by remember(file.id) { mutableStateOf(AccessRole.VIEWER) }
  var showInviteRoleMenu by remember { mutableStateOf(false) }
  val canManageSharing = isOnline && file.ownedByMe

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
    ) {
      Text(
        text = "Share “${file.name}”",
        style = MaterialTheme.typography.headlineSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(14.dp))

      if (!canManageSharing) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
          Text(
            if (!isOnline) "You are currently offline. Connect to the internet to change permissions."
            else "Only the owner can change who has access.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      // Add people by email
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = emailInput,
          onValueChange = { emailInput = it },
          label = { Text("Add people by email") },
          placeholder = { Text("name@example.com") },
          enabled = canManageSharing && !isBusy,
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("share_email_input"),
        )
        Box {
          OutlinedButton(
            onClick = { showInviteRoleMenu = true },
            enabled = canManageSharing && !isBusy,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(52.dp),
          ) {
            Text(inviteRole.label)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
          }
          DropdownMenu(expanded = showInviteRoleMenu, onDismissRequest = { showInviteRoleMenu = false }) {
            listOf(AccessRole.VIEWER, AccessRole.COMMENTER, AccessRole.EDITOR).forEach { r ->
              DropdownMenuItem(
                text = { Text(r.label) },
                trailingIcon = { if (r == inviteRole) Icon(Icons.Filled.Check, contentDescription = null) },
                onClick = {
                  inviteRole = r
                  showInviteRoleMenu = false
                },
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      val parsedEmails =
        emailInput
          .split(',', ';', ' ')
          .map { it.trim() }
          .filter { it.contains('@') && it.contains('.') }

      Button(
        onClick = {
          onInvite(parsedEmails, inviteRole)
          emailInput = ""
        },
        enabled = canManageSharing && !isBusy && parsedEmails.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_invite_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        if (isBusy) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
          Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (parsedEmails.size > 1) "Invite ${parsedEmails.size} people as ${inviteRole.label}" else "Invite as ${inviteRole.label}",
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))
      Text("PEOPLE WITH ACCESS", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
      Spacer(modifier = Modifier.height(8.dp))

      when (permissionsState) {
        is ScreenDataState.Loading -> {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Loading permissions…", style = MaterialTheme.typography.bodyMedium)
          }
        }
        is ScreenDataState.Error -> {
          CloudBoxErrorCard(message = permissionsState.message, onRetry = onRetryLoad)
        }
        is ScreenDataState.Empty, is ScreenDataState.Success -> {
          val perms = (permissionsState as? ScreenDataState.Success)?.data.orEmpty()
          val userPerms = perms.filter { it.type != "anyone" }
          val anyonePerm = perms.firstOrNull { it.type == "anyone" }

          if (userPerms.isEmpty() && file.ownedByMe && currentUser != null) {
            PermissionPersonRow(
              name = "${currentUser.displayName} (you)",
              email = currentUser.email,
              role = AccessRole.OWNER,
              isOwner = true,
              isInherited = false,
              canModify = false,
              onChangeRole = {},
              onRemove = {},
            )
          } else {
            userPerms.forEach { perm ->
              val isItemOwner = perm.role == AccessRole.OWNER ||
                (file.ownerEmail.isNotBlank() && perm.emailAddress.equals(file.ownerEmail, ignoreCase = true))
              val isCurrentUser = currentUser != null && perm.emailAddress.equals(currentUser.email, ignoreCase = true)

              PermissionPersonRow(
                name = when {
                  isItemOwner && isCurrentUser -> "${perm.displayName ?: "You"} (You, Owner)"
                  isItemOwner -> "${perm.displayName ?: perm.emailAddress ?: "Owner"} (Owner)"
                  isCurrentUser -> "${perm.displayName ?: "You"} (You)"
                  else -> perm.displayName ?: perm.emailAddress ?: "Collaborator"
                },
                email = perm.emailAddress ?: "",
                role = perm.role,
                isOwner = isItemOwner,
                isInherited = perm.isInherited,
                canModify = canManageSharing && !isItemOwner && !perm.isInherited && !isBusy,
                onChangeRole = { newRole -> onChangeRole(perm.id, newRole) },
                onRemove = { onRemovePermission(perm.id) },
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          file.parentId?.takeUnless { it == "root" }?.let { folderId ->
            TextButton(onClick = { onOpenFolderSharing(folderId) }, enabled = !isBusy && isOnline) {
              Text("Open parent folder sharing")
            }
          }
          Text("GENERAL ACCESS", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
          Spacer(modifier = Modifier.height(8.dp))

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, extra.border),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = if (anyonePerm != null) Icons.Filled.Public else Icons.Filled.Lock,
                  contentDescription = null,
                  tint = if (anyonePerm != null) extra.success else extra.textSecondary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (anyonePerm != null) "Anyone with the link" else "Restricted",
                    style = MaterialTheme.typography.labelLarge,
                  )
                  Text(
                    text =
                      if (anyonePerm?.isInherited == true) "Manage link access in the sharing folder"
                      else if (anyonePerm != null) "Anyone on the internet with this link can ${anyonePerm.role.label.lowercase()}"
                      else "Only people with access can open",
                    style = MaterialTheme.typography.bodySmall,
                    color = extra.textSecondary,
                  )
                }
                Switch(
                  checked = anyonePerm != null,
                  enabled = anyonePerm?.isInherited != true && canManageSharing && !isBusy,
                  onCheckedChange = { enabled ->
                    onSetAnyoneLink(enabled, anyonePerm?.role ?: AccessRole.VIEWER)
                  },
                )
              }

              if (anyonePerm != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = extra.border.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    "Link access role",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extra.textSecondary,
                  )
                  var showLinkRoleMenu by remember { mutableStateOf(false) }
                  Box {
                    OutlinedButton(
                      onClick = { showLinkRoleMenu = true },
                      enabled = !anyonePerm.isInherited && canManageSharing && !isBusy,
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                      modifier = Modifier.height(36.dp),
                    ) {
                      Text(anyonePerm.role.label, style = MaterialTheme.typography.labelMedium)
                      Spacer(modifier = Modifier.width(4.dp))
                      Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                      expanded = showLinkRoleMenu,
                      onDismissRequest = { showLinkRoleMenu = false },
                    ) {
                      listOf(AccessRole.VIEWER, AccessRole.COMMENTER, AccessRole.EDITOR).forEach { roleOption ->
                        DropdownMenuItem(
                          text = { Text(roleOption.label) },
                          trailingIcon = {
                            if (roleOption == anyonePerm.role) {
                              Icon(Icons.Filled.Check, contentDescription = null)
                            }
                          },
                          onClick = {
                            showLinkRoleMenu = false
                            onSetAnyoneLink(true, roleOption)
                          },
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          val link = file.webViewLink ?: "https://drive.google.com/file/d/${file.id}/view"
          OutlinedButton(
            onClick = { onCopyLink(link) },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_copy_link_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy link")
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionPersonRow(
  name: String,
  email: String,
  role: AccessRole,
  isOwner: Boolean = false,
  isInherited: Boolean = false,
  canModify: Boolean,
  onChangeRole: (AccessRole) -> Unit,
  onRemove: () -> Unit,
) {
  var menuOpen by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(name, style = MaterialTheme.typography.labelLarge)
      if (email.isNotBlank()) {
        Text(email, style = MaterialTheme.typography.bodySmall, color = LocalCloudBoxColors.current.textSecondary)
      }
      if (isInherited) {
        Text(
          "Manage this access in the sharing folder",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.tertiary,
        )
      }
    }
    if (isOwner || role == AccessRole.OWNER) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Owner",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    } else {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
          OutlinedButton(
            onClick = { if (canModify) menuOpen = true },
            enabled = canModify,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp),
          ) {
            Text(role.label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
          }
          DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            listOf(AccessRole.VIEWER, AccessRole.COMMENTER, AccessRole.EDITOR).forEach { r ->
              DropdownMenuItem(
                text = { Text(r.label) },
                trailingIcon = { if (r == role) Icon(Icons.Filled.Check, contentDescription = null) },
                onClick = {
                  menuOpen = false
                  onChangeRole(r)
                },
              )
            }
            HorizontalDivider()
            DropdownMenuItem(
              text = { Text("Remove access", color = MaterialTheme.colorScheme.error) },
              leadingIcon = {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              },
              onClick = {
                menuOpen = false
                onRemove()
              },
            )
          }
        }

        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
          onClick = onRemove,
          enabled = canModify,
          modifier = Modifier.size(36.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = "Remove access",
            tint = if (canModify) MaterialTheme.colorScheme.error else LocalCloudBoxColors.current.textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsSheet(
  file: CloudFile,
  locationName: String,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    FileDetailsContent(
      file = file,
      locationName = locationName,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
    )
  }
}

@Composable
fun FileDetailsContent(
  file: CloudFile,
  locationName: String,
  modifier: Modifier = Modifier,
) {
  val extra = LocalCloudBoxColors.current
  Column(modifier = modifier) {
    Text("Details", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(14.dp))
    DetailRow("Name", file.name)
    DetailRow("Type", "${file.category.label} (${file.mimeType})")
    DetailRow("Size", if (file.isFolder) "Folder" else FormatUtils.formatBytes(file.sizeBytes))
    DetailRow("Modified", FormatUtils.formatFullDateTime(file.modifiedTimeEpochMs))
    DetailRow("Created", FormatUtils.formatFullDateTime(file.createdTimeEpochMs))
    DetailRow("Owner", if (file.ownedByMe) "You" else "${file.ownerName} (${file.ownerEmail})")
    DetailRow("Location", locationName)
    DetailRow(
      "Sharing",
      when {
        file.isAnyoneWithLink -> "Anyone with the link"
        file.shared -> "Shared (${file.collaboratorCount} collaborators)"
        else -> "Private"
      },
    )
    if (!file.ownedByMe) {
      DetailRow("Your access", file.userRole.label)
    }
    if (!file.md5Checksum.isNullOrBlank()) {
      DetailRow("MD5", file.md5Checksum)
    }
    DetailRow("Offline", file.offlineStatus.label)
  }
}

@Composable
private fun DetailRow(label: String, value: String) {
  val extra = LocalCloudBoxColors.current
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = extra.textSecondary,
      modifier = Modifier.width(100.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun SheetActionRow(
  icon: ImageVector,
  label: String,
  subtitle: String? = null,
  enabled: Boolean = true,
  isDestructive: Boolean = false,
  onClick: () -> Unit,
  testTag: String,
) {
  val extra = LocalCloudBoxColors.current
  val fg =
    when {
      !enabled -> extra.textSecondary.copy(alpha = 0.45f)
      isDestructive -> MaterialTheme.colorScheme.error
      else -> MaterialTheme.colorScheme.onSurface
    }
  val tileBg =
    when {
      !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      isDestructive -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
      else -> MaterialTheme.colorScheme.primaryContainer
    }
  val iconTint =
    when {
      !enabled -> extra.textSecondary.copy(alpha = 0.45f)
      isDestructive -> MaterialTheme.colorScheme.error
      else -> MaterialTheme.colorScheme.primary
    }

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .clip(RoundedCornerShape(14.dp))
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 6.dp, vertical = 6.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tileBg),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
      if (!subtitle.isNullOrBlank()) {
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = extra.textSecondary)
      }
    }
  }
}
