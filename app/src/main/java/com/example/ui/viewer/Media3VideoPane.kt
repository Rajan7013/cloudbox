package com.example.ui.viewer

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.model.CloudFile
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun Media3VideoPane(
  file: CloudFile,
  videoFile: File,
  autoplay: Boolean,
  onBack: () -> Unit,
  onShare: () -> Unit,
  onDownload: () -> Unit,
  onOpenMoreActions: () -> Unit,
  onSettings: () -> Unit,
  onShowToast: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val extra = LocalCloudBoxColors.current

  val player = remember(videoFile) {
    ExoPlayer.Builder(context).build().apply {
      setMediaItem(MediaItem.fromUri(Uri.fromFile(videoFile)))
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
  var isLooping by remember { mutableStateOf(false) }
  var isFillAspect by remember { mutableStateOf(false) }
  var isLocked by remember { mutableStateOf(false) }
  var showSubtitles by remember { mutableStateOf(true) }
  var subtitleSize by remember { mutableStateOf("M") } // "S", "M", "L"

  // Controls auto-hide
  var showControls by remember { mutableStateOf(true) }
  var lastUserTouchTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

  // Double-tap skip indicator
  var showSkipFeedback by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(player) {
    while (true) {
      isPlaying = player.isPlaying
      positionMs = player.currentPosition.coerceAtLeast(0L)
      durationMs = player.duration.coerceAtLeast(0L)
      delay(250)
    }
  }

  LaunchedEffect(isPlaying, lastUserTouchTime, isLocked) {
    if (isPlaying && !isLocked) {
      delay(3500L)
      showControls = false
    }
  }

  LaunchedEffect(showSkipFeedback) {
    if (showSkipFeedback != null) {
      delay(800L)
      showSkipFeedback = null
    }
  }

  fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
  }

  fun captureVideoFrame() {
    try {
      val retriever = MediaMetadataRetriever()
      retriever.setDataSource(videoFile.absolutePath)
      val timeUs = positionMs * 1000L
      val frameBitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        ?: retriever.frameAtTime
      retriever.release()

      if (frameBitmap != null) {
        val fileName = "${file.name.substringBeforeLast('.')}_frame_${positionMs / 1000}s.jpg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val resolver = context.contentResolver
          val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CloudBox")
          }
          val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
          if (uri != null) {
            resolver.openOutputStream(uri)?.use { out ->
              frameBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            onShowToast("Frame captured and saved to Gallery")
          }
        } else {
          val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CloudBox").apply { mkdirs() }
          val target = File(dir, fileName)
          FileOutputStream(target).use { out -> frameBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
          onShowToast("Saved to Pictures/CloudBox")
        }
      } else {
        onShowToast("Couldn't extract frame")
      }
    } catch (e: Exception) {
      onShowToast("Failed to capture frame: ${e.localizedMessage}")
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .pointerInput(isLocked) {
        detectTapGestures(
          onTap = {
            if (!isLocked) {
              showControls = !showControls
              lastUserTouchTime = System.currentTimeMillis()
            }
          },
          onDoubleTap = { offset ->
            if (!isLocked) {
              val screenWidth = size.width
              if (offset.x < screenWidth * 0.4f) {
                // Left side: -10s
                player.seekTo((player.currentPosition - 10000L).coerceAtLeast(0L))
                showSkipFeedback = "⏪ -10s"
              } else if (offset.x > screenWidth * 0.6f) {
                // Right side: +10s
                player.seekTo(player.currentPosition + 10000L)
                showSkipFeedback = "⏩ +10s"
              } else {
                // Center: play/pause
                if (player.isPlaying) player.pause() else player.play()
              }
              lastUserTouchTime = System.currentTimeMillis()
            }
          },
        )
      },
  ) {
    // Android ExoPlayer Video Surface
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          this.player = player
          useController = false
          resizeMode = if (isFillAspect) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
      },
      update = { view ->
        view.resizeMode = if (isFillAspect) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Double-tap feedback bubble
    if (showSkipFeedback != null) {
      Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.align(Alignment.Center),
      ) {
        Text(
          text = showSkipFeedback.orEmpty(),
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
      }
    }

    // Lock Overlay Button
    if (isLocked) {
      Button(
        onClick = {
          isLocked = false
          showControls = true
          lastUserTouchTime = System.currentTimeMillis()
        },
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
      ) {
        Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Tap to unlock")
      }
    }

    // Video Controls Overlay (Fade in/out)
    AnimatedVisibility(
      visible = showControls && !isLocked,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.5f))
          .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top Bar
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
              text = "Video · ${FormatUtils.formatBytes(file.sizeBytes)} · ${file.userRole.label}",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.75f),
            )
          }
          if (file.canShare) {
            IconButton(onClick = onShare) { Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White) }
          }
          IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White) }
          IconButton(onClick = onOpenMoreActions) { Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White) }
        }

        // Center Big Play / Pause Button (70px circular)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Surface(
            onClick = {
              if (player.isPlaying) player.pause() else player.play()
              lastUserTouchTime = System.currentTimeMillis()
            },
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(70.dp),
            shadowElevation = 8.dp,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color(0xFF4F46E5),
                modifier = Modifier.size(36.dp),
              )
            }
          }
        }

        // Bottom Controls Container
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
          // Progress Slider
          if (durationMs > 0) {
            Slider(
              value = positionMs.toFloat(),
              onValueChange = {
                player.seekTo(it.toLong())
                lastUserTouchTime = System.currentTimeMillis()
              },
              valueRange = 0f..durationMs.toFloat(),
              colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF6366F1)),
              modifier = Modifier.fillMaxWidth(),
            )
          }

          // Time Indicator
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White,
            )
            Text(
              text = "${speed}x · ${if (isFillAspect) "Fill" else "Fit"}",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.8f),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Tool Dock (Spec: 5 visible, horizontal scroll, 64px width tiles)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ViewerToolTile(Icons.Filled.Replay10, "−10s") {
              player.seekTo((player.currentPosition - 10000L).coerceAtLeast(0L))
              lastUserTouchTime = System.currentTimeMillis()
            }
            ViewerToolTile(Icons.Filled.Forward10, "+10s") {
              player.seekTo(player.currentPosition + 10000L)
              lastUserTouchTime = System.currentTimeMillis()
            }
            ViewerToolTile(Icons.Filled.Speed, "${speed}x") {
              speed = when (speed) {
                0.5f -> 1.0f
                1.0f -> 1.25f
                1.25f -> 1.5f
                1.5f -> 2.0f
                else -> 0.5f
              }
              player.setPlaybackSpeed(speed)
              lastUserTouchTime = System.currentTimeMillis()
            }
            ViewerToolTile(Icons.Filled.ClosedCaption, if (showSubtitles) "CC On" else "CC Off", active = showSubtitles) {
              showSubtitles = !showSubtitles
              lastUserTouchTime = System.currentTimeMillis()
            }
            ViewerToolTile(Icons.Filled.Repeat, "Loop", active = isLooping) {
              isLooping = !isLooping
              player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
              lastUserTouchTime = System.currentTimeMillis()
            }
            ViewerToolTile(Icons.Filled.FitScreen, if (isFillAspect) "Fit" else "Fill") {
              isFillAspect = !isFillAspect
              lastUserTouchTime = System.currentTimeMillis()
            }
            ViewerToolTile(Icons.Filled.CameraAlt, "Frame") {
              captureVideoFrame()
              lastUserTouchTime = System.currentTimeMillis()
            }
            ViewerToolTile(Icons.Filled.Lock, "Lock") {
              isLocked = true
              showControls = false
            }
          }
        }
      }
    }
  }
}
