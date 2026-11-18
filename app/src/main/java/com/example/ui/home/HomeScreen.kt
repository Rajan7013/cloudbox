package com.example.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.model.CloudFile
import com.example.model.DriveUser
import com.example.model.FileCategory
import com.example.model.ScreenDataState
import com.example.model.StorageQuotaInfo
import com.example.ui.components.CloudBoxEmptyState
import com.example.ui.components.CloudBoxErrorCard
import com.example.ui.components.FileGridCard
import com.example.ui.components.FileGridSkeleton
import com.example.ui.components.SearchTriggerBar
import com.example.ui.components.UserAvatarButton
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  user: DriveUser?,
  quotaState: ScreenDataState<StorageQuotaInfo>,
  recentState: ScreenDataState<List<CloudFile>>,
  selectedQuickCategory: FileCategory?,
  isRefreshing: Boolean,
  imageLoader: ImageLoader,
  showMenuButton: Boolean,
  onOpenDrawer: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenStorage: () -> Unit,
  onSelectQuickCategory: (FileCategory?) -> Unit,
  onOpenAllFiles: () -> Unit,
  onOpenFile: (CloudFile) -> Unit,
  onFileMoreClick: (CloudFile) -> Unit,
  onUploadFirstFile: () -> Unit,
  onRefresh: () -> Unit,
  onReconnectDrive: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  val firstName = user?.displayName?.substringBefore(' ')?.takeIf { it.isNotBlank() } ?: "there"

  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    modifier = Modifier.fillMaxSize().testTag("home_screen"),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val width = maxWidth
      val horizontalPad = if (width >= 840.dp) 28.dp else if (width >= 600.dp) 24.dp else 16.dp
      val columns = ((width - horizontalPad * 2) / 164.dp).toInt().coerceIn(2, 5)

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Greeting & Avatar
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (showMenuButton) {
              IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.size(48.dp).testTag("home_drawer_button"),
              ) {
                Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
              }
              Spacer(modifier = Modifier.width(4.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Hello, $firstName",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
              )
              Text(
                text = "Your Google Drive workspace",
                style = MaterialTheme.typography.bodySmall,
                color = extra.textSecondary,
              )
            }
            UserAvatarButton(user = user, onClick = onOpenSettings)
          }
        }

        // Search Bar
        item {
          SearchTriggerBar(onClick = onOpenSearch)
        }

        // Hero Storage Card
        item {
          StorageHeroCard(
            quotaState = quotaState,
            onClick = onOpenStorage,
            onRetry = onRefresh,
            onReconnectDrive = onReconnectDrive,
          )
        }

        // Quick Access Chips
        item {
          Column {
            Text(
              text = "Quick access",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(10.dp))
            val quickCategories =
              listOf(
                FileCategory.IMAGE to "Images",
                FileCategory.VIDEO to "Video",
                FileCategory.AUDIO to "Audio",
                FileCategory.PDF to "PDF",
                FileCategory.DOCUMENT to "Docs",
                FileCategory.SPREADSHEET to "Sheets",
                FileCategory.ARCHIVE to "Archives",
              )
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              quickCategories.forEach { (cat, label) ->
                val selected = selectedQuickCategory == cat
                FilterChip(
                  selected = selected,
                  onClick = { onSelectQuickCategory(cat) },
                  label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                  shape = RoundedCornerShape(20.dp),
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primary,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                  modifier = Modifier.height(38.dp).testTag("quick_chip_${cat.name.lowercase()}"),
                )
              }
            }
          }
        }

        // Recent Section Header
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (selectedQuickCategory != null) "Recent ${selectedQuickCategory.label}s" else "Recent",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.weight(1f),
            )
            TextButton(
              onClick = onOpenAllFiles,
              modifier = Modifier.testTag("home_see_all_button"),
            ) {
              Text("See all")
            }
          }
        }

        // Recent Content State
        when (recentState) {
          is ScreenDataState.Loading -> {
            item { FileGridSkeleton(count = 4) }
          }
          is ScreenDataState.Error -> {
            item {
              CloudBoxErrorCard(
                message = recentState.message,
                actionLabel = if (recentState.isAuthExpired) "Reconnect" else "Retry",
                onRetry = if (recentState.isAuthExpired) onReconnectDrive else onRefresh,
              )
            }
          }
          is ScreenDataState.Empty -> {
            item {
              CloudBoxEmptyState(
                title = "Nothing here yet",
                subtitle = "Upload your first file to see it here",
                primaryActionLabel = "Upload a file",
                onPrimaryAction = onUploadFirstFile,
              )
            }
          }
          is ScreenDataState.Success -> {
            val filtered =
              recentState.data
                .let { list ->
                  if (selectedQuickCategory == null) list else list.filter { it.category == selectedQuickCategory }
                }
                .take(8)
            if (filtered.isEmpty()) {
              item {
                CloudBoxEmptyState(
                  title = "No matching recent files",
                  subtitle = "Try another Quick access category or browse My Files",
                  primaryActionLabel = "Clear filter",
                  onPrimaryAction = { onSelectQuickCategory(null) },
                  useIllustration = false,
                )
              }
            } else {
              val chunked = filtered.chunked(columns)
              items(chunked.size) { rowIdx ->
                val rowFiles = chunked[rowIdx]
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  rowFiles.forEach { file ->
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
                  repeat(columns - rowFiles.size) {
                    Spacer(modifier = Modifier.weight(1f))
                  }
                }
              }
            }
          }
        }

        item { Spacer(modifier = Modifier.height(84.dp)) }
      }
    }
  }
}

