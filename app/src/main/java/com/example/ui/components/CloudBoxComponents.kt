package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.AccessRole
import com.example.model.CloudFile
import com.example.model.DriveUser
import com.example.model.FileCategory
import com.example.model.OfflineSyncStatus
import com.example.model.UploadStatus
import com.example.model.UploadTaskState
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils

fun iconForCategory(category: FileCategory): ImageVector =
  when (category) {
    FileCategory.FOLDER -> Icons.Filled.Folder
    FileCategory.DOCUMENT -> Icons.Filled.Description
    FileCategory.SPREADSHEET -> Icons.Filled.TableChart
    FileCategory.PDF -> Icons.Filled.PictureAsPdf
    FileCategory.IMAGE -> Icons.Filled.Image
    FileCategory.VIDEO -> Icons.Filled.VideoFile
    FileCategory.AUDIO -> Icons.Filled.AudioFile
    FileCategory.ARCHIVE -> Icons.Filled.FolderZip
    FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
  }

@Composable
fun FileTypeTile(
  category: FileCategory,
  thumbnailLink: String? = null,
  imageLoader: ImageLoader? = null,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  cornerRadius: Dp = 12.dp,
) {
  val extra = LocalCloudBoxColors.current
  val tintColor = extra.colorForCategory(category)
  Box(
    modifier =
      modifier
        .size(size)
        .clip(RoundedCornerShape(cornerRadius))
        .background(tintColor.copy(alpha = 0.13f)),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = iconForCategory(category),
      contentDescription = category.label,
      tint = tintColor,
      modifier = Modifier.size(size * 0.52f),
    )
    if (!thumbnailLink.isNullOrBlank() && imageLoader != null) {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(thumbnailLink).crossfade(200).build(),
        imageLoader = imageLoader,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridCard(
  file: CloudFile,
  isSelected: Boolean,
  imageLoader: ImageLoader,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onMoreClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val extra = LocalCloudBoxColors.current
  val tintColor = extra.colorForCategory(file.category)
  val cardBg by
    animateColorAsState(
      targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
      animationSpec = tween(150),
      label = "gridCardBg",
    )
  val borderColor =
    when {
      isSelected -> MaterialTheme.colorScheme.primary
      extra.isDark -> extra.border
      else -> extra.border.copy(alpha = 0.7f)
    }

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("file_grid_card_${file.id}")
        .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = if (extra.isDark) 0.dp else 2.dp),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tintColor.copy(alpha = 0.13f)),
      ) {
        Icon(
          imageVector = iconForCategory(file.category),
          contentDescription = file.category.label,
          tint = tintColor,
          modifier = Modifier.size(34.dp).align(Alignment.Center),
        )
        if (!file.thumbnailLink.isNullOrBlank()) {
          AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(file.thumbnailLink).crossfade(200).build(),
            imageLoader = imageLoader,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
        if (isSelected) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.primary,
            modifier =
              Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(22.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
          )
        } else {
          IconButton(
            onClick = onMoreClick,
            modifier = Modifier.align(Alignment.TopEnd).size(40.dp).testTag("file_more_${file.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.MoreVert,
              contentDescription = "More actions for ${file.name}",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = FormatUtils.middleEllipsis(file.name, maxChars = 22),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        val metaText =
          buildString {
            if (!file.isFolder && file.sizeBytes > 0L) {
              append(FormatUtils.formatBytes(file.sizeBytes))
              append(" · ")
            }
            append(FormatUtils.formatRelativeDate(file.modifiedTimeEpochMs))
          }
        Text(
          text = metaText,
          style = MaterialTheme.typography.bodySmall,
          color = extra.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        if (file.starred) {
          Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Starred",
            tint = extra.warning,
            modifier = Modifier.size(14.dp),
          )
        }
        if (file.shared) {
          Icon(
            imageVector = Icons.Filled.People,
            contentDescription = "Shared",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
          )
        }
        if (file.offlineStatus == OfflineSyncStatus.SAVED) {
          Icon(
            imageVector = Icons.Filled.OfflinePin,
            contentDescription = "Available offline",
            tint = extra.success,
            modifier = Modifier.size(14.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListRow(
  file: CloudFile,
  isSelected: Boolean,
  imageLoader: ImageLoader,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onMoreClick: () -> Unit,
  subtitleOverride: String? = null,
  roleBadge: AccessRole? = null,
  showUnopenedDot: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val extra = LocalCloudBoxColors.current
  val rowBg by
    animateColorAsState(
      targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
      animationSpec = tween(150),
      label = "listRowBg",
    )

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("file_list_row_${file.id}")
        .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    shape = RoundedCornerShape(16.dp),
    color = rowBg,
    border =
      BorderStroke(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) MaterialTheme.colorScheme.primary else extra.border.copy(alpha = 0.7f),
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (isSelected) {
        Box(
          modifier =
            Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.onPrimary,
          )
        }
      } else {
        FileTypeTile(
          category = file.category,
          thumbnailLink = file.thumbnailLink,
          imageLoader = imageLoader,
          size = 44.dp,
          cornerRadius = 12.dp,
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = FormatUtils.middleEllipsis(file.name, maxChars = 32),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (showUnopenedDot) {
            Box(
              modifier = Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error),
            )
          }
          if (file.starred) {
            Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = "Starred",
              tint = extra.warning,
              modifier = Modifier.size(14.dp),
            )
          }
          if (file.offlineStatus == OfflineSyncStatus.SAVED) {
            Icon(
              imageVector = Icons.Filled.OfflinePin,
              contentDescription = "Saved offline",
              tint = extra.success,
              modifier = Modifier.size(14.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        val sub =
          subtitleOverride
            ?: buildString {
              if (!file.isFolder && file.sizeBytes > 0L) {
                append(FormatUtils.formatBytes(file.sizeBytes))
                append(" · ")
              }
              append(FormatUtils.formatRelativeDate(file.modifiedTimeEpochMs))
              if (file.shared) append(" · Shared")
            }
        Text(
          text = sub,
          style = MaterialTheme.typography.bodySmall,
          color = extra.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (roleBadge != null) {
        RoleBadgeChip(role = roleBadge)
        Spacer(modifier = Modifier.width(4.dp))
      }

      IconButton(
        onClick = onMoreClick,
        modifier = Modifier.size(48.dp).testTag("file_more_${file.id}"),
      ) {
        Icon(
          imageVector = Icons.Filled.MoreVert,
          contentDescription = "Actions for ${file.name}",
          tint = extra.textSecondary,
        )
      }
    }
  }
}

@Composable
fun RoleBadgeChip(role: AccessRole, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
  ) {
    Text(
      text = role.label,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun UserAvatarButton(
  user: DriveUser?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val initial = user?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "C"
  Surface(
    onClick = onClick,
    modifier = modifier.size(48.dp).testTag("avatar_settings_button"),
    shape = CircleShape,
    color = MaterialTheme.colorScheme.primaryContainer,
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (!user?.photoUrl.isNullOrBlank()) {
        AsyncImage(
          model = user?.photoUrl,
          contentDescription = "Account and Settings",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
      } else {
        Text(
          text = initial,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
fun SearchTriggerBar(
  onClick: () -> Unit,
  placeholder: String = "Search in CloudBox",
  modifier: Modifier = Modifier,
) {
  val extra = LocalCloudBoxColors.current
  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().height(48.dp).testTag("search_trigger_bar"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, extra.border),
    shadowElevation = if (extra.isDark) 0.dp else 2.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Filled.Search,
        contentDescription = "Search",
        tint = extra.textSecondary,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = placeholder,
        style = MaterialTheme.typography.bodyLarge,
        color = extra.textSecondary,
      )
    }
  }
}

@Composable
fun OfflineStatusBanner(modifier: Modifier = Modifier) {
  val extra = LocalCloudBoxColors.current
  Surface(
    modifier = modifier.fillMaxWidth().testTag("offline_banner"),
    color = extra.warning.copy(alpha = 0.16f),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Filled.CloudOff,
        contentDescription = null,
        tint = extra.warning,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "You are offline. Showing saved files.",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
fun ShimmerBrush(): Brush {
  val transition = rememberInfiniteTransition(label = "shimmer")
  val shift by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1000f,
    animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
    label = "shimmerShift",
  )
  val base = MaterialTheme.colorScheme.surfaceVariant
  val highlight = MaterialTheme.colorScheme.surface
  return Brush.linearGradient(
    colors = listOf(base, highlight, base),
    start = Offset(shift - 300f, 0f),
    end = Offset(shift, 300f),
  )
}

@Composable
fun FileGridSkeleton(count: Int = 4) {
  val brush = ShimmerBrush()
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    for (row in 0 until (count + 1) / 2) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
          Surface(
            modifier = Modifier.weight(1f).height(148.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Box(modifier = Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(12.dp)).background(brush))
              Spacer(modifier = Modifier.height(10.dp))
              Box(modifier = Modifier.fillMaxWidth(0.75f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(brush))
              Spacer(modifier = Modifier.height(6.dp))
              Box(modifier = Modifier.fillMaxWidth(0.5f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(brush))
            }
          }
        }
      }
    }
  }
}

@Composable
fun FileListSkeleton(count: Int = 6) {
  val brush = ShimmerBrush()
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    repeat(count) {
      Surface(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
      ) {
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(brush))
          Spacer(modifier = Modifier.width(12.dp))
          Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(brush))
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(brush))
          }
        }
      }
    }
  }
}

@Composable
fun CloudBoxEmptyState(
  title: String,
  subtitle: String,
  primaryActionLabel: String? = null,
  onPrimaryAction: (() -> Unit)? = null,
  secondaryActionLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
  icon: ImageVector? = null,
  useIllustration: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val extra = LocalCloudBoxColors.current
  Column(
    modifier = modifier.fillMaxWidth().padding(24.dp).testTag("empty_state"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (useIllustration && icon == null) {
      Image(
        painter = painterResource(id = R.drawable.img_cloudbox_empty),
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(160.dp).clip(RoundedCornerShape(24.dp)),
      )
    } else if (icon != null) {
      Box(
        modifier =
          Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(44.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyLarge,
      color = extra.textSecondary,
    )
    if (primaryActionLabel != null && onPrimaryAction != null) {
      Spacer(modifier = Modifier.height(20.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (secondaryActionLabel != null && onSecondaryAction != null) {
          OutlinedButton(
            onClick = onSecondaryAction,
            modifier = Modifier.height(48.dp).testTag("empty_secondary_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(secondaryActionLabel)
          }
        }
        Button(
          onClick = onPrimaryAction,
          modifier = Modifier.height(48.dp).testTag("empty_primary_button"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Text(primaryActionLabel)
        }
      }
    }
  }
}

@Composable
fun CloudBoxErrorCard(
  message: String,
  onRetry: () -> Unit,
  actionLabel: String = "Retry",
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth().testTag("error_card"),
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Filled.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(24.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.weight(1f),
      )
      TextButton(
        onClick = onRetry,
        modifier = Modifier.testTag("error_retry_button"),
      ) {
        Text(actionLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
fun UploadProgressOverlayCard(
  tasks: List<UploadTaskState>,
  collapsed: Boolean,
  onToggleCollapse: () -> Unit,
  onCancelTask: (String) -> Unit,
  onRetryTask: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (tasks.isEmpty()) return
  val extra = LocalCloudBoxColors.current
  val activeCount = tasks.count {
    it.status == UploadStatus.UPLOADING || it.status == UploadStatus.QUEUED || it.status == UploadStatus.WAITING_WIFI
  }

  if (collapsed) {
    Surface(
      onClick = onToggleCollapse,
      modifier = modifier.testTag("upload_collapsed_pill"),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
      shadowElevation = 6.dp,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.5.dp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (activeCount > 0) "Uploading ($activeCount)" else "Uploads (${tasks.size})",
          style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Filled.ExpandLess, contentDescription = "Expand upload progress", modifier = Modifier.size(18.dp))
      }
    }
    return
  }

  Card(
    modifier = modifier.fillMaxWidth().testTag("upload_progress_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, extra.border),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            if (activeCount > 0) {
              "Uploading $activeCount ${if (activeCount == 1) "file" else "files"}"
            } else {
              "Upload status"
            },
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onToggleCollapse, modifier = Modifier.testTag("upload_hide_button")) {
          Text("Hide")
          Spacer(modifier = Modifier.width(2.dp))
          Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      tasks.take(4).forEach { task ->
        val animatedProgress by animateFloatAsState(task.progress, label = "uploadProg")
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = FormatUtils.middleEllipsis(task.fileName, 22),
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            when (task.status) {
              UploadStatus.COMPLETED -> {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = "Uploaded",
                  tint = extra.success,
                  modifier = Modifier.size(18.dp),
                )
              }
              UploadStatus.WAITING_WIFI -> {
                Icon(
                  imageVector = Icons.Filled.Schedule,
                  contentDescription = "Waiting for Wi-Fi",
                  tint = extra.warning,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Waiting for Wi-Fi…", style = MaterialTheme.typography.bodySmall, color = extra.warning)
                IconButton(onClick = { onCancelTask(task.id) }, modifier = Modifier.size(32.dp)) {
                  Icon(Icons.Filled.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                }
              }
              UploadStatus.FAILED -> {
                Text(
                  text = task.errorMessage ?: "Failed",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = { onRetryTask(task.id) }) {
                  Text("Retry")
                }
              }
              else -> {
                val pct = (animatedProgress * 100).toInt()
                Text(
                  text = if (task.status == UploadStatus.QUEUED) "Queued" else "$pct%",
                  style = MaterialTheme.typography.bodySmall,
                  color = extra.textSecondary,
                )
                IconButton(onClick = { onCancelTask(task.id) }, modifier = Modifier.size(32.dp)) {
                  Icon(Icons.Filled.Close, contentDescription = "Cancel upload", modifier = Modifier.size(16.dp))
                }
              }
            }
          }
          if (task.status == UploadStatus.UPLOADING || task.status == UploadStatus.COMPLETED) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
              progress = { if (task.status == UploadStatus.COMPLETED) 1f else animatedProgress },
              modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
              color = if (task.status == UploadStatus.COMPLETED) extra.success else MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}
