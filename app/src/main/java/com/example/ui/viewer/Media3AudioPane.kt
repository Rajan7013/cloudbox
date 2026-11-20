package com.example.ui.viewer

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.CloudFile
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Media3AudioPane(
  file: CloudFile,
  audioFile: File,
  siblings: List<CloudFile>,
  autoplay: Boolean,
  onBack: () -> Unit,
  onShare: () -> Unit,
  onDownload: () -> Unit,
  onOpenMoreActions: () -> Unit,
  onSettings: () -> Unit,
  onSelectSibling: (CloudFile) -> Unit,
  onShowToast: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val extra = LocalCloudBoxColors.current

  val player = remember(audioFile) {
    ExoPlayer.Builder(context).build().apply {
      setMediaItem(MediaItem.fromUri(Uri.fromFile(audioFile)))
      prepare()
      playWhenReady = autoplay
    }
  }

  DisposableEffect(player) {
    onDispose { player.release() }
  }

  var isPlaying by remember { mutableStateOf(autoplay) }
  var positionMs by remember { mutableLongStateOf(0L) }
  var durationMs by remember { mutableLongStateOf(0L) }
  var speed by remember { mutableFloatStateOf(1.0f) }
  var isShuffle by remember { mutableStateOf(false) }
  var repeatModeChoice by remember { mutableStateOf("Off") } // "Off", "All", "One"
  var sleepTimerMinutes by remember { mutableIntStateOf(0) }
  var eqPreset by remember { mutableStateOf("Flat") } // "Flat", "Bass", "Vocal", "Treble"
  var volumeLevel by remember { mutableFloatStateOf(0.85f) }

  // Up next queue sheet
  var showQueueSheet by remember { mutableStateOf(false) }

  // Vinyl disc continuous rotation
  val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
  val discAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 10000, easing = LinearEasing),
      repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
    ),
    label = "discRotation",
  )

  LaunchedEffect(player) {
    while (true) {
      isPlaying = player.isPlaying
      positionMs = player.currentPosition.coerceAtLeast(0L)
      durationMs = player.duration.coerceAtLeast(0L)
      delay(250)
    }
  }

  LaunchedEffect(sleepTimerMinutes) {
    if (sleepTimerMinutes > 0) {
      delay(sleepTimerMinutes * 60_000L)
      player.pause()
      sleepTimerMinutes = 0
      onShowToast("Sleep timer ended · Audio paused")
    }
  }

  fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
  }

  val audioSiblings = remember(siblings) {
    siblings.filter { it.category == com.example.model.FileCategory.AUDIO }
  }
  val currentAudioIdx = audioSiblings.indexOfFirst { it.id == file.id }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0F766E), Color(0xFF312E81), Color(0xFF0E1020))
        )
      ),
  ) {
    // Top Bar (Common shell frame)
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = file.name,
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "Audio · ${FormatUtils.formatBytes(file.sizeBytes)} · ${file.userRole.label}",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.75f),
        )
      }
      IconButton(onClick = { showQueueSheet = true }) {
        Icon(Icons.Filled.QueueMusic, contentDescription = "Queue", tint = Color.White)
      }
      if (file.canShare) {
        IconButton(onClick = onShare) { Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White) }
      }
      IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White) }
      IconButton(onClick = onOpenMoreActions) { Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White) }
    }

    // Main Player Body
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      // Spinning Vinyl Album Art Disc
      Box(
        modifier = Modifier
          .size(200.dp)
          .clip(CircleShape)
          .rotate(if (isPlaying) discAngle else 0f)
          .background(
            Brush.sweepGradient(
              listOf(Color(0xFF14B8A6), Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF14B8A6))
            )
          ),
        contentAlignment = Alignment.Center,
      ) {
        // Vinyl center grooved ring
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFF111827)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Track Title & Label
      Text(
        text = file.name.substringBeforeLast('.'),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "CloudBox Media Player · ${file.ownerName.ifBlank { "Original Audio" }}",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.75f),
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Dynamic Audio Waveform Representation (Spec Section 11)
      val progressFrac = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val barCount = 36
        for (i in 0 until barCount) {
          val frac = i.toFloat() / barCount.toFloat()
          val heightFraction = (abs(sin(i * 1.2f)) * 0.75f + 0.25f).coerceIn(0.15f, 1f)
          val isPassed = frac <= progressFrac
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(heightFraction)
              .padding(horizontal = 1.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(if (isPassed) Color.White else Color.White.copy(alpha = 0.3f)),
          )
        }
      }

      // Scrub Slider
      if (durationMs > 0) {
        Slider(
          value = positionMs.toFloat(),
          onValueChange = { player.seekTo(it.toLong()) },
          valueRange = 0f..durationMs.toFloat(),
          colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF14B8A6)),
          modifier = Modifier.fillMaxWidth(),
        )
      }

      // Time & Audio Metadata Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(formatTime(positionMs), style = MaterialTheme.typography.bodySmall, color = Color.White)
        Text(
          text = "${speed}x · EQ $eqPreset${if (sleepTimerMinutes > 0) " · 😴 ${sleepTimerMinutes}m" else ""}",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.8f),
        )
        Text(formatTime(durationMs), style = MaterialTheme.typography.bodySmall, color = Color.White)
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Main Controls: Shuffle, Prev, ⟲15, Play/Pause, 15⟳, Next, Repeat
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = { isShuffle = !isShuffle }) {
          Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (isShuffle) Color(0xFF14B8A6) else Color.White.copy(alpha = 0.5f))
        }
        IconButton(onClick = {
          if (currentAudioIdx > 0) onSelectSibling(audioSiblings[currentAudioIdx - 1])
          else player.seekTo(0L)
        }) {
          Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        IconButton(onClick = { player.seekTo((player.currentPosition - 15000L).coerceAtLeast(0L)) }) {
          Icon(Icons.Filled.Replay, contentDescription = "Replay 15s", tint = Color.White, modifier = Modifier.size(28.dp))
        }

        // Center 66px Circular Play Button
        Surface(
          onClick = { if (player.isPlaying) player.pause() else player.play() },
          shape = CircleShape,
          color = Color.White,
          modifier = Modifier.size(66.dp),
          shadowElevation = 8.dp,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              tint = Color(0xFF312E81),
              modifier = Modifier.size(34.dp),
            )
          }
        }

        IconButton(onClick = { player.seekTo(player.currentPosition + 15000L) }) {
          Icon(Icons.Filled.Forward10, contentDescription = "Forward 15s", tint = Color.White, modifier = Modifier.size(28.dp))
        }
        IconButton(onClick = {
          if (currentAudioIdx in 0 until audioSiblings.lastIndex) onSelectSibling(audioSiblings[currentAudioIdx + 1])
        }) {
          Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        IconButton(onClick = {
          repeatModeChoice = when (repeatModeChoice) {
            "Off" -> "All"
            "All" -> "One"
            else -> "Off"
          }
          player.repeatMode = when (repeatModeChoice) {
            "One" -> Player.REPEAT_MODE_ONE
            "All" -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
          }
        }) {
          Icon(
            imageVector = if (repeatModeChoice == "One") Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = "Repeat",
            tint = if (repeatModeChoice != "Off") Color(0xFF14B8A6) else Color.White.copy(alpha = 0.5f),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Volume Slider
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Icon(Icons.Filled.VolumeMute, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Slider(
          value = volumeLevel,
          onValueChange = {
            volumeLevel = it
            player.volume = it
          },
          valueRange = 0f..1f,
          colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White),
          modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
      }
    }

    // Bottom Tool Dock (Spec: Speed, Sleep, EQ, Loop A-B, Download)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color.Black.copy(alpha = 0.3f))
        .padding(horizontal = 8.dp, vertical = 6.dp)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.Filled.Speed, "${speed}x") {
        speed = when (speed) {
          0.75f -> 1.0f
          1.0f -> 1.25f
          1.25f -> 1.5f
          1.5f -> 2.0f
          else -> 0.75f
        }
        player.setPlaybackSpeed(speed)
      }
      ViewerToolTile(Icons.Filled.Bedtime, if (sleepTimerMinutes == 0) "Sleep" else "${sleepTimerMinutes}m", active = sleepTimerMinutes > 0) {
        sleepTimerMinutes = when (sleepTimerMinutes) {
          0 -> 15
          15 -> 30
          30 -> 60
          else -> 0
        }
      }
      ViewerToolTile(Icons.Filled.GraphicEq, "EQ $eqPreset", active = eqPreset != "Flat") {
        eqPreset = when (eqPreset) {
          "Flat" -> "Bass"
          "Bass" -> "Vocal"
          "Vocal" -> "Treble"
          else -> "Flat"
        }
        onShowToast("Equalizer preset: $eqPreset")
      }
      ViewerToolTile(Icons.Filled.QueueMusic, "Queue") {
        showQueueSheet = true
      }
      if (file.canDownload) {
        ViewerToolTile(Icons.Filled.Download, "Download") {
          onDownload()
        }
      }
    }
  }

  // Up Next Queue Sheet
  if (showQueueSheet) {
    ModalBottomSheet(
      onDismissRequest = { showQueueSheet = false },
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 28.dp)) {
        Text("Up Next · Audio Playlist", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(14.dp))
        if (audioSiblings.isEmpty()) {
          Text("No other audio tracks in this folder", style = MaterialTheme.typography.bodyMedium, color = extra.textSecondary)
        } else {
          audioSiblings.forEachIndexed { idx, item ->
            val isCurrent = item.id == file.id
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                if (!isCurrent) {
                  onSelectSibling(item)
                  showQueueSheet = false
                }
              },
            ) {
              Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (isCurrent && isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                  contentDescription = null,
                  tint = if (isCurrent) MaterialTheme.colorScheme.primary else extra.textSecondary,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text(FormatUtils.formatBytes(item.sizeBytes), style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
                }
                if (isCurrent) {
                  SuggestionChip(onClick = {}, label = { Text("Playing", fontSize = 10.sp) })
                }
              }
            }
          }
        }
      }
    }
  }
}
