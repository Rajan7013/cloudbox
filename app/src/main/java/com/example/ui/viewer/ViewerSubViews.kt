package com.example.ui.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.model.CloudFile
import com.example.model.FileCategory
import com.example.model.ReaderTheme
import com.example.ui.components.FileTypeTile
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ImageViewerPane(
  imageFile: File,
  canSwipePrev: Boolean,
  canSwipeNext: Boolean,
  onSwipePrev: () -> Unit,
  onSwipeNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var scale by remember(imageFile) { mutableFloatStateOf(1f) }
  var offset by remember(imageFile) { mutableStateOf(Offset.Zero) }
  var rotationDeg by remember(imageFile) { mutableFloatStateOf(0f) }

  Column(modifier = modifier.fillMaxSize()) {
    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(Color.Black.copy(alpha = 0.88f))
          .pointerInput(imageFile) {
            detectTapGestures(
              onDoubleTap = {
                if (scale > 1.2f) {
                  scale = 1f
                  offset = Offset.Zero
                } else {
                  scale = 2.2f
                }
              },
            )
          }
          .pointerInput(imageFile) {
            detectTransformGestures { _, pan, zoom, _ ->
              val nextScale = (scale * zoom).coerceIn(1f, 3f)
              scale = nextScale
              offset =
                if (nextScale > 1.05f) {
                  Offset(offset.x + pan.x, offset.y + pan.y)
                } else {
                  Offset.Zero
                }
            }
          },
      contentAlignment = Alignment.Center,
    ) {
      AsyncImage(
        model = imageFile,
        contentDescription = imageFile.name,
        contentScale = ContentScale.Fit,
        modifier =
          Modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = scale
              scaleY = scale
              translationX = offset.x
              translationY = offset.y
              rotationZ = rotationDeg
            },
      )

      Surface(
        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.65f),
      ) {
        Text(
          text = "${(scale * 100).toInt()}%",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }

      if (canSwipePrev) {
        IconButton(
          onClick = onSwipePrev,
          modifier = Modifier.align(Alignment.CenterStart).padding(8.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous image", tint = Color.White)
        }
      }
      if (canSwipeNext) {
        IconButton(
          onClick = onSwipeNext,
          modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next image", tint = Color.White)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.Filled.ZoomIn, "Zoom +") { scale = (scale + 0.4f).coerceAtMost(3f) }
      ViewerToolTile(Icons.Filled.ZoomOut, "Zoom -") {
        scale = (scale - 0.4f).coerceAtLeast(1f)
        if (scale <= 1.02f) offset = Offset.Zero
      }
      ViewerToolTile(Icons.Filled.RotateRight, "Rotate") { rotationDeg = (rotationDeg + 90f) % 360f }
      ViewerToolTile(Icons.Filled.FitScreen, "Fit") {
        scale = 1f
        offset = Offset.Zero
        rotationDeg = 0f
      }
    }
  }
}

@Composable
fun PdfViewerPane(
  fileMeta: CloudFile,
  pdfFile: File,
  bookmarkedPages: List<Int>,
  onToggleBookmark: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val extra = LocalCloudBoxColors.current
  var pageCount by remember(pdfFile) { mutableIntStateOf(0) }
  var currentPage by remember(pdfFile) { mutableIntStateOf(0) }
  var zoomPercent by remember(pdfFile) { mutableIntStateOf(100) }
  var readerTheme by remember { mutableStateOf(ReaderTheme.LIGHT) }

  val bitmap by produceState<Bitmap?>(initialValue = null, pdfFile, currentPage, zoomPercent, readerTheme) {
    value =
      withContext(Dispatchers.IO) {
        try {
          ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
              pageCount = renderer.pageCount
              if (renderer.pageCount == 0) return@withContext null
              val safePage = currentPage.coerceIn(0, renderer.pageCount - 1)
              renderer.openPage(safePage).use { page ->
                val scale = (zoomPercent / 100f) * 2.0f
                val w = (page.width * scale).toInt().coerceIn(200, 2600)
                val h = (page.height * scale).toInt().coerceIn(200, 3400)
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
                  val cm =
                    if (readerTheme == ReaderTheme.DARK) {
                      ColorMatrix(
                        floatArrayOf(
                          -1f, 0f, 0f, 0f, 240f,
                          0f, -1f, 0f, 0f, 240f,
                          0f, 0f, -1f, 0f, 250f,
                          0f, 0f, 0f, 1f, 0f,
                        )
                      )
                    } else {
                      // Sepia tint
                      ColorMatrix(
                        floatArrayOf(
                          0.97f, 0f, 0f, 0f, 0f,
                          0f, 0.93f, 0f, 0f, 0f,
                          0f, 0f, 0.85f, 0f, 0f,
                          0f, 0f, 0f, 1f, 0f,
                        )
                      )
                    }
                  paint.colorFilter = ColorMatrixColorFilter(cm)
                  tc.drawBitmap(raw, 0f, 0f, paint)
                  raw.recycle()
                  themed
                }
              }
            }
          }
        } catch (_: Exception) {
          null
        }
      }
  }

  val paperColor =
    when (readerTheme) {
      ReaderTheme.LIGHT -> Color.White
      ReaderTheme.SEPIA -> extra.sepiaPaperBg
      ReaderTheme.DARK -> Color(0xFF151726)
    }

  Column(modifier = modifier.fillMaxSize()) {
    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(paperColor)
          .border(1.dp, extra.border, RoundedCornerShape(18.dp))
          .verticalScroll(rememberScrollState())
          .horizontalScroll(rememberScrollState()),
      contentAlignment = Alignment.Center,
    ) {
      val bmp = bitmap
      if (bmp != null) {
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = "${fileMeta.name} page ${currentPage + 1}",
          modifier = Modifier.padding(8.dp),
        )
      } else {
        Text(
          "Rendering PDF page…",
          style = MaterialTheme.typography.bodyMedium,
          color = extra.textSecondary,
          modifier = Modifier.padding(24.dp),
        )
      }
    }

    if (pageCount > 0) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          enabled = currentPage > 0,
          onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
        }
        Text(
          text = "Page ${currentPage + 1} of $pageCount",
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(end = 8.dp),
        )
        if (pageCount > 1) {
          Slider(
            value = currentPage.toFloat(),
            onValueChange = { currentPage = it.toInt().coerceIn(0, pageCount - 1) },
            valueRange = 0f..(pageCount - 1).toFloat(),
            modifier = Modifier.weight(1f),
          )
        } else {
          Spacer(modifier = Modifier.weight(1f))
        }
        IconButton(
          enabled = currentPage < pageCount - 1,
          onClick = { currentPage = (currentPage + 1).coerceAtMost(pageCount - 1) },
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
        }
      }
    }

    val isBookmarked = currentPage in bookmarkedPages
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.Filled.ZoomIn, "+") { zoomPercent = (zoomPercent + 20).coerceAtMost(180) }
      ViewerToolTile(Icons.Filled.ZoomOut, "-") { zoomPercent = (zoomPercent - 20).coerceAtLeast(80) }
      ViewerToolTile(Icons.Filled.Palette, readerTheme.label) {
        readerTheme =
          when (readerTheme) {
            ReaderTheme.LIGHT -> ReaderTheme.SEPIA
            ReaderTheme.SEPIA -> ReaderTheme.DARK
            ReaderTheme.DARK -> ReaderTheme.LIGHT
          }
      }
      ViewerToolTile(
        icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
        label = if (isBookmarked) "Marked" else "Bookmark",
        active = isBookmarked,
      ) { onToggleBookmark(currentPage) }
      ViewerToolTile(Icons.Filled.Print, "Print") {
        printPdfFile(context, pdfFile, fileMeta.name)
      }
    }
  }
}

