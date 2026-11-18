package com.example.ui.viewer

import com.example.model.ReaderTheme
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CloudFile
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils
import java.io.File
import kotlin.math.hypot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PdfScrollMode {
  VERTICAL,   // Continuous vertical scroll up/down
  HORIZONTAL  // Swipe left/right page by page like a book
}

enum class AnnotationTool {
  VIEW,        // Normal gestures, zoom & swipe
  PEN,         // Draw ink
  HIGHLIGHTER, // Translucent highlight
  ERASER       // Erase strokes
}

data class DrawnStroke(
  val points: List<Offset>,
  val color: Color,
  val strokeWidth: Float,
  val isHighlighter: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedPdfViewerPane(
  fileMeta: CloudFile,
  pdfFile: File,
  bookmarkedPages: List<Int>,
  onToggleBookmark: (Int) -> Unit,
  onBack: () -> Unit,
  onOpenMoreActions: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val extra = LocalCloudBoxColors.current
  val scope = rememberCoroutineScope()

  var pageCount by remember(pdfFile) { mutableIntStateOf(0) }
  var currentPage by remember(pdfFile) { mutableIntStateOf(0) }
  var scrollMode by remember { mutableStateOf(PdfScrollMode.HORIZONTAL) }
  var readerTheme by remember { mutableStateOf(ReaderTheme.LIGHT) }

  // Zoom & Pan state
  var zoomScale by remember { mutableFloatStateOf(1f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  // Annotation states (stored per page)
  var activeTool by remember { mutableStateOf(AnnotationTool.VIEW) }
  var penColor by remember { mutableStateOf(Color(0xFF2563EB)) }
  var penStrokeWidth by remember { mutableFloatStateOf(8f) }
  var highlighterColor by remember { mutableStateOf(Color(0xFFFDE047)) }
  val pageStrokes = remember { mutableStateMapOf<Int, MutableList<DrawnStroke>>() }
  var activeDrawingPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

  // In-Document Search state
  var showSearch by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var extractedPageTexts by remember(pdfFile) { mutableStateOf<Map<Int, String>>(emptyMap()) }
  var matchingPages by remember { mutableStateOf<List<Int>>(emptyList()) }
  var currentMatchIndex by remember { mutableIntStateOf(0) }

  // Voice Reader (TTS) state
  var showVoiceReader by remember { mutableStateOf(false) }
  var controllerRef: PdfVoiceReaderController? = null
  val voiceReader = remember(context) {
    PdfVoiceReaderController(context) { finishedPage ->
      if (finishedPage < pageCount - 1) {
        val nextPage = finishedPage + 1
        currentPage = nextPage
        val nextText = extractedPageTexts[nextPage]
        if (!nextText.isNullOrBlank()) {
          controllerRef?.readPage(nextPage, nextText)
        }
      }
    }.also { controllerRef = it }
  }

  DisposableEffect(voiceReader) {
    onDispose { voiceReader.shutdown() }
  }

  // Determine initial page count
  LaunchedEffect(pdfFile) {
    withContext(Dispatchers.IO) {
      try {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
          PdfRenderer(pfd).use { renderer ->
            pageCount = renderer.pageCount
          }
        }
      } catch (_: Exception) {}
    }
  }

  // Background text extraction for search and TTS
  LaunchedEffect(pdfFile, pageCount) {
    if (pageCount > 0) {
      withContext(Dispatchers.IO) {
        val texts = PdfTextExtractor.extractTextByPage(pdfFile, pageCount)
        withContext(Dispatchers.Main) {
          extractedPageTexts = texts
        }
      }
    }
  }

  // Update matching pages when search query changes
  LaunchedEffect(searchQuery, extractedPageTexts) {
    val q = searchQuery.trim()
    if (q.isBlank() || extractedPageTexts.isEmpty()) {
      matchingPages = emptyList()
      currentMatchIndex = 0
    } else {
      val matches = mutableListOf<Int>()
      for (p in 0 until pageCount) {
        val text = extractedPageTexts[p].orEmpty()
        if (text.contains(q, ignoreCase = true)) {
          matches.add(p)
        }
      }
      matchingPages = matches
      currentMatchIndex = 0
      if (matches.isNotEmpty() && !matches.contains(currentPage)) {
        currentPage = matches.first()
      }
    }
  }

  val paperColor =
    when (readerTheme) {
      ReaderTheme.LIGHT -> Color.White
      ReaderTheme.SEPIA -> extra.sepiaPaperBg
      ReaderTheme.DARK -> Color(0xFF131522)
    }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top Bar with full filename, reading modes, search, voice, and tools
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      border = BorderStroke(1.dp, extra.border.copy(alpha = 0.5f))
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }

          Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(
              text = fileMeta.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "PDF · ${if (pageCount > 0) "Page ${currentPage + 1} of $pageCount" else "Loading..."}",
              style = MaterialTheme.typography.bodySmall,
              color = extra.textSecondary,
            )
          }

          // Scroll mode toggle (Vertical continuous vs Horizontal book swipe)
          IconButton(
            onClick = {
              scrollMode = if (scrollMode == PdfScrollMode.HORIZONTAL) PdfScrollMode.VERTICAL else PdfScrollMode.HORIZONTAL
              zoomScale = 1f
              panOffset = Offset.Zero
            },
            modifier = Modifier.size(40.dp)
          ) {
            Icon(
              imageVector = if (scrollMode == PdfScrollMode.HORIZONTAL) Icons.Filled.SwapVert else Icons.Filled.SwapHoriz,
              contentDescription = if (scrollMode == PdfScrollMode.HORIZONTAL) "Switch to Vertical scroll" else "Switch to Horizontal swipe",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          // Search button
          IconButton(
            onClick = { showSearch = !showSearch },
            modifier = Modifier.size(40.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.Search,
              contentDescription = "Search text",
              tint = if (showSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Indian Voice Reader button
          IconButton(
            onClick = {
              showVoiceReader = !showVoiceReader
              if (!showVoiceReader && voiceReader.isSpeaking) {
                voiceReader.stop()
              }
            },
            modifier = Modifier.size(40.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.RecordVoiceOver,
              contentDescription = "Read aloud",
              tint = if (showVoiceReader || voiceReader.isSpeaking) extra.success else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Annotation / Draw tool button
          IconButton(
            onClick = {
              activeTool = if (activeTool == AnnotationTool.VIEW) AnnotationTool.PEN else AnnotationTool.VIEW
            },
            modifier = Modifier.size(40.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = "Annotate & Draw",
              tint = if (activeTool != AnnotationTool.VIEW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // More actions menu
          IconButton(onClick = onOpenMoreActions, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
          }
        }

        // Search Bar (Animates down)
        AnimatedVisibility(
          visible = showSearch,
          enter = slideInVertically() + fadeIn(),
          exit = slideOutVertically() + fadeOut()
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search in PDF…", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(10.dp),
              )
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(36.dp)) {
                  Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }
              }
              if (matchingPages.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${currentMatchIndex + 1}/${matchingPages.size}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                  enabled = currentMatchIndex > 0,
                  onClick = {
                    currentMatchIndex--
                    currentPage = matchingPages[currentMatchIndex]
                  },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous match")
                }
                IconButton(
                  enabled = currentMatchIndex < matchingPages.lastIndex,
                  onClick = {
                    currentMatchIndex++
                    currentPage = matchingPages[currentMatchIndex]
                  },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next match")
                }
              } else if (searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text("No match", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }

        // Voice Reader Control Bar (Indian Voice TTS)
        AnimatedVisibility(
          visible = showVoiceReader,
          enter = slideInVertically() + fadeIn(),
          exit = slideOutVertically() + fadeOut()
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              IconButton(
                onClick = {
                  val text = extractedPageTexts[currentPage] ?: "This page contains no readable text."
                  voiceReader.togglePlayPause(currentPage, text)
                },
                modifier = Modifier.size(42.dp)
              ) {
                Icon(
                  imageVector = if (voiceReader.isSpeaking) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                  contentDescription = if (voiceReader.isSpeaking) "Pause" else "Play",
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(26.dp)
                )
              }

              if (voiceReader.isSpeaking) {
                IconButton(
                  onClick = { voiceReader.stop() },
                  modifier = Modifier.size(42.dp)
                ) {
                  Icon(
                    Icons.Filled.Stop,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (voiceReader.isSpeaking) "Reading Page ${currentPage + 1} (Indian Voice)…" else "Indian Voice Reader",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Native Indian Accent · ${if (voiceReader.voiceLanguage == "hi-IN") "Hindi" else "English (India)"}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              }

              // Language toggle chip (English IN <-> Hindi IN)
              FilterChip(
                selected = voiceReader.voiceLanguage == "hi-IN",
                onClick = {
                  val nextLang = if (voiceReader.voiceLanguage == "hi-IN") "en-IN" else "hi-IN"
                  voiceReader.setLanguage(nextLang)
                },
                label = { Text(if (voiceReader.voiceLanguage == "hi-IN") "Hindi" else "English") },
                colors = FilterChipDefaults.filterChipColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
              )
            }
          }
        }

        // Annotation & Drawing Toolbar (Pen, Highlighter, Eraser, Colors, Thickness)
        AnimatedVisibility(
          visible = activeTool != AnnotationTool.VIEW,
          enter = slideInVertically() + fadeIn(),
          exit = slideOutVertically() + fadeOut()
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(14.dp)
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
              // Tool selectors (Pan/View, Pen, Highlighter, Eraser)
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                FilterChip(
                  selected = activeTool == AnnotationTool.VIEW,
                  onClick = { activeTool = AnnotationTool.VIEW },
                  leadingIcon = { Icon(Icons.Filled.PanTool, contentDescription = null, modifier = Modifier.size(16.dp)) },
                  label = { Text("View & Zoom") }
                )
                FilterChip(
                  selected = activeTool == AnnotationTool.PEN,
                  onClick = { activeTool = AnnotationTool.PEN },
                  leadingIcon = { Icon(Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) },
                  label = { Text("Pen") }
                )
                FilterChip(
                  selected = activeTool == AnnotationTool.HIGHLIGHTER,
                  onClick = { activeTool = AnnotationTool.HIGHLIGHTER },
                  leadingIcon = { Icon(Icons.Filled.Highlight, contentDescription = null, modifier = Modifier.size(16.dp)) },
                  label = { Text("Highlighter") }
                )
                FilterChip(
                  selected = activeTool == AnnotationTool.ERASER,
                  onClick = { activeTool = AnnotationTool.ERASER },
                  leadingIcon = { Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(16.dp)) },
                  label = { Text("Eraser") }
                )

                // Undo
                IconButton(
                  onClick = {
                    val list = pageStrokes[currentPage]
                    if (!list.isNullOrEmpty()) {
                      list.removeAt(list.lastIndex)
                    }
                  },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Filled.Undo, contentDescription = "Undo")
                }

                // Clear page annotations
                TextButton(
                  onClick = { pageStrokes[currentPage]?.clear() }
                ) {
                  Text("Clear", color = MaterialTheme.colorScheme.error)
                }
              }

              // Color & Stroke width selectors when Pen or Highlighter is active
              if (activeTool == AnnotationTool.PEN || activeTool == AnnotationTool.HIGHLIGHTER) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text("Color:", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
                  val colors = if (activeTool == AnnotationTool.PEN) {
                    listOf(
                      Color(0xFF1E293B), // Dark slate
                      Color(0xFF2563EB), // Blue
                      Color(0xFFDC2626), // Red
                      Color(0xFF16A34A), // Green
                      Color(0xFFF59E0B), // Yellow/Amber
                      Color(0xFFFFFFFF)  // White
                    )
                  } else {
                    listOf(
                      Color(0xFFFDE047), // Neon Yellow
                      Color(0xFF86EFAC), // Bright Green
                      Color(0xFF67E8F9), // Soft Cyan
                      Color(0xFFF472B6)  // Pink
                    )
                  }

                  colors.forEach { col ->
                    val selected = if (activeTool == AnnotationTool.PEN) penColor == col else highlighterColor == col
                    Box(
                      modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(col)
                        .border(
                          width = if (selected) 2.5.dp else 1.dp,
                          color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                          shape = CircleShape
                        )
                        .clickable {
                          if (activeTool == AnnotationTool.PEN) penColor = col else highlighterColor = col
                        },
                      contentAlignment = Alignment.Center
                    ) {
                      if (selected) {
                        Icon(
                          Icons.Filled.Check,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                          tint = if (col == Color.White || col == Color(0xFFFDE047)) Color.Black else Color.White
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))
                  Text("Width:", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
                  val widths = if (activeTool == AnnotationTool.PEN) {
                    listOf(4f to "Fine", 8f to "Medium", 16f to "Bold", 26f to "Thick")
                  } else {
                    listOf(18f to "Thin", 28f to "Standard", 42f to "Wide")
                  }

                  widths.forEach { (w, label) ->
                    val isSelected = penStrokeWidth == w
                    FilterChip(
                      selected = isSelected,
                      onClick = { penStrokeWidth = w },
                      label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // PDF Pages Viewport (Supports Vertical & Horizontal scroll with 2-finger zoom)
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(paperColor)
    ) {
      if (pageCount <= 0) {
        Column(
          modifier = Modifier.align(Alignment.Center),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          CircularProgressIndicator(strokeWidth = 2.5.dp)
          Spacer(modifier = Modifier.height(12.dp))
          Text("Rendering document…", style = MaterialTheme.typography.bodyMedium, color = extra.textSecondary)
        }
      } else {
        if (scrollMode == PdfScrollMode.HORIZONTAL) {
          // Horizontal Page Carousel (Swipe left/right like a book)
          val pagerState = rememberPagerState(initialPage = currentPage) { pageCount }

          LaunchedEffect(currentPage) {
            if (pagerState.currentPage != currentPage) {
              pagerState.animateScrollToPage(currentPage)
            }
          }
          LaunchedEffect(pagerState.currentPage) {
            if (currentPage != pagerState.currentPage) {
              currentPage = pagerState.currentPage
            }
          }

          HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
          ) { pageIdx ->
            PdfSinglePageView(
              pdfFile = pdfFile,
              pageIndex = pageIdx,
              readerTheme = readerTheme,
              paperColor = paperColor,
              zoomScale = zoomScale,
              panOffset = panOffset,
              onZoomChange = { scale, offset ->
                zoomScale = scale
                panOffset = offset
              },
              activeTool = activeTool,
              penColor = penColor,
              penStrokeWidth = penStrokeWidth,
              highlighterColor = highlighterColor,
              strokes = pageStrokes.getOrPut(pageIdx) { mutableListOf() },
              activeDrawingPoints = if (pageIdx == currentPage) activeDrawingPoints else emptyList(),
              onUpdateActivePoints = { activeDrawingPoints = it },
              onAddStroke = { stroke -> pageStrokes.getOrPut(pageIdx) { mutableListOf() }.add(stroke) },
              onEraseNearby = { touchOffset ->
                val list = pageStrokes[pageIdx] ?: return@PdfSinglePageView
                list.removeAll { stroke ->
                  stroke.points.any { p -> hypot((p.x - touchOffset.x).toDouble(), (p.y - touchOffset.y).toDouble()) < 30.0 }
                }
              },
              searchMatch = matchingPages.contains(pageIdx),
              searchQuery = searchQuery
            )
          }
        } else {
          // Vertical Continuous Scroll View (Scroll up/down through all pages)
          val listState = rememberLazyListState()

          LaunchedEffect(currentPage) {
            if (!listState.isScrollInProgress) {
              listState.animateScrollToItem(currentPage)
            }
          }

          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            items(pageCount) { pageIdx ->
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                  modifier = Modifier.padding(bottom = 6.dp)
                ) {
                  Text(
                    text = "Page ${pageIdx + 1} of $pageCount",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                PdfSinglePageView(
                  pdfFile = pdfFile,
                  pageIndex = pageIdx,
                  readerTheme = readerTheme,
                  paperColor = paperColor,
                  zoomScale = zoomScale,
                  panOffset = panOffset,
                  onZoomChange = { scale, offset ->
                    zoomScale = scale
                    panOffset = offset
                  },
                  activeTool = activeTool,
                  penColor = penColor,
                  penStrokeWidth = penStrokeWidth,
                  highlighterColor = highlighterColor,
                  strokes = pageStrokes.getOrPut(pageIdx) { mutableListOf() },
                  activeDrawingPoints = if (pageIdx == currentPage) activeDrawingPoints else emptyList(),
                  onUpdateActivePoints = { activeDrawingPoints = it },
                  onAddStroke = { stroke -> pageStrokes.getOrPut(pageIdx) { mutableListOf() }.add(stroke) },
                  onEraseNearby = { touchOffset ->
                    val list = pageStrokes[pageIdx] ?: return@PdfSinglePageView
                    list.removeAll { stroke ->
                      stroke.points.any { p -> hypot((p.x - touchOffset.x).toDouble(), (p.y - touchOffset.y).toDouble()) < 30.0 }
                    }
                  },
                  searchMatch = matchingPages.contains(pageIdx),
                  searchQuery = searchQuery
                )
              }
            }
          }
        }

        // Floating zoom badge (tap to reset to 100%)
        if (zoomScale > 1.05f) {
          Surface(
            onClick = {
              zoomScale = 1f
              panOffset = Offset.Zero
            },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(14.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${(zoomScale * 100).toInt()}% · Reset",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface
              )
            }
          }
        }
      }
    }

    // Bottom Navigation & Reading Controls Bar (No download/share clutter!)
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 3.dp,
      border = BorderStroke(1.dp, extra.border.copy(alpha = 0.5f))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          enabled = currentPage > 0,
          onClick = {
            currentPage = (currentPage - 1).coerceAtLeast(0)
            zoomScale = 1f
            panOffset = Offset.Zero
          },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
        }

        Text(
          text = "${currentPage + 1} / $pageCount",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 4.dp)
        )

        IconButton(
          enabled = currentPage < pageCount - 1,
          onClick = {
            currentPage = (currentPage + 1).coerceAtMost(pageCount - 1)
            zoomScale = 1f
            panOffset = Offset.Zero
          },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
        }

        if (pageCount > 1) {
          Slider(
            value = currentPage.toFloat(),
            onValueChange = {
              currentPage = it.toInt().coerceIn(0, pageCount - 1)
              zoomScale = 1f
              panOffset = Offset.Zero
            },
            valueRange = 0f..(pageCount - 1).toFloat(),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
          )
        } else {
          Spacer(modifier = Modifier.weight(1f))
        }

        // Bookmark page toggle
        val isBookmarked = currentPage in bookmarkedPages
        IconButton(
          onClick = { onToggleBookmark(currentPage) },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else extra.textSecondary
          )
        }

        // Reading Theme switch (Light / Sepia / Dark)
        IconButton(
          onClick = {
            readerTheme = when (readerTheme) {
              ReaderTheme.LIGHT -> ReaderTheme.SEPIA
              ReaderTheme.SEPIA -> ReaderTheme.DARK
              ReaderTheme.DARK -> ReaderTheme.LIGHT
            }
          },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Filled.Palette, contentDescription = "Theme", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
private fun PdfSinglePageView(
  pdfFile: File,
  pageIndex: Int,
  readerTheme: ReaderTheme,
  paperColor: Color,
  zoomScale: Float,
  panOffset: Offset,
  onZoomChange: (Float, Offset) -> Unit,
  activeTool: AnnotationTool,
  penColor: Color,
  penStrokeWidth: Float,
  highlighterColor: Color,
  strokes: List<DrawnStroke>,
  activeDrawingPoints: List<Offset>,
  onUpdateActivePoints: (List<Offset>) -> Unit,
  onAddStroke: (DrawnStroke) -> Unit,
  onEraseNearby: (Offset) -> Unit,
  searchMatch: Boolean,
  searchQuery: String,
) {
  val extra = LocalCloudBoxColors.current

  // Render Page Bitmap
  val bitmap by produceState<Bitmap?>(initialValue = null, pdfFile, pageIndex, readerTheme) {
    value = withContext(Dispatchers.IO) {
      try {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
          PdfRenderer(pfd).use { renderer ->
            if (pageIndex in 0 until renderer.pageCount) {
              renderer.openPage(pageIndex).use { page ->
                val scale = 2.0f
                val w = (page.width * scale).toInt().coerceIn(300, 2600)
                val h = (page.height * scale).toInt().coerceIn(400, 3400)
                val raw = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val c = Canvas(raw)
                c.drawColor(android.graphics.Color.WHITE)
                page.render(raw, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                if (readerTheme == ReaderTheme.LIGHT) {
                  raw
                } else {
                  val themed = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                  val tc = Canvas(themed)
                  val paint = Paint()
                  val cm = if (readerTheme == ReaderTheme.DARK) {
                    ColorMatrix(floatArrayOf(
                      -1f, 0f, 0f, 0f, 240f,
                      0f, -1f, 0f, 0f, 240f,
                      0f, 0f, -1f, 0f, 250f,
                      0f, 0f, 0f, 1f, 0f
                    ))
                  } else {
                    ColorMatrix(floatArrayOf(
                      0.97f, 0f, 0f, 0f, 0f,
                      0f, 0.93f, 0f, 0f, 0f,
                      0f, 0f, 0.85f, 0f, 0f,
                      0f, 0f, 0f, 1f, 0f
                    ))
                  }
                  paint.colorFilter = ColorMatrixColorFilter(cm)
                  tc.drawBitmap(raw, 0f, 0f, paint)
                  raw.recycle()
                  themed
                }
              }
            } else null
          }
        }
      } catch (_: Exception) {
        null
      }
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    val bmp = bitmap
    if (bmp != null) {
      Box(
        modifier = Modifier
          .graphicsLayer(
            scaleX = zoomScale,
            scaleY = zoomScale,
            translationX = panOffset.x,
            translationY = panOffset.y
          )
          // Pinch-to-zoom & double-tap gesture when in VIEW mode
          .pointerInput(activeTool) {
            if (activeTool == AnnotationTool.VIEW) {
              detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (zoomScale * zoom).coerceIn(1f, 4f)
                val newOffset = if (newScale > 1f) {
                  Offset(panOffset.x + pan.x, panOffset.y + pan.y)
                } else {
                  Offset.Zero
                }
                onZoomChange(newScale, newOffset)
              }
            }
          }
          .pointerInput(activeTool) {
            if (activeTool == AnnotationTool.VIEW) {
              detectTapGestures(
                onDoubleTap = {
                  if (zoomScale > 1.05f) {
                    onZoomChange(1f, Offset.Zero)
                  } else {
                    onZoomChange(2.2f, Offset.Zero)
                  }
                }
              )
            }
          }
          // Pen & Highlighter & Eraser gesture
          .pointerInput(activeTool, penColor, penStrokeWidth, highlighterColor) {
            if (activeTool == AnnotationTool.PEN || activeTool == AnnotationTool.HIGHLIGHTER) {
              detectDragGestures(
                onDragStart = { start ->
                  onUpdateActivePoints(listOf(start))
                },
                onDrag = { change, _ ->
                  change.consume()
                  onUpdateActivePoints(activeDrawingPoints + change.position)
                },
                onDragEnd = {
                  if (activeDrawingPoints.size >= 2) {
                    val isHl = activeTool == AnnotationTool.HIGHLIGHTER
                    val stroke = DrawnStroke(
                      points = activeDrawingPoints,
                      color = if (isHl) highlighterColor.copy(alpha = 0.35f) else penColor,
                      strokeWidth = if (isHl) 28f else penStrokeWidth,
                      isHighlighter = isHl
                    )
                    onAddStroke(stroke)
                  }
                  onUpdateActivePoints(emptyList())
                },
                onDragCancel = {
                  onUpdateActivePoints(emptyList())
                }
              )
            } else if (activeTool == AnnotationTool.ERASER) {
              detectDragGestures { change, _ ->
                change.consume()
                onEraseNearby(change.position)
              }
            }
          }
      ) {
        // PDF Page Bitmap
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = "PDF Page ${pageIndex + 1}",
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, extra.border.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        )

        // Highlight indicator overlay if matched in search
        if (searchMatch && searchQuery.isNotBlank()) {
          Surface(
            color = Color(0xFFF59E0B).copy(alpha = 0.22f),
            modifier = Modifier
              .matchParentSize()
              .clip(RoundedCornerShape(8.dp))
              .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
          ) {
            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.TopStart) {
              Surface(
                color = Color(0xFFF59E0B),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "Search Match: “$searchQuery”",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.Black,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }
        }

        // Annotation Canvas (Draws strokes & active drawing path)
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
          // Draw completed strokes
          strokes.forEach { stroke ->
            if (stroke.points.size >= 2) {
              val path = Path().apply {
                moveTo(stroke.points.first().x, stroke.points.first().y)
                for (i in 1 until stroke.points.size) {
                  lineTo(stroke.points[i].x, stroke.points[i].y)
                }
              }
              drawPath(
                path = path,
                color = stroke.color,
                style = Stroke(
                  width = stroke.strokeWidth,
                  cap = StrokeCap.Round,
                  join = StrokeJoin.Round
                )
              )
            }
          }

          // Draw active drawing points currently being dragged
          if (activeDrawingPoints.size >= 2) {
            val isHl = activeTool == AnnotationTool.HIGHLIGHTER
            val strokeCol = if (isHl) highlighterColor.copy(alpha = 0.35f) else penColor
            val strokeW = if (isHl) 28f else penStrokeWidth
            val activePath = Path().apply {
              moveTo(activeDrawingPoints.first().x, activeDrawingPoints.first().y)
              for (i in 1 until activeDrawingPoints.size) {
                lineTo(activeDrawingPoints[i].x, activeDrawingPoints[i].y)
              }
            }
            drawPath(
              path = activePath,
              color = strokeCol,
              style = Stroke(
                width = strokeW,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
              )
            )
          }
        }
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(420.dp),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
      }
    }
  }
}
