package com.example.ui.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.model.CloudFile
import com.example.model.DriveUser
import com.example.model.FilterConfig
import com.example.model.ScreenDataState
import com.example.model.SharedSegment
import com.example.model.ViewMode
import com.example.ui.components.CloudBoxEmptyState
import com.example.ui.components.CloudBoxErrorCard
import com.example.ui.components.FileGridCard
import com.example.ui.components.FileGridSkeleton
import com.example.ui.components.FileListRow
import com.example.ui.components.FileListSkeleton
import com.example.ui.components.UserAvatarButton
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedScreen(
  user: DriveUser?,
  segment: SharedSegment,
  selectedOwnerFilter: String?,
  sharedState: ScreenDataState<List<CloudFile>>,
  openedSharedIds: Set<String>,
  isRefreshing: Boolean,
  imageLoader: ImageLoader,
  showMenuButton: Boolean,
  onOpenDrawer: () -> Unit,
  onSelectSegment: (SharedSegment) -> Unit,
  onSelectOwnerFilter: (String?) -> Unit,
  onOpenSortFilter: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenFile: (CloudFile) -> Unit,
  onFileMoreClick: (CloudFile) -> Unit,
  onShareFileShortcut: () -> Unit,
  onRefresh: () -> Unit,
  onReconnectDrive: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  val rawItems = (sharedState as? ScreenDataState.Success)?.data.orEmpty()
  val dynamicOwners =
    rawItems
      .mapNotNull { it.sharingUserName ?: it.ownerName.takeIf { n -> n != "You" } }
      .distinct()

  val visibleItems =
    if (selectedOwnerFilter == null) rawItems
    else rawItems.filter { (it.sharingUserName ?: it.ownerName) == selectedOwnerFilter }

  Column(modifier = Modifier.fillMaxSize().testTag("shared_screen")) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (showMenuButton) {
        IconButton(onClick = onOpenDrawer, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
        }
      }
      Text(
        text = "Shared",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onOpenSearch, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Filled.Search, contentDescription = "Search")
      }
      UserAvatarButton(user = user, onClick = onOpenSettings)
    }

    // Segmented control: Shared with me | By me
    Surface(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, extra.border),
    ) {
      Row(modifier = Modifier.padding(4.dp)) {
        SharedSegment.entries.forEach { seg ->
          val active = segment == seg
          Box(
            modifier =
              Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .clickable { onSelectSegment(seg) }
                .testTag("shared_segment_${seg.name.lowercase()}"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = seg.label,
              style = MaterialTheme.typography.labelLarge,
              color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    if (segment == SharedSegment.WITH_ME && dynamicOwners.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedOwnerFilter == null,
          onClick = { onSelectOwnerFilter(null) },
          label = { Text("Everyone") },
          shape = RoundedCornerShape(20.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
          ),
        )
        dynamicOwners.forEach { owner ->
          FilterChip(
            selected = selectedOwnerFilter == owner,
            onClick = { onSelectOwnerFilter(owner) },
            label = { Text(owner) },
            shape = RoundedCornerShape(20.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
          )
        }
      }
    } else {
      Spacer(modifier = Modifier.height(6.dp))
    }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
      FilterChip(
        selected = false,
        onClick = onOpenSortFilter,
        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text("Sort & filter") },
        shape = RoundedCornerShape(20.dp),
      )
    }

    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
      when (sharedState) {
        is ScreenDataState.Loading -> {
          Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            FileListSkeleton(5)
          }
        }
        is ScreenDataState.Error -> {
          Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CloudBoxErrorCard(
              message = sharedState.message,
              actionLabel = if (sharedState.isAuthExpired) "Reconnect" else "Retry",
              onRetry = if (sharedState.isAuthExpired) onReconnectDrive else onRefresh,
            )
          }
        }
        is ScreenDataState.Empty -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (segment == SharedSegment.WITH_ME) {
              CloudBoxEmptyState(
                title = "Nothing shared with you yet",
                subtitle = "When someone shares a file with your Google account, it will appear here.",
                icon = Icons.Filled.People,
              )
            } else {
              CloudBoxEmptyState(
                title = "You haven’t shared anything",
                subtitle = "Open My Files and share any file or folder with teammates.",
                primaryActionLabel = "Browse files to share",
                onPrimaryAction = onShareFileShortcut,
                icon = Icons.Filled.People,
              )
            }
          }
        }
        is ScreenDataState.Success -> {
          val grouped = visibleItems.groupBy { FormatUtils.daysAgoGroup(it.modifiedTimeEpochMs) }
          val sectionOrder = listOf("TODAY", "THIS WEEK", "EARLIER")
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            sectionOrder.forEach { group ->
              val itemsInGroup = grouped[group].orEmpty()
              if (itemsInGroup.isNotEmpty()) {
                item(key = "hdr_$group") {
                  Text(
                    text = group,
                    style = MaterialTheme.typography.labelSmall,
                    color = extra.textSecondary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                  )
                }
                items(itemsInGroup.size, key = { idx -> "${group}_${itemsInGroup[idx].id}" }) { idx ->
                  val file = itemsInGroup[idx]
                  val sub =
                    if (segment == SharedSegment.WITH_ME) {
                      "${file.sharingUserName ?: file.ownerName} · ${FormatUtils.formatBytes(file.sizeBytes)}"
                    } else {
                      val linkText = if (file.isAnyoneWithLink) "Anyone with link" else "Restricted"
                      val peopleText = if (file.collaboratorCount > 0) "${file.collaboratorCount} people · " else ""
                      "$peopleText$linkText"
                    }
                  FileListRow(
                    file = file,
                    isSelected = false,
                    imageLoader = imageLoader,
                    onClick = { onOpenFile(file) },
                    onLongClick = { onFileMoreClick(file) },
                    onMoreClick = { onFileMoreClick(file) },
                    subtitleOverride = sub,
                    roleBadge = if (segment == SharedSegment.WITH_ME) file.userRole else null,
                    showUnopenedDot = segment == SharedSegment.WITH_ME && file.id !in openedSharedIds,
                  )
                }
              }
            }
            item { Spacer(modifier = Modifier.height(84.dp)) }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredScreen(
  user: DriveUser?,
  starredState: ScreenDataState<List<CloudFile>>,
  filter: FilterConfig,
  viewMode: ViewMode,
  isRefreshing: Boolean,
  imageLoader: ImageLoader,
  showMenuButton: Boolean,
  onOpenDrawer: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenSortFilter: () -> Unit,
  onToggleViewMode: () -> Unit,
  onOpenFile: (CloudFile) -> Unit,
  onFileMoreClick: (CloudFile) -> Unit,
  onBrowseFiles: () -> Unit,
  onRefresh: () -> Unit,
  onReconnectDrive: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current

  Column(modifier = Modifier.fillMaxSize().testTag("starred_screen")) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (showMenuButton) {
        IconButton(onClick = onOpenDrawer, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
        }
      }
      Text(
        text = "Starred",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onOpenSearch, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Filled.Search, contentDescription = "Search")
      }
      UserAvatarButton(user = user, onClick = onOpenSettings)
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = filter.hasActiveFilters,
        onClick = onOpenSortFilter,
        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text("Sort & filter") },
        shape = RoundedCornerShape(20.dp),
      )
      Spacer(modifier = Modifier.weight(1f))
      Surface(
        onClick = onToggleViewMode,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.border),
        modifier = Modifier.height(36.dp),
      ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
            contentDescription = "Toggle view",
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (viewMode == ViewMode.GRID) "Grid" else "List", style = MaterialTheme.typography.labelLarge)
        }
      }
    }

    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPad = if (maxWidth >= 840.dp) 24.dp else 16.dp
        val columns = ((maxWidth - horizontalPad * 2) / 164.dp).toInt().coerceIn(2, 6)

        when (starredState) {
          is ScreenDataState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
              if (viewMode == ViewMode.GRID) FileGridSkeleton(4) else FileListSkeleton(5)
            }
          }
          is ScreenDataState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
              CloudBoxErrorCard(
                message = starredState.message,
                actionLabel = if (starredState.isAuthExpired) "Reconnect" else "Retry",
                onRetry = if (starredState.isAuthExpired) onReconnectDrive else onRefresh,
              )
            }
          }
          is ScreenDataState.Empty -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CloudBoxEmptyState(
                title = "No starred files",
                subtitle = "Star important files to find them fast from anywhere.",
                primaryActionLabel = "Browse files",
                onPrimaryAction = onBrowseFiles,
                icon = Icons.Filled.StarOutline,
              )
            }
          }
          is ScreenDataState.Success -> {
            val items = starredState.data
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (viewMode == ViewMode.GRID) {
                val rows = items.chunked(columns)
                items(rows.size, key = { idx -> "sg_${rows[idx].first().id}" }) { idx ->
                  val row = rows[idx]
                  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { file ->
                      FileGridCard(
                        file = file,
                        isSelected = false,
                        imageLoader = imageLoader,
                        onClick = { onOpenFile(file) },
                        onLongClick = { onFileMoreClick(file) },
                        onMoreClick = { onFileMoreClick(file) },
                        modifier = Modifier.weight(1f),
                      )
                    }
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                  }
                }
              } else {
                items(items.size, key = { idx -> "sl_${items[idx].id}" }) { idx ->
                  val file = items[idx]
                  FileListRow(
                    file = file,
                    isSelected = false,
                    imageLoader = imageLoader,
                    onClick = { onOpenFile(file) },
                    onLongClick = { onFileMoreClick(file) },
                    onMoreClick = { onFileMoreClick(file) },
                  )
                }
              }
              item { Spacer(modifier = Modifier.height(84.dp)) }
            }
          }
        }
      }
    }
  }
}
