package com.example.ui.viewer

import com.example.data.FileRepository
import com.example.model.CloudFile
import com.example.model.FileCategory
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SpreadsheetData(
  val headers: List<String>,
  val rows: List<List<String>>,
  val numericSum: Double,
  val largestValue: Double,
  val totalCells: Int,
)

data class ZipArchiveEntryInfo(
  val name: String,
  val isDirectory: Boolean,
  val sizeBytes: Long,
)

sealed interface ViewerContentState {
  data object Loading : ViewerContentState
  data class ImageFile(val file: File) : ViewerContentState
  data class PdfFile(val file: File) : ViewerContentState
  data class Document(val file: File, val text: String, val isGoogleDoc: Boolean = false) : ViewerContentState
  data class SlidesDeck(val file: File, val slides: List<String>, val notes: List<String> = emptyList()) : ViewerContentState
  data class SheetsWorkbook(val file: File, val sheets: List<Pair<String, SpreadsheetData>>) : ViewerContentState
  data class Markdown(val file: File, val rawMarkdown: String) : ViewerContentState
  data class TextDoc(val file: File, val content: String, val isLog: Boolean = false, val isJson: Boolean = false) : ViewerContentState
  data class Video(val file: File) : ViewerContentState
  data class Audio(val file: File) : ViewerContentState
  data class TextFile(val text: String) : ViewerContentState
  data class Slides(val pages: List<String>) : ViewerContentState
  data class Workbook(val sheets: List<Pair<String, SpreadsheetData>>) : ViewerContentState
  data class Spreadsheet(val file: File, val data: SpreadsheetData) : ViewerContentState
  data class VideoOrAudio(val localFile: File) : ViewerContentState
  data class Archive(val file: File, val entries: List<ZipArchiveEntryInfo>, val extractedDir: File? = null) : ViewerContentState
  data class Unsupported(val file: File) : ViewerContentState
  data class Error(val message: String, val needsInternet: Boolean = false) : ViewerContentState
}

class ViewerContentLoader(private val repository: FileRepository) {
  private val _state = MutableStateFlow<ViewerContentState>(ViewerContentState.Loading)
  val state: StateFlow<ViewerContentState> = _state.asStateFlow()

  fun observePdfBookmarks(fileId: String): Flow<List<Int>> = repository.observePdfBookmarks(fileId)

  suspend fun togglePdfBookmark(fileId: String, pageIndex: Int) {
    repository.togglePdfBookmark(fileId, pageIndex)
  }

  suspend fun loadFile(file: CloudFile, isOnline: Boolean) {
    _state.value = ViewerContentState.Loading
    val hasLocal = file.localOfflinePath?.let { File(it).exists() } == true
    if (!isOnline && !hasLocal) {
      _state.value = ViewerContentState.Error("Needs internet to open this file", needsInternet = true)
      return
    }
    try {
      val downloaded = repository.downloadFileToCacheForViewer(file)
      _state.value =
        withContext(Dispatchers.IO) {
          val extension = if (file.isGoogleWorkspaceDoc) downloaded.extension.lowercase() else file.name.substringAfterLast('.', "").lowercase()
          when (extension) {
            "docx" -> return@withContext OfficePreview(downloaded).use { ViewerContentState.Document(downloaded, it.document()) }
            "pptx" -> return@withContext OfficePreview(downloaded).use { ViewerContentState.SlidesDeck(downloaded, it.slides()) }
            "xlsx" -> return@withContext OfficePreview(downloaded).use { ViewerContentState.SheetsWorkbook(downloaded, it.workbook()) }
            "odt" -> return@withContext OfficePreview(downloaded).use {
              val s = it.openDocument(extension)
              if (s is ViewerContentState.TextFile) ViewerContentState.Document(downloaded, s.text) else s
            }
            "odp" -> return@withContext OfficePreview(downloaded).use {
              val s = it.openDocument(extension)
              if (s is ViewerContentState.Slides) ViewerContentState.SlidesDeck(downloaded, s.pages) else s
            }
            "ods" -> return@withContext OfficePreview(downloaded).use {
              val s = it.openDocument(extension)
              if (s is ViewerContentState.Workbook) ViewerContentState.SheetsWorkbook(downloaded, s.sheets) else s
            }
            "md", "markdown" -> return@withContext ViewerContentState.Markdown(downloaded, readPreview(downloaded))
            "doc", "ppt", "xls", "rtf" -> return@withContext ViewerContentState.Unsupported(downloaded)
          }
          when (file.category) {
            FileCategory.IMAGE -> ViewerContentState.ImageFile(downloaded)
            FileCategory.PDF -> ViewerContentState.PdfFile(downloaded)
            FileCategory.VIDEO -> ViewerContentState.Video(downloaded)
            FileCategory.AUDIO -> ViewerContentState.Audio(downloaded)
            FileCategory.SPREADSHEET -> {
              val parsed = parseSpreadsheet(downloaded)
              if (parsed is ViewerContentState.Spreadsheet) {
                ViewerContentState.SheetsWorkbook(downloaded, listOf(file.name.substringBeforeLast('.') to parsed.data))
              } else parsed
            }
            FileCategory.DOCUMENT -> {
              if (file.mimeType == "application/vnd.google-apps.document" ||
                file.mimeType == "application/vnd.google-apps.presentation"
              ) {
                ViewerContentState.PdfFile(downloaded)
              } else {
                val ext = downloaded.extension.lowercase()
                if (ext == "pdf") {
                  ViewerContentState.PdfFile(downloaded)
                } else if (ext in setOf("md", "markdown")) {
                  ViewerContentState.Markdown(downloaded, readPreview(downloaded))
                } else {
                  val content = readPreview(downloaded)
                  ViewerContentState.Document(downloaded, content)
                }
              }
            }
            FileCategory.ARCHIVE -> {
              val entries = readZipEntries(downloaded)
              ViewerContentState.Archive(downloaded, entries)
            }
            FileCategory.FOLDER, FileCategory.OTHER -> {
              val ext = downloaded.extension.lowercase()
              if (ext in setOf("md", "markdown")) {
                ViewerContentState.Markdown(downloaded, readPreview(downloaded))
              } else if (ext in setOf("txt", "log", "json", "xml", "csv", "tsv", "kt", "ts", "js", "yaml", "yml", "ini", "css", "java", "c", "cpp", "html", "py", "sh")) {
                val content = readPreview(downloaded)
                ViewerContentState.TextDoc(downloaded, content, isLog = ext == "log", isJson = ext == "json")
              } else {
                ViewerContentState.Unsupported(downloaded)
              }
            }
          }
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
      throw e
    } catch (e: Exception) {
      _state.value = ViewerContentState.Error(e.localizedMessage ?: "Can’t open this file")
    }
  }

  suspend fun extractArchive(archiveFile: File, fileId: String): File =
    withContext(Dispatchers.IO) {
      val outDir = File(archiveFile.parentFile, "extracted_$fileId").apply {
        deleteRecursively()
        mkdirs()
      }
      val canonicalOutDir = outDir.canonicalFile
      ZipInputStream(FileInputStream(archiveFile)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
          val destFile = File(canonicalOutDir, entry.name).canonicalFile
          // Prevent Zip Slip path traversal
          if (destFile.path.startsWith(canonicalOutDir.path + File.separator)) {
            if (entry.isDirectory) {
              destFile.mkdirs()
            } else {
              destFile.parentFile?.mkdirs()
              FileOutputStream(destFile).use { out -> zis.copyTo(out) }
            }
          }
          zis.closeEntry()
          entry = zis.nextEntry
        }
      }
      val current = _state.value
      if (current is ViewerContentState.Archive) {
        _state.value = current.copy(extractedDir = outDir)
      }
      outDir
    }