@Composable
private fun StorageHeroCard(
  quotaState: ScreenDataState<StorageQuotaInfo>,
  onClick: () -> Unit,
  onRetry: () -> Unit,
  onReconnectDrive: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current

  when (quotaState) {
    is ScreenDataState.Loading -> {
      Surface(
        modifier = Modifier.fillMaxWidth().height(126.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Box(modifier = Modifier.padding(18.dp)) {
          Text("Loading Google Drive storage…", style = MaterialTheme.typography.bodyMedium, color = extra.textSecondary)
        }
      }
    }
    is ScreenDataState.Error -> {
      CloudBoxErrorCard(
        message = quotaState.message,
        actionLabel = if (quotaState.isAuthExpired) "Reconnect" else "Retry",
        onRetry = if (quotaState.isAuthExpired) onReconnectDrive else onRetry,
      )
    }
    is ScreenDataState.Empty -> Unit
    is ScreenDataState.Success -> {
      val quota = quotaState.data
      val animatedFraction by animateFloatAsState(
        targetValue = quota.usageFraction,
        animationSpec = tween(600),
        label = "storageFraction",
      )
      val gradient =
        if (quota.isAlmostFull) {
          Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFF59E0B)))
        } else {
          Brush.linearGradient(listOf(extra.heroGradientStart, extra.heroGradientEnd))
        }

      Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("home_storage_hero_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
      ) {
        Box(modifier = Modifier.fillMaxWidth().background(gradient).padding(18.dp)) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "Google Drive storage",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f),
              )
              if (quota.isAlmostFull) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
              }
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Manage storage",
                tint = Color.White,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "${FormatUtils.formatBytes(quota.usageBytes)} of ${FormatUtils.formatBytes(quota.limitBytes)}",
              style = MaterialTheme.typography.headlineMedium,
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
              progress = { animatedFraction },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
              color = Color.White,
              trackColor = Color.White.copy(alpha = 0.25f),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text =
                if (quota.isAlmostFull) "Almost full · Tap to manage storage"
                else "In Drive: ${FormatUtils.formatBytes(quota.usageInDriveBytes)} · Trash: ${FormatUtils.formatBytes(quota.usageInTrashBytes)}",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.9f),
            )
          }
        }
      }
    }
  }
}
