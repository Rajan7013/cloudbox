package com.example.ui.viewer

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CloudFile
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val MATRIX_BW = ColorMatrix().apply { setToSaturation(0f) }
private val MATRIX_WARM = ColorMatrix(
  floatArrayOf(
    1.15f, 0f, 0f, 0f, 15f,
    0f, 1.05f, 0f, 0f, 5f,
    0f, 0f, 0.88f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
  )
)
private val MATRIX_COOL = ColorMatrix(
  floatArrayOf(
    0.9f, 0f, 0f, 0f, 0f,
    0f, 1.0f, 0f, 0f, 5f,
    0f, 0f, 1.2f, 0f, 20f,
    0f, 0f, 0f, 1f, 0f,
  )
)
private val MATRIX_VIVID = ColorMatrix().apply { setToSaturation(1.6f) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
  file: CloudFile,
  imageFile: File,
  siblings: List<CloudFile>,
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

  val imageSiblings = remember(siblings) {
    siblings.filter { it.category == com.example.model.FileCategory.IMAGE }
  }
  val currentIndex = imageSiblings.indexOfFirst { it.id == file.id }
  val canSwipePrev = currentIndex > 0
  val canSwipeNext = currentIndex in 0 until imageSiblings.lastIndex

  var scale by remember(imageFile) { mutableFloatStateOf(1f) }
  var offset by remember(imageFile) { mutableStateOf(Offset.Zero) }
  var fillAspect by remember(imageFile) { mutableStateOf(false) }
  var rotationDeg by remember(imageFile) { mutableFloatStateOf(0f) }
  var showInfo by remember(imageFile) { mutableStateOf(false) }

  // Filters
  var activeFilter by remember(imageFile) { mutableStateOf("None") } // "None", "B&W", "Warm", "Cool", "Vivid"
  val colorFilter = when (activeFilter) {
    "B&W" -> ColorFilter.colorMatrix(MATRIX_BW)
    "Warm" -> ColorFilter.colorMatrix(MATRIX_WARM)
    "Cool" -> ColorFilter.colorMatrix(MATRIX_COOL)
    "Vivid" -> ColorFilter.colorMatrix(MATRIX_VIVID)
    else -> null
  }

  // Slideshow
  var isSlideshow by remember { mutableStateOf(false) }
  var slideshowIntervalSec by remember { mutableIntStateOf(4) }

  // Pen Markup
  var isPenActive by remember { mutableStateOf(false) }
  val drawnStrokes = remember(imageFile) { mutableStateListOf<DrawnStroke>() }
  val currentPoints = remember { mutableStateListOf<Offset>() }
  var penColor by remember { mutableStateOf(Color(0xFFEF4444)) }

  // EXIF Dimensions
  val dimensionsText by produceState(initialValue = "", imageFile, showInfo) {
    if (showInfo) {
      value = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.path, options)
        if (options.outWidth > 0) "${options.outWidth} × ${options.outHeight} · ${options.outMimeType ?: "image"}"
        else "Dimensions unavailable"
      }
    }
  }

  // Slideshow ticker
  LaunchedEffect(isSlideshow, slideshowIntervalSec, currentIndex, canSwipeNext) {
    if (isSlideshow) {
      while (isSlideshow) {
        delay(slideshowIntervalSec * 1000L)
        if (canSwipeNext) {
          onSelectSibling(imageSiblings[currentIndex + 1])
        } else if (imageSiblings.isNotEmpty()) {
          onSelectSibling(imageSiblings.first())
        }
      }
    }
  }

  fun saveImageToGallery() {
    try {
      val raw = BitmapFactory.decodeFile(imageFile.absolutePath)
      val fileName = "CloudBox_${file.name.substringBeforeLast('.')}.jpg"
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
          put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
          put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CloudBox")
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
          resolver.openOutputStream(uri)?.use { out -> raw.compress(Bitmap.CompressFormat.JPEG, 95, out) }
          onShowToast("Image saved to Gallery")
        }
      } else {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CloudBox").apply { mkdirs() }
        val target = File(dir, fileName)
        FileOutputStream(target).use { out -> raw.compress(Bitmap.CompressFormat.JPEG, 95, out) }
        onShowToast("Saved to Pictures/CloudBox")
      }
    } catch (e: Exception) {
      onShowToast("Failed to save image: ${e.localizedMessage}")
    }
  }

  Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
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
          text = "Photo · ${currentIndex + 1} of ${imageSiblings.size.coerceAtLeast(1)} · ${FormatUtils.formatBytes(file.sizeBytes)} · ${file.userRole.label}",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.75f),
        )
      }
      IconButton(onClick = { showInfo = !showInfo }) {
        Icon(Icons.Filled.Info, contentDescription = "Info", tint = if (showInfo) Color(0xFF60A5FA) else Color.White)
      }
      if (file.canShare) {
        IconButton(onClick = onShare) { Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White) }
      }
      IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White) }
      IconButton(onClick = onOpenMoreActions) { Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White) }
    }

    // Photo Area with Zoom, Pan & Gestures
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .pointerInput(imageFile, isPenActive) {
          if (isPenActive) {
            detectDragGestures(
              onDragStart = { currentPoints.clear(); currentPoints.add(it) },
              onDrag = { change, _ -> change.consume(); currentPoints.add(change.position) },
              onDragEnd = {
                if (currentPoints.isNotEmpty()) {
                  drawnStrokes.add(DrawnStroke(currentPoints.toList(), penColor, 4f))
                  currentPoints.clear()
                }
              },
            )
          } else {
            detectTapGestures(
              onDoubleTap = {
                if (scale > 1.2f) {
                  scale = 1f
                  offset = Offset.Zero
                } else {
                  scale = 2.5f
                }
              },
            )
          }
        }
        .pointerInput(imageFile, isPenActive) {
          if (!isPenActive) {
            detectTransformGestures { _, pan, zoom, _ ->
              val nextScale = (scale * zoom).coerceIn(0.5f, 6f)
              scale = nextScale
              offset = if (nextScale > 1.05f) Offset(offset.x + pan.x, offset.y + pan.y) else Offset.Zero
            }
          }
        },
      contentAlignment = Alignment.Center,
    ) {
      AsyncImage(
        model = imageFile,
        contentDescription = file.name,
        contentScale = if (fillAspect) ContentScale.Crop else ContentScale.Fit,
        colorFilter = colorFilter,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
            rotationZ = rotationDeg
          },
      )

      // Pen Markup overlay
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawnStrokes.forEach { s ->
          if (s.points.size > 1) {
            val p = Path().apply {
              moveTo(s.points[0].x, s.points[0].y)
              for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
            }
            drawPath(p, s.color, style = Stroke(width = s.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
          }
        }
        if (currentPoints.size > 1) {
          val p = Path().apply {
            moveTo(currentPoints[0].x, currentPoints[0].y)
            for (i in 1 until currentPoints.size) lineTo(currentPoints[i].x, currentPoints[i].y)
          }
          drawPath(p, penColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
      }

      // Zoom badge
      Surface(
        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.65f),
      ) {
        Text(
          text = if (showInfo) dimensionsText else "${(scale * 100).toInt()}%",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }

      // Previous & Next navigation buttons
      if (canSwipePrev) {
        IconButton(
          onClick = { onSelectSibling(imageSiblings[currentIndex - 1]) },
          modifier = Modifier.align(Alignment.CenterStart).padding(8.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev", tint = Color.White)
        }
      }
      if (canSwipeNext) {
        IconButton(
          onClick = { onSelectSibling(imageSiblings[currentIndex + 1]) },
          modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", tint = Color.White)
        }
      }
    }

    // Photo Filters Row
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      listOf("None", "B&W", "Warm", "Cool", "Vivid").forEach { flt ->
        FilterChip(
          selected = activeFilter == flt,
          onClick = { activeFilter = flt },
          label = { Text(flt) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF4F46E5),
            selectedLabelColor = Color.White,
          ),
        )
      }
    }

    // Bottom Tool Dock
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color.Black.copy(alpha = 0.4f))
        .padding(horizontal = 8.dp, vertical = 6.dp)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.Filled.FitScreen, if (fillAspect) "Fit" else "Fill") { fillAspect = !fillAspect }
      ViewerToolTile(Icons.Filled.ZoomIn, "Zoom +") { scale = (scale + 0.4f).coerceAtMost(5f) }
      ViewerToolTile(Icons.Filled.ZoomOut, "Zoom −") {
        scale = (scale - 0.4f).coerceAtLeast(0.5f)
        if (scale <= 1.05f) offset = Offset.Zero
      }
      ViewerToolTile(Icons.Filled.RotateRight, "Rotate") { rotationDeg = (rotationDeg + 90f) % 360f }
      ViewerToolTile(
        icon = if (isSlideshow) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        label = if (isSlideshow) "${slideshowIntervalSec}s Show" else "SlideShow",
        active = isSlideshow,
      ) {
        isSlideshow = !isSlideshow
      }
      ViewerToolTile(Icons.Filled.Draw, "Markup", active = isPenActive) { isPenActive = !isPenActive }
      if (drawnStrokes.isNotEmpty()) {
        ViewerToolTile(Icons.Filled.DeleteSweep, "Clear") {
          drawnStrokes.clear()
          onShowToast("Markup cleared")
        }
      }
      if (file.canDownload) {
        ViewerToolTile(Icons.Filled.Download, "Save") {
          saveImageToGallery()
        }
      }
    }
  }
}