  private fun parseSpreadsheet(file: File): ViewerContentState {
    val ext = file.extension.lowercase()
    if (ext != "csv" && ext != "tsv") {
      // Binary .xlsx/.xls not directly CSV-parsed without Apache POI; fallback safely or inspect if text
      val previewBytes = file.inputStream().use { it.readNBytes(64) }
      val looksBinary = previewBytes.any { it == 0.toByte() }
      if (looksBinary) {
        return ViewerContentState.Unsupported(file)
      }
    }
    val delimiter = if (ext == "tsv") '\t' else ','
    val rows = mutableListOf<List<String>>()
    file.bufferedReader().use { reader ->
      var row = mutableListOf<String>()
      val field = StringBuilder()
      var quoted = false
      var ch = reader.read()
      while (ch != -1 && rows.size < 500) {
        when (val c = ch.toChar()) {
          '"' -> {
            if (quoted) {
              reader.mark(1)
              if (reader.read() == '"'.code) field.append('"')
              else { quoted = false; reader.reset() }
            } else if (field.isEmpty()) quoted = true else field.append(c)
          }
          delimiter -> if (quoted) field.append(c) else { row.add(field.toString()); field.clear() }
          '\n', '\r' -> if (quoted) field.append(c) else {
            if (c == '\r') { reader.mark(1); if (reader.read() != '\n'.code) reader.reset() }
            row.add(field.toString()); rows.add(row); row = mutableListOf(); field.clear()
          }
          else -> field.append(c)
        }
        require(row.size <= 512 && field.length <= 250_000) { "Cell exceeds the local preview limit" }
        ch = reader.read()
      }
      if (rows.size < 500 && (row.isNotEmpty() || field.isNotEmpty())) { row.add(field.toString()); rows.add(row) }
    }
    return ViewerContentState.Spreadsheet(file, spreadsheetData(rows))
  }

  private fun readPreview(file: File): String = file.bufferedReader().use { reader ->
    val buffer = CharArray(8192)
    buildString {
      while (length < 250_000) {
        val count = reader.read(buffer, 0, minOf(buffer.size, 250_000 - length))
        if (count < 0) break
        append(buffer, 0, count)
      }
      if (reader.read() != -1) append("\n[Preview limited to 250,000 characters]")
    }
  }

  private fun readZipEntries(zipFile: File): List<ZipArchiveEntryInfo> {
    val entries = mutableListOf<ZipArchiveEntryInfo>()
    ZipInputStream(FileInputStream(zipFile)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null && entries.size < 200) {
        entries +=
          ZipArchiveEntryInfo(
            name = entry.name,
            isDirectory = entry.isDirectory,
            sizeBytes = entry.size.coerceAtLeast(0L),
          )
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }
    return entries
  }
}
