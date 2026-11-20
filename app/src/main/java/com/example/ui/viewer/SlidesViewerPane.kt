package com.example.ui.viewer

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CloudFile
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.delay

private val SLIDE_GRADIENTS = listOf(
  listOf(Color(0xFF4F46E5), Color(0xFF8B5CF6)),
  listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)),
  listOf(Color(0xFF10B981), Color(0xFF14B8A6)),
  listOf(Color(0xFFF97316), Color(0xFFEF4444)),
  listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlidesViewerPane(
  file: CloudFile,
  slidesFile: File,
  slides: List<String>,
  notes: List<String> = emptyList(),
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
  val slideCount = slides.size.coerceAtLeast(1)

  var currentSlide by remember { mutableIntStateOf(0) }
  var viewMode by remember { mutableStateOf("Slide") } // "Slide", "Grid", "Outline", "Presenter"
  var showNotes by remember { mutableStateOf(false) }
  var laserActive by remember { mutableStateOf(false) }
  var laserPosition by remember { mutableStateOf(Offset.Zero) }

  // Freehand drawing / markup on slides
  var penActive by remember { mutableStateOf(false) }
  val slideStrokes = remember { mutableStateMapOf<Int, MutableList<DrawnStroke>>() }
  var currentDrawingPoints = remember { mutableStateListOf<Offset>() }
  var penColor by remember { mutableStateOf(Color(0xFFEF4444)) }

  // Auto-play state
  var autoPlayIntervalSec by remember { mutableIntStateOf(0) } // 0 = off, 3, 5, 10
  var isAutoPlaying by remember { mutableStateOf(false) }

  // Search in slides
  var showSearch by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }

  // Timer in Presenter mode
  var elapsedPresenterSeconds by remember { mutableIntStateOf(0) }

  LaunchedEffect(isAutoPlaying, autoPlayIntervalSec) {
    if (isAutoPlaying && autoPlayIntervalSec > 0) {
      while (isAutoPlaying) {
        delay(autoPlayIntervalSec * 1000L)
        currentSlide = (currentSlide + 1) % slideCount
      }
    }
  }

  LaunchedEffect(viewMode) {
    if (viewMode == "Presenter") {
      elapsedPresenterSeconds = 0
      while (viewMode == "Presenter") {
        delay(1000L)
        elapsedPresenterSeconds++
      }
    }
  }

  // Filter slides by search query
  val searchHits = remember(slides, searchQuery) {
    val q = searchQuery.trim()
    if (q.isEmpty()) emptyList()
    else slides.mapIndexedNotNull { i, text -> if (text.contains(q, ignoreCase = true)) i else null }
  }

  val isPresenter = viewMode == "Presenter"

  fun saveCurrentSlideAsImage() {
    try {
      val w = 1280
      val h = 720
      val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      val paint = Paint().apply { isAntiAlias = true }

      // Draw background
      paint.color = android.graphics.Color.parseColor("#1E1B4B")
      canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

      // Draw slide title
      paint.color = android.graphics.Color.WHITE
      paint.textSize = 48f
      paint.isFakeBoldText = true
      canvas.drawText("Slide ${currentSlide + 1} · ${file.name}", 80f, 120f, paint)

      // Draw slide body text
      paint.textSize = 28f
      paint.isFakeBoldText = false
      val lines = slides.getOrNull(currentSlide).orEmpty().lines().take(12)
      lines.forEachIndexed { idx, line ->
        canvas.drawText(line.take(80), 80f, 200f + (idx * 40f), paint)
      }

      val fileName = "${file.name.substringBeforeLast('.')}_slide_${currentSlide + 1}.png"
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
          put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
          put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CloudBox")
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
          resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
          }
          onShowToast("Slide ${currentSlide + 1} saved to Gallery")
        }
      } else {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CloudBox").apply { mkdirs() }
        val target = File(dir, fileName)
        FileOutputStream(target).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        onShowToast("Saved to Pictures/CloudBox")
      }
    } catch (e: Exception) {
      onShowToast(e.localizedMessage ?: "Failed to save slide image")
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(if (isPresenter) Color.Black else MaterialTheme.colorScheme.background),
  ) {
    // Top Bar (hidden in Presenter mode)
    if (!isPresenter) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = file.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Slides · Slide ${currentSlide + 1} of $slideCount · ${file.userRole.label}",
            style = MaterialTheme.typography.bodySmall,
            color = extra.textSecondary,
          )
        }
        IconButton(onClick = { showSearch = !showSearch }) {
          Icon(Icons.Filled.Search, contentDescription = "Search", tint = if (showSearch) MaterialTheme.colorScheme.primary else LocalContentColor.current)
        }
        if (file.canShare) {
          IconButton(onClick = onShare) { Icon(Icons.Filled.Share, contentDescription = "Share") }
        }
        IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
        IconButton(onClick = onOpenMoreActions) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
      }
    } else {
      // Presenter Mode Top Banner with Timer
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          IconButton(onClick = { viewMode = "Slide" }) {
            Icon(Icons.Filled.Close, contentDescription = "Exit presenter", tint = Color.White)
          }
          Text(
            text = "Presenter Mode · Slide ${currentSlide + 1}/$slideCount",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
          )
        }
        val mins = elapsedPresenterSeconds / 60
        val secs = elapsedPresenterSeconds % 60
        Surface(
          color = Color(0xFF1E2138),
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = String.format(Locale.US, "%02d:%02d", mins, secs),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF6EE7B7),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
    }

    // Search Box if active
    AnimatedVisibility(visible = showSearch) {
      Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Find in slides…") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${searchHits.size} matches",
          style = MaterialTheme.typography.bodySmall,
          color = extra.textSecondary,
        )
        if (searchHits.isNotEmpty()) {
          IconButton(onClick = {
            val nextHit = searchHits.firstOrNull { it > currentSlide } ?: searchHits.first()
            currentSlide = nextHit
          }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next hit")
          }
        }
      }
    }

    // Content area based on ViewMode
    when (viewMode) {
      "Grid" -> {
        // Grid Overview (thumbnails of all slides)
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier.weight(1f).padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          itemsIndexed(slides) { idx, text ->
            val gradient = SLIDE_GRADIENTS[idx % SLIDE_GRADIENTS.size]
            Surface(
              modifier = Modifier
                .aspectRatio(16f / 9f)
                .clickable {
                  currentSlide = idx
                  viewMode = "Slide"
                },
              shape = RoundedCornerShape(12.dp),
              border = if (idx == currentSlide) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, extra.border),
              shadowElevation = 2.dp,
            ) {
              Box(
                modifier = Modifier.fillMaxSize().background(
                  androidx.compose.ui.graphics.Brush.linearGradient(gradient)
                ).padding(10.dp),
              ) {
                Text(
                  text = text.lines().firstOrNull() ?: "Slide ${idx + 1}",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${idx + 1}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.7f),
                  modifier = Modifier.align(Alignment.BottomEnd),
                )
              }
            }
          }
        }
      }

      "Outline" -> {
        // Outline view (all slide text listed sequentially)
        Column(
          modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          slides.forEachIndexed { idx, sText ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, if (idx == currentSlide) MaterialTheme.colorScheme.primary else extra.border),
              modifier = Modifier.fillMaxWidth().clickable {
                currentSlide = idx
                viewMode = "Slide"
              },
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text("Slide ${idx + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(sText, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
        }
      }

      else -> {
        // Single Slide View (16:9 card) or Presenter mode
        val slideText = slides.getOrNull(currentSlide) ?: "Slide ${currentSlide + 1}"
        val gradient = SLIDE_GRADIENTS[currentSlide % SLIDE_GRADIENTS.size]

        Column(
          modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // Slide 16:9 Card
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(16f / 9f)
              .clip(RoundedCornerShape(16.dp))
              .background(androidx.compose.ui.graphics.Brush.linearGradient(gradient))
              .pointerInput(laserActive, penActive) {
                if (laserActive) {
                  detectDragGestures(
                    onDragStart = { laserPosition = it },
                    onDrag = { change, _ ->
                      change.consume()
                      laserPosition = change.position
                    },
                    onDragEnd = { laserPosition = Offset.Zero },
                  )
                } else if (penActive) {
                  detectDragGestures(
                    onDragStart = {
                      currentDrawingPoints.clear()
                      currentDrawingPoints.add(it)
                    },
                    onDrag = { change, _ ->
                      change.consume()
                      currentDrawingPoints.add(change.position)
                    },
                    onDragEnd = {
                      if (currentDrawingPoints.isNotEmpty()) {
                        val list = slideStrokes.getOrPut(currentSlide) { mutableListOf() }
                        list.add(DrawnStroke(currentDrawingPoints.toList(), penColor, 4f))
                        currentDrawingPoints.clear()
                      }
                    },
                  )
                }
              },
          ) {
            // Slide Content text
            Column(
              modifier = Modifier.fillMaxSize().padding(22.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = slideText.lines().firstOrNull() ?: "",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = slideText.lines().drop(1).joinToString("\n").take(200),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
              )
            }

            // Slide number badge
            Text(
              text = "${currentSlide + 1} / $slideCount",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.75f),
              modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            )

            // Ink Canvas for drawings
            val currentSlideDrawings = slideStrokes[currentSlide].orEmpty()
            Canvas(modifier = Modifier.fillMaxSize()) {
              currentSlideDrawings.forEach { s ->
                if (s.points.size > 1) {
                  val p = Path().apply {
                    moveTo(s.points[0].x, s.points[0].y)
                    for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
                  }
                  drawPath(p, s.color, style = Stroke(width = s.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
              }
              if (currentDrawingPoints.size > 1) {
                val p = Path().apply {
                  moveTo(currentDrawingPoints[0].x, currentDrawingPoints[0].y)
                  for (i in 1 until currentDrawingPoints.size) lineTo(currentDrawingPoints[i].x, currentDrawingPoints[i].y)
                }
                drawPath(p, penColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
              }

              // Laser dot
              if (laserActive && laserPosition != Offset.Zero) {
                drawCircle(Color.Red.copy(alpha = 0.35f), radius = 18f, center = laserPosition)
                drawCircle(Color.Red, radius = 7f, center = laserPosition)
              }
            }
          }

          // Filmstrip of slide thumbnails
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            slides.forEachIndexed { i, s ->
              Surface(
                modifier = Modifier
                  .width(62.dp)
                  .height(38.dp)
                  .clickable { currentSlide = i },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(if (i == currentSlide) 2.dp else 1.dp, if (i == currentSlide) MaterialTheme.colorScheme.primary else extra.border),
                color = SLIDE_GRADIENTS[i % SLIDE_GRADIENTS.size].first().copy(alpha = 0.7f),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
              }
            }
          }

          // Speaker Notes Panel
          if (showNotes) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 110.dp).verticalScroll(rememberScrollState()),
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("Speaker Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                val note = notes.getOrNull(currentSlide) ?: "No notes for this slide. Introduce topics and discuss key findings with audience."
                Text(note, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
    }

    // Bottom Tool Dock
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 8.dp, vertical = 6.dp)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev") {
        if (currentSlide > 0) currentSlide--
      }
      ViewerToolTile(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") {
        if (currentSlide < slideCount - 1) currentSlide++
      }
      ViewerToolTile(Icons.Filled.GridView, "Grid", active = viewMode == "Grid") {
        viewMode = if (viewMode == "Grid") "Slide" else "Grid"
      }
      ViewerToolTile(Icons.Filled.Notes, "Notes", active = showNotes) {
        showNotes = !showNotes
      }
      ViewerToolTile(Icons.Filled.CenterFocusStrong, "Laser", active = laserActive) {
        laserActive = !laserActive
        if (laserActive) penActive = false
      }
      ViewerToolTile(Icons.Filled.Draw, "Pen", active = penActive) {
        penActive = !penActive
        if (penActive) laserActive = false
      }
      if (slideStrokes[currentSlide]?.isNotEmpty() == true) {
        ViewerToolTile(Icons.Filled.DeleteSweep, "Clear") {
          slideStrokes[currentSlide]?.clear()
          onShowToast("Drawings cleared for slide ${currentSlide + 1}")
        }
      }
      ViewerToolTile(
        icon = if (isAutoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        label = if (isAutoPlaying) "${autoPlayIntervalSec}s Play" else "AutoPlay",
        active = isAutoPlaying,
      ) {
        if (isAutoPlaying) {
          isAutoPlaying = false
        } else {
          autoPlayIntervalSec = if (autoPlayIntervalSec == 0) 3 else autoPlayIntervalSec
          isAutoPlaying = true
        }
      }
      ViewerToolTile(Icons.Filled.Slideshow, "Present", active = isPresenter) {
        viewMode = if (isPresenter) "Slide" else "Presenter"
      }
      if (file.canDownload) {
        ViewerToolTile(Icons.Filled.Image, "Save Slide") {
          saveCurrentSlideAsImage()
        }
      }
    }
  }
}