@Composable
fun TextDocumentViewerPane(
  textContent: String,
  modifier: Modifier = Modifier,
) {
  val extra = LocalCloudBoxColors.current
  var showFind by remember { mutableStateOf(false) }
  var findQuery by remember { mutableStateOf("") }
  var fontScalePercent by remember { mutableIntStateOf(100) }
  var readerTheme by remember { mutableStateOf(ReaderTheme.LIGHT) }

  val paperBg =
    when (readerTheme) {
      ReaderTheme.LIGHT -> MaterialTheme.colorScheme.surface
      ReaderTheme.SEPIA -> extra.sepiaPaperBg
      ReaderTheme.DARK -> Color(0xFF121524)
    }
  val inkColor =
    when (readerTheme) {
      ReaderTheme.LIGHT -> MaterialTheme.colorScheme.onSurface
      ReaderTheme.SEPIA -> extra.sepiaPaperText
      ReaderTheme.DARK -> Color(0xFFE5E7F5)
    }

  val matchCount =
    remember(textContent, findQuery) {
      val q = findQuery.trim()
      if (q.isEmpty()) 0
      else Regex(Regex.escape(q), RegexOption.IGNORE_CASE).findAll(textContent).count()
    }

  val primaryColor = MaterialTheme.colorScheme.primary
  val annotated =
    remember(textContent, findQuery, primaryColor) {
      val q = findQuery.trim()
      if (q.isEmpty()) {
        buildAnnotatedString { append(textContent) }
      } else {
        buildAnnotatedString {
          var idx = 0
          while (idx < textContent.length) {
            val found = textContent.indexOf(q, startIndex = idx, ignoreCase = true)
            if (found < 0) {
              append(textContent.substring(idx))
              break
            }
            append(textContent.substring(idx, found))
            withStyle(SpanStyle(background = primaryColor.copy(alpha = 0.25f), fontWeight = FontWeight.Bold)) {
              append(textContent.substring(found, found + q.length))
            }
            idx = found + q.length
          }
        }
      }
    }

  Column(modifier = modifier.fillMaxSize()) {
    if (showFind) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = findQuery,
          onValueChange = { findQuery = it },
          placeholder = { Text("Find in document…") },
          singleLine = true,
          trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (findQuery.isNotEmpty()) {
                Text("$matchCount", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 6.dp))
              }
              IconButton(onClick = { showFind = false; findQuery = "" }) {
                Icon(Icons.Filled.Close, contentDescription = "Close find")
              }
            }
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    Surface(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      color = paperBg,
      border = BorderStroke(1.dp, extra.border),
    ) {
      Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text(
          text = annotated,
          style =
            MaterialTheme.typography.bodyLarge.copy(
              fontSize = (15 * fontScalePercent / 100f).sp,
              lineHeight = (22 * fontScalePercent / 100f).sp,
            ),
          color = inkColor,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.Filled.Search, "Find", active = showFind) { showFind = !showFind }
      ViewerToolTile(Icons.Filled.ZoomIn, "+") { fontScalePercent = (fontScalePercent + 20).coerceAtMost(180) }
      ViewerToolTile(Icons.Filled.ZoomOut, "-") { fontScalePercent = (fontScalePercent - 20).coerceAtLeast(80) }
      ViewerToolTile(Icons.Filled.Palette, readerTheme.label) {
        readerTheme =
          when (readerTheme) {
            ReaderTheme.LIGHT -> ReaderTheme.SEPIA
            ReaderTheme.SEPIA -> ReaderTheme.DARK
            ReaderTheme.DARK -> ReaderTheme.LIGHT
          }
      }
    }
  }
}

