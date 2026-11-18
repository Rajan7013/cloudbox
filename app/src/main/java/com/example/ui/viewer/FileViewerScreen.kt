package com.example.ui.viewer

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FileRepository
import com.example.model.CloudFile
import com.example.model.FileCategory
import com.example.model.OfflineSyncStatus
import com.example.ui.components.CloudBoxErrorCard
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun FileViewerScreen(
  file: CloudFile,
  siblingImages: List<CloudFile>,
  repository: FileRepository,
  isOnline: Boolean,
  autoplayMedia: Boolean,
  keepScreenOn: Boolean,
  onBack: () -> Unit,
  onSelectSiblingImage: (CloudFile) -> Unit,
  onOpenMoreActions: (CloudFile) -> Unit,
  onDownload: (CloudFile) -> Unit,
  onShare: (CloudFile) -> Unit,
  onToggleStar: (CloudFile) -> Unit,
  onToggleOffline: (CloudFile) -> Unit,
  onShowDetails: (CloudFile) -> Unit,
  onShowToast: (String) -> Unit,
) {
  val context = LocalContext.current
  val extra = LocalCloudBoxColors.current
  val scope = rememberCoroutineScope()
  val loader = remember(repository) { ViewerContentLoader(repository) }
  val state by loader.state.collectAsStateWithLifecycle()
  val pdfBookmarks by loader.observePdfBookmarks(file.id).collectAsStateWithLifecycle(initialValue = emptyList())

  DisposableEffect(keepScreenOn) {
    val window = (context as? Activity)?.window
    if (keepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    onDispose {
      if (keepScreenOn) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  LaunchedEffect(file.id, isOnline) {
    loader.loadFile(file, isOnline)
  }

  // If this is a PDF, launch the full-screen immersive PDF reader with no bottom clutter
  if (state is ViewerContentState.PdfFile) {
    val s = state as ViewerContentState.PdfFile
    AdvancedPdfViewerPane(
      fileMeta = file,
      pdfFile = s.file,
      bookmarkedPages = pdfBookmarks,
      onToggleBookmark = { pageIdx ->
        scope.launch { loader.togglePdfBookmark(file.id, pageIdx) }
      },
      onBack = onBack,
      onOpenMoreActions = { onOpenMoreActions(file) },
      modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
    )
    return
  }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp).testTag("file_viewer_screen")) {
    // Top bar
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("viewer_back_button")) {
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
          text = "${file.category.label} · ${FormatUtils.formatBytes(file.sizeBytes)}",
          style = MaterialTheme.typography.bodySmall,
          color = extra.textSecondary,
        )
      }
      IconButton(onClick = { onOpenMoreActions(file) }, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Content area
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (val s = state) {
        is ViewerContentState.Loading -> {
          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Opening ${file.name}…", style = MaterialTheme.typography.bodyMedium, color = extra.textSecondary)
          }
        }
        is ViewerContentState.Error -> {
          Box(modifier = Modifier.align(Alignment.Center)) {
            CloudBoxErrorCard(
              message = s.message,
              onRetry = { scope.launch { loader.loadFile(file, isOnline) } },
            )
          }
        }
        is ViewerContentState.ImageFile -> {
          val imgIndex = siblingImages.indexOfFirst { it.id == file.id }
          ImageViewerPane(
            imageFile = s.file,
            canSwipePrev = imgIndex > 0,
            canSwipeNext = imgIndex in 0 until siblingImages.lastIndex,
            onSwipePrev = { if (imgIndex > 0) onSelectSiblingImage(siblingImages[imgIndex - 1]) },
            onSwipeNext = { if (imgIndex in 0 until siblingImages.lastIndex) onSelectSiblingImage(siblingImages[imgIndex + 1]) },
          )
        }
        is ViewerContentState.PdfFile -> {
          PdfViewerPane(
            fileMeta = file,
            pdfFile = s.file,
            bookmarkedPages = pdfBookmarks,
            onToggleBookmark = { pageIdx ->
              scope.launch { loader.togglePdfBookmark(file.id, pageIdx) }
            },
          )
        }
        is ViewerContentState.TextFile -> {
          TextDocumentViewerPane(textContent = s.text)
        }
        is ViewerContentState.Spreadsheet -> {
          SpreadsheetViewerPane(
            data = s.data,
            onExportCsv = { onDownload(file) },
          )
        }
        is ViewerContentState.VideoOrAudio -> {
          Media3VideoOrAudioPane(
            fileMeta = file,
            mediaFile = s.localFile,
            isVideo = file.category == FileCategory.VIDEO,
            autoplay = autoplayMedia,
          )
        }
        is ViewerContentState.Archive -> {
          ZipArchiveViewerPane(
            fileMeta = file,
            archiveFile = s.file,
            entries = s.entries,
            extractedDir = s.extractedDir,
            onExtractAll = {
              scope.launch {
                runCatching { loader.extractArchive(s.file, file.id) }
                  .onSuccess { onShowToast("Extracted ${s.entries.size} items") }
                  .onFailure { onShowToast(it.localizedMessage ?: "Extraction failed") }
              }
            },
            onOpenExtractedFile = { extractedFile ->
              openWithExternalApp(context, extractedFile, null, onShowToast)
            },
          )
        }
        is ViewerContentState.Unsupported -> {
          UnsupportedViewerPane(
            fileMeta = file,
            onDownload = { onDownload(file) },
            onOpenExternal = { openWithExternalApp(context, s.file, file.mimeType, onShowToast) },
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Persistent Bottom Action Strip
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.Filled.Download, "Download") { onDownload(file) }
      if (file.canShare) {
        ViewerToolTile(Icons.Filled.Share, "Share") { onShare(file) }
      }
      ViewerToolTile(
        icon = if (file.starred) Icons.Filled.Star else Icons.Filled.StarOutline,
        label = if (file.starred) "Starred" else "Star",
        active = file.starred,
      ) { onToggleStar(file) }
      val isOffline = file.offlineStatus == OfflineSyncStatus.SAVED
      ViewerToolTile(
        icon = Icons.Filled.OfflinePin,
        label = if (isOffline) "Saved" else "Offline",
        active = isOffline,
      ) { onToggleOffline(file) }
      ViewerToolTile(Icons.Filled.Info, "Details") { onShowDetails(file) }
    }
  }
}

private fun openWithExternalApp(
  context: android.content.Context,
  localFile: File,
  mimeType: String?,
  onError: (String) -> Unit,
) {
  try {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
    val resolvedMime =
      mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
        ?: android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(localFile.extension.lowercase())
        ?: "*/*"
    val intent =
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, resolvedMime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(intent, "Open with"))
  } catch (e: Exception) {
    onError("No installed app can open this file type")
  }
}
