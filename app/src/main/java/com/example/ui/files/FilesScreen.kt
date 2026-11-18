package com.example.ui.files

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.model.BreadcrumbSegment
import com.example.model.CloudFile
import com.example.model.DriveUser
import com.example.model.FilterConfig
import com.example.model.ScreenDataState
import com.example.model.ViewMode
import com.example.ui.components.CloudBoxEmptyState
import com.example.ui.components.CloudBoxErrorCard
import com.example.ui.components.FileGridCard
import com.example.ui.components.FileGridSkeleton
import com.example.ui.components.FileListRow
import com.example.ui.components.FileListSkeleton
import com.example.ui.components.UserAvatarButton
import com.example.ui.theme.LocalCloudBoxColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
  user: DriveUser?,
  breadcrumbs: List<BreadcrumbSegment>,
  filesState: ScreenDataState<List<CloudFile>>,
  filter: FilterConfig,
  viewMode: ViewMode,
  selectedIds: Set<String>,
  hasMorePages: Boolean,
  isLoadingMore: Boolean,
  isRefreshing: Boolean,
  imageLoader: ImageLoader,
  showMenuButton: Boolean,
  onOpenDrawer: () -> Unit,
  onNavigateUp: () -> Unit,
  onSelectBreadcrumb: (Int) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenSortFilter: () -> Unit,
  onToggleViewMode: () -> Unit,
  onOpenFolder: (CloudFile) -> Unit,
  onOpenFile: (CloudFile) -> Unit,
  onFileMoreClick: (CloudFile) -> Unit,
  onToggleSelect: (String) -> Unit,
  onSelectAll: (List<CloudFile>) -> Unit,
  onClearSelection: () -> Unit,
  onBatchStar: (List<CloudFile>) -> Unit,
  onBatchTrash: (List<CloudFile>) -> Unit,
  onUploadClick: () -> Unit,
  onNewFolderClick: () -> Unit,
  onClearFilters: () -> Unit,
  onLoadMore: () -> Unit,
  onRefresh: () -> Unit,
  onReconnectDrive: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  val currentItems = (filesState as? ScreenDataState.Success)?.data.orEmpty()
  val isSelectionMode = selectedIds.isNotEmpty()
  val currentTitle = breadcrumbs.lastOrNull()?.name ?: "My Files"

  val listState = rememberLazyListState()
  val shouldLoadMore by remember(hasMorePages, isLoadingMore) {
    derivedStateOf {
      val layoutInfo = listState.layoutInfo
      val total = layoutInfo.totalItemsCount
      val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      hasMorePages && !isLoadingMore && total > 4 && lastVisible >= total - 5
    }
  }
  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) onLoadMore()
  }

  Column(modifier = Modifier.fillMaxSize().testTag("files_screen")) {
    // Top Header or Selection Mode Action Bar
    if (isSelectionMode) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().testTag("selection_top_bar"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = onClearSelection, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Exit selection")
          }
          Text(
            text = "${selectedIds.size} selected",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
          )
          IconButton(onClick = { onSelectAll(currentItems) }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
          }
          IconButton(onClick = { onBatchStar(currentItems) }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.StarOutline, contentDescription = "Star selected")
          }
          IconButton(onClick = { onBatchTrash(currentItems) }, modifier = Modifier.size(48.dp)) {
            Icon(
              Icons.Filled.DeleteOutline,
              contentDescription = "Move selected to trash",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (breadcrumbs.size > 1) {
          IconButton(onClick = onNavigateUp, modifier = Modifier.size(48.dp).testTag("folder_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up one folder")
          }
        } else if (showMenuButton) {
          IconButton(onClick = onOpenDrawer, modifier = Modifier.size(48.dp).testTag("files_drawer_button")) {
            Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
          }
        }
        Text(
          text = currentTitle,
          style = MaterialTheme.typography.headlineMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenSearch, modifier = Modifier.size(48.dp).testTag("files_search_button")) {
          Icon(Icons.Filled.Search, contentDescription = "Search files")
        }
        UserAvatarButton(user = user, onClick = onOpenSettings)
      }
    }

    // Breadcrumb trail (collapses middle when > 3 segments)
    if (breadcrumbs.size > 1) {
      val scrollState = rememberScrollState()
      LaunchedEffect(breadcrumbs.size) { scrollState.animateScrollTo(scrollState.maxValue) }
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        breadcrumbs.forEachIndexed { index, seg ->
          val isMiddleCollapsed = breadcrumbs.size > 4 && index in 1..(breadcrumbs.size - 3)
          if (isMiddleCollapsed) {
            if (index == 1) {
              Text("…", style = MaterialTheme.typography.labelLarge, color = extra.textSecondary)
              Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = extra.textSecondary,
                modifier = Modifier.size(16.dp),
              )
            }
          } else {
            val isCurrent = index == breadcrumbs.lastIndex
            Text(
              text = seg.name,
              style =
                MaterialTheme.typography.labelLarge.copy(
                  fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                ),
              color = if (isCurrent) MaterialTheme.colorScheme.primary else extra.textSecondary,
              modifier =
                Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable(enabled = !isCurrent) { onSelectBreadcrumb(index) }
                  .padding(horizontal = 6.dp, vertical = 4.dp),
            )
            if (!isCurrent) {
              Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = extra.textSecondary,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }
    }

    // Sort & filter bar + Grid/List toggle
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = filter.hasActiveFilters,
        onClick = onOpenSortFilter,
        leadingIcon = {
          Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        label = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${filter.sortField.label} ${if (filter.ascending) "↑" else "↓"}")
            if (filter.hasActiveFilters) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier =
                  Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
              )
            }
          }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(36.dp).testTag("files_sort_filter_chip"),
      )

      Spacer(modifier = Modifier.weight(1f))

      Surface(
        onClick = onToggleViewMode,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.border),
        modifier = Modifier.height(36.dp).testTag("files_view_toggle_chip"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
            contentDescription = "Switch to ${if (viewMode == ViewMode.GRID) "List" else "Grid"} view",
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (viewMode == ViewMode.GRID) "Grid" else "List",
            style = MaterialTheme.typography.labelLarge,
          )
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

        when (filesState) {
          is ScreenDataState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = horizontalPad, vertical = 8.dp)) {
              if (viewMode == ViewMode.GRID) FileGridSkeleton(6) else FileListSkeleton(7)
            }
          }
          is ScreenDataState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
              CloudBoxErrorCard(
                message = filesState.message,
                actionLabel = if (filesState.isAuthExpired) "Reconnect Google Drive" else "Retry",
                onRetry = if (filesState.isAuthExpired) onReconnectDrive else onRefresh,
              )
            }
          }
          is ScreenDataState.Empty -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              if (filter.hasActiveFilters) {
                CloudBoxEmptyState(
                  title = "No files match your filter",
                  subtitle = "Adjust or reset your type, owner, or date filters.",
                  primaryActionLabel = "Clear filter",
                  onPrimaryAction = onClearFilters,
                  useIllustration = false,
                )
              } else {
                CloudBoxEmptyState(
                  title = "This folder is empty",
                  subtitle = "Tap + to add files or create a folder",
                  primaryActionLabel = "Upload a file",
                  onPrimaryAction = onUploadClick,
                  secondaryActionLabel = "New folder",
                  onSecondaryAction = onNewFolderClick,
                )
              }
            }
          }
          is ScreenDataState.Success -> {
            val folders = filesState.data.filter { it.isFolder }
            val files = filesState.data.filter { !it.isFolder }

            AnimatedContent(
              targetState = viewMode,
              transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
              label = "gridListMorph",
            ) { mode ->
              LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                if (folders.isNotEmpty()) {
                  item(key = "header_folders") {
                    Text(
                      text = "FOLDERS",
                      style = MaterialTheme.typography.labelSmall,
                      color = extra.textSecondary,
                      modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                  }
                  if (mode == ViewMode.GRID) {
                    val folderRows = folders.chunked(columns)
                    items(folderRows.size, key = { idx -> "fg_${folderRows[idx].first().id}" }) { idx ->
                      val row = folderRows[idx]
                      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { folder ->
                          FileGridCard(
                            file = folder,
                            isSelected = folder.id in selectedIds,
                            imageLoader = imageLoader,
                            onClick = {
                              if (isSelectionMode) onToggleSelect(folder.id) else onOpenFolder(folder)
                            },
                            onLongClick = { onToggleSelect(folder.id) },
                            onMoreClick = { onFileMoreClick(folder) },
                            modifier = Modifier.weight(1f),
                          )
                        }
                        repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                      }
                    }
                  } else {
                    items(folders.size, key = { idx -> "fl_${folders[idx].id}" }) { idx ->
                      val folder = folders[idx]
                      FileListRow(
                        file = folder,
                        isSelected = folder.id in selectedIds,
                        imageLoader = imageLoader,
                        onClick = {
                          if (isSelectionMode) onToggleSelect(folder.id) else onOpenFolder(folder)
                        },
                        onLongClick = { onToggleSelect(folder.id) },
                        onMoreClick = { onFileMoreClick(folder) },
                      )
                    }
                  }
                }

                if (files.isNotEmpty()) {
                  item(key = "header_files") {
                    Text(
                      text = "FILES",
                      style = MaterialTheme.typography.labelSmall,
                      color = extra.textSecondary,
                      modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                  }
                  if (mode == ViewMode.GRID) {
                    val fileRows = files.chunked(columns)
                    items(fileRows.size, key = { idx -> "fgr_${fileRows[idx].first().id}" }) { idx ->
                      val row = fileRows[idx]
                      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { file ->
                          FileGridCard(
                            file = file,
                            isSelected = file.id in selectedIds,
                            imageLoader = imageLoader,
                            onClick = {
                              if (isSelectionMode) onToggleSelect(file.id) else onOpenFile(file)
                            },
                            onLongClick = { onToggleSelect(file.id) },
                            onMoreClick = { onFileMoreClick(file) },
                            modifier = Modifier.weight(1f),
                          )
                        }
                        repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                      }
                    }
                  } else {
                    items(files.size, key = { idx -> "flr_${files[idx].id}" }) { idx ->
                      val file = files[idx]
                      FileListRow(
                        file = file,
                        isSelected = file.id in selectedIds,
                        imageLoader = imageLoader,
                        onClick = {
                          if (isSelectionMode) onToggleSelect(file.id) else onOpenFile(file)
                        },
                        onLongClick = { onToggleSelect(file.id) },
                        onMoreClick = { onFileMoreClick(file) },
                      )
                    }
                  }
                }

                if (isLoadingMore) {
                  item(key = "loading_more") {
                    Box(
                      modifier = Modifier.fillMaxWidth().padding(12.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    }
                  }
                }

                item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(84.dp)) }
              }
            }
          }
        }
      }
    }
  }
}