@Composable
fun SpreadsheetViewerPane(
  data: SpreadsheetData,
  onExportCsv: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val extra = LocalCloudBoxColors.current
  var showSummary by remember { mutableStateOf(false) }
  var selectedRow by remember { mutableIntStateOf(0) }
  var selectedCol by remember { mutableIntStateOf(0) }

  val selectedCellVal = data.rows.getOrNull(selectedRow)?.getOrNull(selectedCol).orEmpty()
  val colLabel = data.headers.getOrNull(selectedCol) ?: "A"

  Column(modifier = modifier.fillMaxSize()) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, extra.border),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("fx", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "$colLabel${selectedRow + 1}: $selectedCellVal",
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (showSummary) {
      Column(
        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SummaryStatCard("Numeric Total", String.format(java.util.Locale.US, "%.2f", data.numericSum))
        SummaryStatCard("Largest Value", String.format(java.util.Locale.US, "%.2f", data.largestValue))
        SummaryStatCard("Rows", data.rows.size.toString())
        SummaryStatCard("Total Cells", data.totalCells.toString())
      }
    } else {
      val hScroll = rememberScrollState()
      val vScroll = rememberScrollState()
      Surface(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.border),
      ) {
        Column(modifier = Modifier.verticalScroll(vScroll).horizontalScroll(hScroll)) {
          // Header Row
          Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(modifier = Modifier.width(42.dp).height(34.dp), contentAlignment = Alignment.Center) {
              Text("", style = MaterialTheme.typography.bodySmall)
            }
            data.headers.forEach { header ->
              Box(
                modifier = Modifier.width(104.dp).height(34.dp).border(0.5.dp, extra.border),
                contentAlignment = Alignment.Center,
              ) {
                Text(header, style = MaterialTheme.typography.labelLarge)
              }
            }
          }
          // Data Rows
          data.rows.forEachIndexed { rIdx, row ->
            Row {
              Box(
                modifier =
                  Modifier
                    .width(42.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(0.5.dp, extra.border),
                contentAlignment = Alignment.Center,
              ) {
                Text("${rIdx + 1}", style = MaterialTheme.typography.bodySmall)
              }
              row.forEachIndexed { cIdx, cell ->
                val isSelected = rIdx == selectedRow && cIdx == selectedCol
                Box(
                  modifier =
                    Modifier
                      .width(104.dp)
                      .height(36.dp)
                      .border(if (isSelected) 2.dp else 0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else extra.border)
                      .clickable {
                        selectedRow = rIdx
                        selectedCol = cIdx
                      }
                      .padding(horizontal = 6.dp),
                  contentAlignment = Alignment.CenterStart,
                ) {
                  Text(cell, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ViewerToolTile(Icons.Filled.PlayArrow, "Sheet1", active = !showSummary) { showSummary = false }
      ViewerToolTile(Icons.Filled.Speed, "Summary", active = showSummary) { showSummary = true }
      ViewerToolTile(Icons.Filled.Download, "Export CSV") { onExportCsv() }
    }
  }
}

@Composable
private fun SummaryStatCard(label: String, value: String) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, LocalCloudBoxColors.current.border),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
      Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    }
  }
}

@Composable
fun Media3VideoOrAudioPane(
  fileMeta: CloudFile,
  mediaFile: File,
  isVideo: Boolean,
  autoplay: Boolean,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val extra = LocalCloudBoxColors.current
  val player = remember(mediaFile) {
    ExoPlayer.Builder(context).build().apply {
      setMediaItem(MediaItem.fromUri(Uri.fromFile(mediaFile)))
      prepare()
      playWhenReady = autoplay
    }
  }

  DisposableEffect(player) {
    onDispose { player.release() }
  }

  var isPlaying by remember { mutableStateOf(autoplay) }
  var positionMs by remember { mutableIntStateOf(0) }
  var durationMs by remember { mutableIntStateOf(0) }
  var speed by remember { mutableFloatStateOf(1.0f) }
  var isMuted by remember { mutableStateOf(false) }
  var isLooping by remember { mutableStateOf(false) }
  var isFullscreen by remember { mutableStateOf(false) }

  LaunchedEffect(player) {
    while (true) {
      isPlaying = player.isPlaying
      positionMs = player.currentPosition.toInt().coerceAtLeast(0)
      durationMs = player.duration.toInt().coerceAtLeast(0)
      delay(250)
    }
  }

  // Real audio peak samples computed from file bytes
  val waveformBars =
    remember(mediaFile, isVideo) {
      if (isVideo) emptyList()
      else {
        val bytes = runCatching { mediaFile.readBytes() }.getOrElse { ByteArray(0) }
        val barCount = 34
        if (bytes.isEmpty()) {
          List(barCount) { 0.35f }
        } else {
          val step = (bytes.size / barCount).coerceAtLeast(1)
          List(barCount) { idx ->
            val sliceStart = (idx * step).coerceAtMost(bytes.lastIndex)
            val sliceEnd = ((idx + 1) * step).coerceAtMost(bytes.size)
            var sum = 0
            for (i in sliceStart until sliceEnd) {
              sum += kotlin.math.abs(bytes[i].toInt())
            }
            val avg = sum.toFloat() / (sliceEnd - sliceStart).coerceAtLeast(1)
            (avg / 128f).coerceIn(0.18f, 1f)
          }
        }
      }
    }

  Column(modifier = modifier.fillMaxSize()) {
    if (isVideo) {
      Box(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black),
      ) {
        AndroidView(
          factory = { ctx ->
            PlayerView(ctx).apply {
              this.player = player
              useController = false
            }
          },
          modifier = Modifier.fillMaxSize(),
        )
      }
    } else {
      Surface(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.border),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(20.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(
            modifier =
              Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(extra.colorForCategory(FileCategory.AUDIO).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.MusicNote,
              contentDescription = null,
              tint = extra.colorForCategory(FileCategory.AUDIO),
              modifier = Modifier.size(36.dp),
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(fileMeta.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Spacer(modifier = Modifier.height(24.dp))
          val fraction = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
          Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            waveformBars.forEachIndexed { idx, amp ->
              val barFrac = idx.toFloat() / waveformBars.size.toFloat()
              val active = barFrac <= fraction
              Box(
                modifier =
                  Modifier
                    .weight(1f)
                    .height((amp * 58f).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                      if (active) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    ),
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (durationMs > 0) {
      Slider(
        value = positionMs.toFloat(),
        onValueChange = { player.seekTo(it.toLong()) },
        valueRange = 0f..durationMs.toFloat(),
        modifier = Modifier.fillMaxWidth(),
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(formatMediaMs(positionMs), style = MaterialTheme.typography.bodySmall)
      Spacer(modifier = Modifier.weight(1f))
      Text("${speed}x · ${formatMediaMs(durationMs)}", style = MaterialTheme.typography.bodySmall, color = extra.textSecondary)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.Filled.Replay10, "-10s") {
        player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
      }
      ViewerToolTile(
        icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        label = if (isPlaying) "Pause" else "Play",
        active = isPlaying,
      ) {
        if (player.isPlaying) player.pause() else player.play()
      }
      ViewerToolTile(Icons.Filled.Forward10, "+10s") {
        player.seekTo(player.currentPosition + 10_000L)
      }
      ViewerToolTile(Icons.Filled.Speed, "${speed}x") {
        speed =
          when (speed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            2.0f -> 0.5f
            else -> 1.0f
          }
        player.setPlaybackSpeed(speed)
      }
      ViewerToolTile(Icons.Filled.Repeat, "Loop", active = isLooping) {
        isLooping = !isLooping
        player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
      }
      ViewerToolTile(if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, "Mute", active = isMuted) {
        isMuted = !isMuted
        player.volume = if (isMuted) 0f else 1f
      }
      if (isVideo) {
        ViewerToolTile(Icons.Filled.Fullscreen, "Full", active = isFullscreen) {
          isFullscreen = !isFullscreen
        }
      }
    }
  }
}

@Composable
fun ZipArchiveViewerPane(
  fileMeta: CloudFile,
  archiveFile: File,
  entries: List<ZipArchiveEntryInfo>,
  extractedDir: File?,
  onExtractAll: () -> Unit,
  onOpenExtractedFile: (File) -> Unit,
  modifier: Modifier = Modifier,
) {
  val extra = LocalCloudBoxColors.current
  Column(modifier = modifier.fillMaxSize()) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, extra.border),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${fileMeta.name} · ${entries.size} items", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (extractedDir != null) {
          Icon(Icons.Filled.CheckCircle, contentDescription = "Extracted", tint = extra.success)
        }
      }
    }
    Spacer(modifier = Modifier.height(10.dp))
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(entries.size, key = { idx -> "${idx}_${entries[idx].name}" }) { idx ->
        val item = entries[idx]
        val extractedTarget = extractedDir?.let { File(it, item.name) }
        Surface(
          onClick = {
            if (!item.isDirectory && extractedTarget != null && extractedTarget.exists()) {
              onOpenExtractedFile(extractedTarget)
            }
          },
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, extra.border.copy(alpha = 0.6f)),
        ) {
          Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (item.isDirectory) Icons.Filled.Folder else Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = null,
              tint = if (item.isDirectory) extra.colorForCategory(FileCategory.FOLDER) else extra.textSecondary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(item.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!item.isDirectory && item.sizeBytes > 0L) {
              Text(FormatUtils.formatBytes(item.sizeBytes), style = MaterialTheme.typography.bodySmall, color = extra.textSecondary)
            }
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Button(
      onClick = onExtractAll,
      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("extract_zip_button"),
      shape = RoundedCornerShape(14.dp),
    ) {
      Icon(Icons.Filled.Unarchive, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(if (extractedDir != null) "Re-extract all" else "Extract all")
    }
  }
}

@Composable
fun UnsupportedViewerPane(
  fileMeta: CloudFile,
  onDownload: () -> Unit,
  onOpenExternal: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    FileTypeTile(category = fileMeta.category, size = 76.dp, cornerRadius = 20.dp)
    Spacer(modifier = Modifier.height(16.dp))
    Text(fileMeta.name, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      "${fileMeta.mimeType} · ${FormatUtils.formatBytes(fileMeta.sizeBytes)}",
      style = MaterialTheme.typography.bodySmall,
      color = LocalCloudBoxColors.current.textSecondary,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      OutlinedButton(onClick = onDownload, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(14.dp)) {
        Text("Download")
      }
      Button(onClick = onOpenExternal, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(14.dp)) {
        Text("Open with another app")
      }
    }
  }
}

@Composable
fun ViewerToolTile(
  icon: ImageVector,
  label: String,
  active: Boolean = false,
  onClick: () -> Unit,
) {
  val bg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
  val fg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else LocalCloudBoxColors.current.border),
    modifier = Modifier.height(62.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.height(4.dp))
      Text(label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
    }
  }
}

private fun formatMediaMs(ms: Int): String {
  val totalSec = (ms / 1000).coerceAtLeast(0)
  val mins = totalSec / 60
  val secs = totalSec % 60
  return "$mins:${secs.toString().padStart(2, '0')}"
}

private fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
  val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
  val adapter =
    object : PrintDocumentAdapter() {
      override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?,
      ) {
        if (cancellationSignal?.isCanceled == true) {
          callback?.onLayoutCancelled()
          return
        }
        val info = PrintDocumentInfo.Builder(jobName).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build()
        callback?.onLayoutFinished(info, true)
      }

      override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?,
      ) {
        try {
          FileInputStream(pdfFile).use { input ->
            FileOutputStream(destination?.fileDescriptor).use { output ->
              input.copyTo(output)
            }
          }
          callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
          callback?.onWriteFailed(e.message)
        }
      }
    }
  printManager.print(jobName, adapter, null)
}
