package com.example.ui.viewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
  siblings: List<CloudFile>,
  repository: FileRepository,
  isOnline: Boolean,
  autoplayMedia: Boolean,
  keepScreenOn: Boolean,
  onBack: () -> Unit,
  onSelectSibling: (CloudFile) -> Unit,
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
  val loader = remember(repository, file.id) { ViewerContentLoader(repository) }
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

  var showSettingsDialog by remember(file.id) { mutableStateOf(false) }
  val kind = ReaderKind.forFile(file)
  var browseFolder by remember(file.id) { mutableStateOf(false) }

  if (browseFolder) {
    FolderMediaDialog(file, siblings, onSelectSibling) { browseFolder = false }
  }

  if (showSettingsDialog) {
    ViewerSettingsDialog(kind) { showSettingsDialog = false }
  }

  var fullScreen by rememberSaveable(file.id) { mutableStateOf(false) }
  BackHandler(enabled = fullScreen) { fullScreen = false }

  DisposableEffect(fullScreen) {
    val window = (context as? Activity)?.window
    val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    if (fullScreen) controller?.hide(WindowInsetsCompat.Type.systemBars())
    onDispose { if (fullScreen) controller?.show(WindowInsetsCompat.Type.systemBars()) }
  }

  when (val s = state) {
    // 1. PDF Viewer (Section 3)
    is ViewerContentState.PdfFile -> {
      AdvancedPdfViewerPane(
        fileMeta = file,
        pdfFile = s.file,
        immersive = fullScreen,
        onSettings = { showSettingsDialog = true },
        toggleImmersive = { fullScreen = !fullScreen },
        bookmarkedPages = pdfBookmarks,
        onToggleBookmark = { pageIdx ->
          scope.launch { loader.togglePdfBookmark(file.id, pageIdx) }
        },
        onBack = onBack,
        onOpenMoreActions = { onOpenMoreActions(file) },
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    // 2. Word & Google Docs Viewer (Section 4)
    is ViewerContentState.Document -> {
      WordDocsViewerPane(
        file = file,
        docFile = s.file,
        initialText = s.text,
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    // 3. PowerPoint & Google Slides Viewer (Section 5)
    is ViewerContentState.SlidesDeck -> {
      SlidesViewerPane(
        file = file,
        slidesFile = s.file,
        slides = s.slides,
        notes = s.notes,
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    // 4. Excel & Google Sheets Viewer (Section 6)
    is ViewerContentState.SheetsWorkbook -> {
      SheetsViewerPane(
        file = file,
        sheetFile = s.file,
        sheets = s.sheets,
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    // 5. Markdown Viewer (Section 7)
    is ViewerContentState.Markdown -> {
      MarkdownViewerPane(
        file = file,
        mdFile = s.file,
        rawMarkdown = s.rawMarkdown,
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    // 6. Text File Viewer (Section 8)
    is ViewerContentState.TextDoc -> {
      TextFileViewerPane(
        file = file,
        textFile = s.file,
        initialContent = s.content,
        isLog = s.isLog,
        isJson = s.isJson,
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    // 7. Image Viewer (Section 9)
    is ViewerContentState.ImageFile -> {
      ImageViewerScreen(
        file = file,
        imageFile = s.file,
        siblings = siblings,
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onSelectSibling = onSelectSibling,
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    // 8. Video Viewer (Section 10)
    is ViewerContentState.Video -> {
      Media3VideoPane(
        file = file,
        videoFile = s.file,
        autoplay = autoplayMedia,
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    // 9. Audio Viewer (Section 11)
    is ViewerContentState.Audio -> {
      Media3AudioPane(
        file = file,
        audioFile = s.file,
        siblings = siblings,
        autoplay = autoplayMedia,
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onSelectSibling = onSelectSibling,
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    // Fallbacks for remaining states
    is ViewerContentState.TextFile -> {
      val isMd = file.name.substringAfterLast('.').lowercase() in setOf("md", "markdown")
      if (isMd) {
        MarkdownViewerPane(
          file = file,
          mdFile = File(context.cacheDir, file.name),
          rawMarkdown = s.text,
          onBack = onBack,
          onShare = { onShare(file) },
          onDownload = { onDownload(file) },
          onOpenMoreActions = { onOpenMoreActions(file) },
          onSettings = { showSettingsDialog = true },
          onShowToast = onShowToast,
          modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
        )
      } else {
        WordDocsViewerPane(
          file = file,
          docFile = File(context.cacheDir, file.name),
          initialText = s.text,
          onBack = onBack,
          onShare = { onShare(file) },
          onDownload = { onDownload(file) },
          onOpenMoreActions = { onOpenMoreActions(file) },
          onSettings = { showSettingsDialog = true },
          onShowToast = onShowToast,
          modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
        )
      }
    }

    is ViewerContentState.Slides -> {
      SlidesViewerPane(
        file = file,
        slidesFile = File(context.cacheDir, file.name),
        slides = s.pages,
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    is ViewerContentState.Workbook -> {
      SheetsViewerPane(
        file = file,
        sheetFile = File(context.cacheDir, file.name),
        sheets = s.sheets,
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    is ViewerContentState.Spreadsheet -> {
      SheetsViewerPane(
        file = file,
        sheetFile = s.file,
        sheets = listOf(file.name.substringBeforeLast('.') to s.data),
        onBack = onBack,
        onShare = { onShare(file) },
        onDownload = { onDownload(file) },
        onOpenMoreActions = { onOpenMoreActions(file) },
        onSettings = { showSettingsDialog = true },
        onShowToast = onShowToast,
        modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
      )
    }

    is ViewerContentState.VideoOrAudio -> {
      if (file.category == FileCategory.VIDEO) {
        Media3VideoPane(
          file = file,
          videoFile = s.localFile,
          autoplay = autoplayMedia,
          onBack = onBack,
          onShare = { onShare(file) },
          onDownload = { onDownload(file) },
          onOpenMoreActions = { onOpenMoreActions(file) },
          onSettings = { showSettingsDialog = true },
          onShowToast = onShowToast,
          modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
        )
      } else {
        Media3AudioPane(
          file = file,
          audioFile = s.localFile,
          siblings = siblings,
          autoplay = autoplayMedia,
          onBack = onBack,
          onShare = { onShare(file) },
          onDownload = { onDownload(file) },
          onOpenMoreActions = { onOpenMoreActions(file) },
          onSettings = { showSettingsDialog = true },
          onSelectSibling = onSelectSibling,
          onShowToast = onShowToast,
          modifier = Modifier.fillMaxSize().testTag("file_viewer_screen"),
        )
      }
    }

    is ViewerContentState.Archive -> {
      Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Archive · ${s.entries.size} items", style = MaterialTheme.typography.bodySmall, color = extra.textSecondary)
          }
          IconButton(onClick = { onOpenMoreActions(file) }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
        }
        Spacer(modifier = Modifier.height(10.dp))
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
    }

    is ViewerContentState.Unsupported -> {
      Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
          Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
          IconButton(onClick = { onOpenMoreActions(file) }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          UnsupportedViewerPane(
            fileMeta = file,
            onDownload = { onDownload(file) },
            onOpenExternal = { openWithExternalApp(context, s.file, file.mimeType, onShowToast) },
          )
        }
      }
    }

    is ViewerContentState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(strokeWidth = 3.dp)
          Spacer(modifier = Modifier.height(16.dp))
          Text("Opening ${file.name}…", style = MaterialTheme.typography.bodyMedium, color = extra.textSecondary)
        }
      }
    }

    is ViewerContentState.Error -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CloudBoxErrorCard(
          message = s.message,
          onRetry = { scope.launch { loader.loadFile(file, isOnline) } },
        )
      }
    }
  }
}

private fun openWithExternalApp(
  context: Context,
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
  } catch (_: Exception) {
    onError("No installed app can open this file type")
  }
}
