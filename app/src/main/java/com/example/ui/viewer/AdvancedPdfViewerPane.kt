package com.example.ui.viewer

import com.example.model.ReaderTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Print
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

enum class PenTip { Round, Marker, Calligraphy }

data class DrawnStroke(
  val points: List<Offset>,
  val color: Color,
  val strokeWidth: Float,
  val isHighlighter: Boolean = false,
  val tip: PenTip = PenTip.Round,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedPdfViewerPane(
  fileMeta: CloudFile,
  pdfFile: File,
  immersive: Boolean,
  toggleImmersive: () -> Unit,
  onSettings: () -> Unit,
  bookmarkedPages: List<Int>,
  onToggleBookmark: (Int) -> Unit,
  onBack: () -> Unit,
  onOpenMoreActions: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val extra = LocalCloudBoxColors.current
  val scope = rememberCoroutineScope()

  val account = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
  val preferences = remember(account, fileMeta.id, fileMeta.modifiedTimeEpochMs) {
    context.getSharedPreferences("reader_${account}_${fileMeta.id}_${fileMeta.modifiedTimeEpochMs}", android.content.Context.MODE_PRIVATE)
  }
  val ink = remember(preferences) { PageInk(preferences) }
  var pageDialog by remember { mutableStateOf(false) }
  var indexing by remember(pdfFile) { mutableStateOf(true) }
  var pageError by remember(pdfFile) { mutableStateOf(false) }
  var markerWidth by remember { mutableFloatStateOf(28f) }
  var eraseRadius by remember { mutableFloatStateOf(30f) }
  var penTip by remember { mutableStateOf(PenTip.Round) }
  var opacity by remember { mutableFloatStateOf(0.35f) }
  var pageCount by remember(pdfFile) { mutableIntStateOf(0) }
  var currentPage by remember(pdfFile) { mutableIntStateOf(preferences.getInt("page", 0)) }
  val kind = ReaderKind.forFile(fileMeta)
  val direction = readerDefault(ReaderKind.PDF)
  val appearance = readerDefault(kind)
  var scrollMode by remember(direction) {
    mutableStateOf(if (kind != ReaderKind.SLIDES && direction == "Vertical") PdfScrollMode.VERTICAL else PdfScrollMode.HORIZONTAL)
  }
  var readerTheme by remember(appearance) { mutableStateOf(ReaderTheme.entries.firstOrNull { it.name.equals(appearance, true) } ?: ReaderTheme.valueOf(preferences.getString("theme", "LIGHT")!!)) }

  LaunchedEffect(currentPage, scrollMode, readerTheme) {
    preferences.edit().putInt("page", currentPage).putString("theme", readerTheme.name).apply()
  }
  // Zoom & Pan state
  var zoomScale by remember { mutableFloatStateOf(1f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  // Annotation states (stored per page)
  var activeTool by remember { mutableStateOf(AnnotationTool.VIEW) }
  var penColor by remember { mutableStateOf(Color(0xFF2563EB)) }
  var penStrokeWidth by remember { mutableFloatStateOf(8f) }
  var highlighterColor by remember { mutableStateOf(Color(0xFFFDE047)) }
  val pageStrokes = remember(pdfFile) { mutableStateMapOf<Int, MutableList<DrawnStroke>>() }
  fun strokesFor(page: Int) = pageStrokes.getOrPut(page) {
    androidx.compose.runtime.mutableStateListOf<DrawnStroke>().apply { addAll(ink.load(page)) }
  }
  var drawingPage by remember { mutableIntStateOf(-1) }
  var activeDrawingPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

  // In-Document Search state
  var showSearch by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var pages by remember(pdfFile) { mutableStateOf<Map<Int, IndexedPage>>(emptyMap()) }
  val extractedPageTexts = remember(pages) { pages.mapValues { it.value.text } }
  var matchingPages by remember { mutableStateOf<List<Int>>(emptyList()) }
  var currentMatchIndex by remember { mutableIntStateOf(0) }

  var scanJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
  var scanStatus by remember { mutableStateOf("") }
  fun scanMissingPages() {
    val missing = (0 until pageCount).filter { pages[it]?.text.isNullOrBlank() }.toSet()
    scanJob = scope.launch {
      scanStatus = "Reading scan…"
      var done = 0
      try {
        withContext(Dispatchers.IO) {
          recognizePdf(pdfFile, missing) { index, page ->
            withContext(Dispatchers.Main) {
              pages = pages + (index to page)
              done++
              scanStatus = "OCR $done/${missing.size}"
            }
          }
        }
        scanStatus = "OCR complete"
      } catch (e: kotlinx.coroutines.CancellationException) {
        scanStatus = "OCR stopped"
        throw e
      } catch (_: Exception) { scanStatus = "Could not recognize text" }
      finally { scanJob = null }
    }
  }

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
            currentPage = currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
          }
        }
      } catch (_: Exception) { pageError = true }
    }
  }

  // Background text extraction for search and TTS
  LaunchedEffect(pdfFile, pageCount) {
    if (pageCount > 0) {
      withContext(Dispatchers.IO) {
        val texts = try { PdfTextExtractor.index(context, pdfFile) } catch (_: java.io.IOException) { emptyMap() }
        withContext(Dispatchers.Main) {
          pages = texts
          indexing = false
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
      kotlinx.coroutines.delay(200)
      val matches = withContext(Dispatchers.Default) {
        buildList {
          for (p in 0 until pageCount) repeat(pages[p]?.bounds(q)?.size ?: 0) { add(p) }
        }
      }
      matchingPages = matches
      currentMatchIndex = 0
      if (matches.isNotEmpty()) {
        currentPage = matches.first()
      }
    }
  }

  if (pageDialog) PdfPageDialog(pdfFile, fileMeta.name, currentPage, pageCount, bookmarkedPages, fileMeta.canDownload, { currentPage = it }, { pageDialog = false })

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
    if (!immersive) Surface(
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
              text = "${when (fileMeta.mimeType) { "application/vnd.google-apps.presentation" -> "Slides"; "application/vnd.google-apps.document" -> "Docs"; else -> "PDF" }} · ${if (pageCount > 0) "Page ${currentPage + 1} of $pageCount" else "Loading..."}",
              style = MaterialTheme.typography.bodySmall,
              color = extra.textSecondary,
            )
          }

        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
          IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Viewer settings") }
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

          if (fileMeta.canDownload) {
            IconButton(onClick = { printPdfFile(context, pdfFile, fileMeta.name) }) { Icon(Icons.Filled.Print, "Print original") }
          }
          if (fileMeta.isGoogleWorkspaceDoc && fileMeta.webViewLink?.startsWith("https://") == true) {
            TextButton(onClick = {
              try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fileMeta.webViewLink))) }
              catch (_: android.content.ActivityNotFoundException) { android.widget.Toast.makeText(context, "No app available to open this link", android.widget.Toast.LENGTH_SHORT).show() }
            }) { Text("Open in Google") }
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
                  enabled = matchingPages.size > 1,
                  onClick = {
                    currentMatchIndex = (currentMatchIndex + matchingPages.size - 1) % matchingPages.size
                    currentPage = matchingPages[currentMatchIndex]
                  },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous match")
                }
                IconButton(
                  enabled = matchingPages.size > 1,
                  onClick = {
                    currentMatchIndex = (currentMatchIndex + 1) % matchingPages.size
                    currentPage = matchingPages[currentMatchIndex]
                  },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next match")
                }
              } else if (searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (indexing) "Indexing…" else if (extractedPageTexts.values.none { it.isNotBlank() }) "No text layer" else "No match", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }

        if (showSearch && !indexing && (scanStatus.isNotEmpty() || (0 until pageCount).any { pages[it]?.text.isNullOrBlank() })) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (scanStatus.isNotEmpty()) Text(scanStatus, style = MaterialTheme.typography.labelSmall)
            if (scanJob != null) TextButton(onClick = { scanJob?.cancel() }) { Text("Stop OCR") }
            else if ((0 until pageCount).any { pages[it]?.text.isNullOrBlank() }) {
              TextButton(onClick = { scanMissingPages() }) { Text("Recognize scanned pages") }
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
                      ink.save(currentPage, list)
                    }
                  },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Filled.Undo, contentDescription = "Undo")
                }

                // Clear page annotations
                TextButton(
                  onClick = { strokesFor(currentPage).clear(); ink.save(currentPage, emptyList()) }
                ) {
                  Text("Clear", color = MaterialTheme.colorScheme.error)
                }
              }

              if (activeTool == AnnotationTool.ERASER) {
                Text("Eraser radius ${eraseRadius.toInt()} px")
                Slider(value = eraseRadius, onValueChange = { eraseRadius = it }, valueRange = 6f..70f)
              }
              if (activeTool == AnnotationTool.PEN) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                  PenTip.entries.forEach { tip ->
                    FilterChip(selected = penTip == tip, onClick = { penTip = tip }, label = { Text(tip.name) })
                  }
                }
              }
              if (activeTool == AnnotationTool.HIGHLIGHTER) {
                Text("Opacity ${(opacity * 100).toInt()}%")
                Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.1f..0.8f)
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
                    val isSelected = (if (activeTool == AnnotationTool.PEN) penStrokeWidth else markerWidth) == w
                    FilterChip(
                      selected = isSelected,
                      onClick = { if (activeTool == AnnotationTool.PEN) penStrokeWidth = w else markerWidth = w },
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
          if (!pageError) CircularProgressIndicator(strokeWidth = 2.5.dp)
          Spacer(modifier = Modifier.height(12.dp))
          Text(if (pageError) "Unable to open this PDF. It may be locked or damaged." else "Rendering document…", style = MaterialTheme.typography.bodyMedium, color = extra.textSecondary)
        }
      } else {
        if (scrollMode == PdfScrollMode.HORIZONTAL) {
          // Horizontal Page Carousel (Swipe left/right like a book)
          val pagerState = rememberPagerState(initialPage = currentPage) { pageCount }

          LaunchedEffect(currentPage) {
            if (pagerState.currentPage != currentPage) {
              pagerState.scrollToPage(currentPage)
            }
          }
          LaunchedEffect(pagerState.currentPage) {
            if (currentPage != pagerState.currentPage) {
              currentPage = pagerState.currentPage
            }
          }

          HorizontalPager(
            state = pagerState,
            userScrollEnabled = activeTool == AnnotationTool.VIEW && zoomScale <= 1f,
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
              penStrokeWidth = if (activeTool == AnnotationTool.HIGHLIGHTER) markerWidth else penStrokeWidth,
              penTip = penTip,
              highlighterColor = highlighterColor.copy(alpha = opacity),
              strokes = strokesFor(pageIdx),
              activeDrawingPoints = if (pageIdx == drawingPage) activeDrawingPoints else emptyList(),
              onUpdateActivePoints = { drawingPage = pageIdx; activeDrawingPoints = it },
              onAddStroke = { stroke -> strokesFor(pageIdx).add(stroke); ink.save(pageIdx, strokesFor(pageIdx)) },
              onEraseNearby = { touchOffset, width, height ->
                val list = pageStrokes[pageIdx] ?: return@PdfSinglePageView
                if (list.removeAll { stroke ->
                  stroke.intersects(touchOffset, width, height, eraseRadius)
                }) ink.save(pageIdx, list)
              },
              indexedPage = pages[pageIdx],
              activeHit = if (matchingPages.getOrNull(currentMatchIndex) == pageIdx) currentMatchIndex - matchingPages.indexOf(pageIdx) else -1,
              searchQuery = searchQuery
            )
          }
        } else {
          // Vertical Continuous Scroll View (Scroll up/down through all pages)
          val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)
          LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress) currentPage = listState.firstVisibleItemIndex
          }

          LaunchedEffect(currentPage) {
            if (!listState.isScrollInProgress) {
              listState.scrollToItem(currentPage)
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
                  penStrokeWidth = if (activeTool == AnnotationTool.HIGHLIGHTER) markerWidth else penStrokeWidth,
              penTip = penTip,
                  highlighterColor = highlighterColor.copy(alpha = opacity),
                  strokes = strokesFor(pageIdx),
                  activeDrawingPoints = if (pageIdx == drawingPage) activeDrawingPoints else emptyList(),
                  onUpdateActivePoints = { drawingPage = pageIdx; activeDrawingPoints = it },
                  onAddStroke = { stroke -> strokesFor(pageIdx).add(stroke); ink.save(pageIdx, strokesFor(pageIdx)) },
                  onEraseNearby = { touchOffset, width, height ->
                    val list = pageStrokes[pageIdx] ?: return@PdfSinglePageView
                    if (list.removeAll { stroke ->
                      stroke.intersects(touchOffset, width, height, eraseRadius)
                    }) ink.save(pageIdx, list)
                  },
                  indexedPage = pages[pageIdx],
              activeHit = if (matchingPages.getOrNull(currentMatchIndex) == pageIdx) currentMatchIndex - matchingPages.indexOf(pageIdx) else -1,
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
      if (immersive) TextButton(onClick = toggleImmersive, modifier = Modifier.align(Alignment.TopEnd)) { Text("Exit full screen") }
    }

    // Bottom Navigation & Reading Controls Bar (No download/share clutter!)
    if (!immersive) Surface(
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
          modifier = Modifier.clickable(enabled = pageCount > 0) { pageDialog = true }.padding(horizontal = 4.dp)
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

        IconButton(onClick = toggleImmersive) { Icon(Icons.Filled.Fullscreen, "Full screen") }
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
  penTip: PenTip,
  highlighterColor: Color,
  strokes: List<DrawnStroke>,
  activeDrawingPoints: List<Offset>,
  onUpdateActivePoints: (List<Offset>) -> Unit,
  onAddStroke: (DrawnStroke) -> Unit,
  onEraseNearby: (Offset, Float, Float) -> Unit,
  indexedPage: IndexedPage?,
  activeHit: Int,
  searchQuery: String,
) {
  val extra = LocalCloudBoxColors.current

  val latestZoom by androidx.compose.runtime.rememberUpdatedState(zoomScale)
  val latestOffset by androidx.compose.runtime.rememberUpdatedState(panOffset)
  // Render Page Bitmap
  val bitmap by produceState<Bitmap?>(initialValue = null, pdfFile, pageIndex, readerTheme) {
    value = withContext(Dispatchers.IO) {
      try {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
          PdfRenderer(pfd).use { renderer ->
            if (pageIndex in 0 until renderer.pageCount) {
              renderer.openPage(pageIndex).use { page ->
                val scale = minOf(2f, 2600f / page.width, 3400f / page.height)
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
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
              awaitEachGesture {
                var scale = latestZoom
                var offset = latestOffset
                do {
                  val event = awaitPointerEvent()
                  if (event.changes.count { it.pressed } >= 2 || scale > 1f && event.changes.any { it.position != it.previousPosition }) {
                    scale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                    offset = if (scale > 1f) offset + event.calculatePan() else Offset.Zero
                    onZoomChange(scale, offset)
                    event.changes.forEach { it.consume() }
                  }
                } while (event.changes.any { it.pressed })
              }

            }
          }
          .pointerInput(activeTool) {
            if (activeTool == AnnotationTool.VIEW) {
              detectTapGestures(
                onDoubleTap = {
                  if (latestZoom > 1.05f) {
                    onZoomChange(1f, Offset.Zero)
                  } else {
                    onZoomChange(2.2f, Offset.Zero)
                  }
                }
              )
            }
          }
          // Pen & Highlighter & Eraser gesture
          .pointerInput(activeTool, penColor, penStrokeWidth, highlighterColor, penTip) {
            var points = emptyList<Offset>()
            if (activeTool == AnnotationTool.PEN || activeTool == AnnotationTool.HIGHLIGHTER) {
              detectDragGestures(
                onDragStart = { start ->
                  points = listOf(start)
                  onUpdateActivePoints(points)
                },
                onDrag = { change, _ ->
                  change.consume()
                  points = points + change.position
                  onUpdateActivePoints(points)
                },
                onDragEnd = {
                  if (points.size >= 2) {
                    val isHl = activeTool == AnnotationTool.HIGHLIGHTER
                    val stroke = DrawnStroke(
                      points = points.map { Offset(it.x / size.width, it.y / size.height) },
                      color = if (isHl) highlighterColor else penColor,
                      strokeWidth = penStrokeWidth / size.width,
                      tip = if (isHl) PenTip.Marker else penTip,
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
                onEraseNearby(change.position, size.width.toFloat(), size.height.toFloat())
              }
            }
          }
      ) {
        // PDF Page Bitmap
        Image(
          bitmap = bmp.asImageBitmap(),
          contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
          contentDescription = "PDF Page ${pageIndex + 1}",
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bmp.width.toFloat() / bmp.height)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, extra.border.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        )

        // Highlight indicator overlay if matched in search
        val rectangles = remember(indexedPage, searchQuery) { indexedPage?.bounds(searchQuery.trim()).orEmpty() }
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
          rectangles.forEachIndexed { index, r ->
            drawRect((if (index == activeHit) Color(0xFFFF9000) else Color.Yellow).copy(alpha = 0.48f), Offset(r.left * size.width, r.top * size.height),
              androidx.compose.ui.geometry.Size(r.width() * size.width, r.height() * size.height))
          }
        }

        // Annotation Canvas (Draws strokes & active drawing path)
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
          for (stroke in strokes) {
            drawInk(stroke.points, stroke.color, stroke.strokeWidth * size.width, stroke.tip, normalized = true)
          }
          val marking = activeTool == AnnotationTool.HIGHLIGHTER
          drawInk(activeDrawingPoints, if (marking) highlighterColor else penColor,
            penStrokeWidth, if (marking) PenTip.Marker else penTip, normalized = false)
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


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInk(
  points: List<Offset>, color: Color, width: Float, tip: PenTip, normalized: Boolean,
) {
  if (points.size < 2) return
  fun point(i: Int) = if (normalized) Offset(points[i].x * size.width, points[i].y * size.height) else points[i]
  val path = Path()
  val start = point(0)
  if (tip == PenTip.Calligraphy) {
    val nib = Offset(width * 0.42f, -width * 0.26f)
    path.moveTo(start.x + nib.x, start.y + nib.y)
    for (i in 1 until points.size) (point(i) + nib).let { path.lineTo(it.x, it.y) }
    for (i in points.indices.reversed()) (point(i) - nib).let { path.lineTo(it.x, it.y) }
    path.close()
    drawPath(path, color)
  } else {
    path.moveTo(start.x, start.y)
    for (i in 1 until points.size) point(i).let { path.lineTo(it.x, it.y) }
    drawPath(path, color, style = Stroke(width, cap = if (tip == PenTip.Marker) StrokeCap.Square else StrokeCap.Round, join = StrokeJoin.Round))
  }
}

private fun DrawnStroke.intersects(touch: Offset, width: Float, height: Float, radius: Float): Boolean {
  val distance = radius + strokeWidth * width / 2
  for (i in 1 until points.size) {
    val a = Offset(points[i - 1].x * width, points[i - 1].y * height)
    val b = Offset(points[i].x * width, points[i].y * height)
    val delta = b - a
    val lengthSquared = delta.getDistanceSquared()
    val fromA = touch - a
    val t = if (lengthSquared == 0f) 0f else ((fromA.x * delta.x + fromA.y * delta.y) / lengthSquared).coerceIn(0f, 1f)
    if ((fromA - delta * t).getDistanceSquared() <= distance * distance) return true
  }
  return false
}
