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
  data class TextFile(val text: String) : ViewerContentState
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
          when (file.category) {
            FileCategory.IMAGE -> ViewerContentState.ImageFile(downloaded)
            FileCategory.PDF -> ViewerContentState.PdfFile(downloaded)
            FileCategory.SPREADSHEET -> parseSpreadsheet(downloaded)
            FileCategory.DOCUMENT -> {
              if (file.mimeType == "application/vnd.google-apps.document" ||
                file.mimeType == "application/vnd.google-apps.presentation"
              ) {
                ViewerContentState.PdfFile(downloaded)
              } else {
                val ext = downloaded.extension.lowercase()
                if (ext == "pdf") {
                  ViewerContentState.PdfFile(downloaded)
                } else {
                  val content = downloaded.readText(Charsets.UTF_8).take(250_000)
                  ViewerContentState.TextFile(content)
                }
              }
            }
            FileCategory.VIDEO, FileCategory.AUDIO -> ViewerContentState.VideoOrAudio(downloaded)
            FileCategory.ARCHIVE -> {
              val entries = readZipEntries(downloaded)
              ViewerContentState.Archive(downloaded, entries)
            }
            FileCategory.FOLDER, FileCategory.OTHER -> {
              if (downloaded.extension.lowercase() in setOf("txt", "md", "json", "xml", "log", "csv", "kt", "ts", "js")) {
                ViewerContentState.TextFile(downloaded.readText(Charsets.UTF_8).take(250_000))
              } else {
                ViewerContentState.Unsupported(downloaded)
              }
            }
          }
        }
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
    val allRows = mutableListOf<List<String>>()
    BufferedReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8)).use { reader ->
      var line = reader.readLine()
      var count = 0
      while (line != null && count < 500) {
        allRows += parseCsvLine(line, delimiter)
        line = reader.readLine()
        count++
      }
    }
    if (allRows.isEmpty()) {
      return ViewerContentState.Spreadsheet(
        file,
        SpreadsheetData(emptyList(), emptyList(), 0.0, 0.0, 0),
      )
    }
    val colCount = allRows.maxOf { it.size }.coerceAtLeast(1)
    val colLetters = (0 until colCount).map { idx ->
      val letter = ('A' + (idx % 26))
      if (idx < 26) "$letter" else "${'A' + (idx / 26 - 1)}$letter"
    }
    val paddedRows = allRows.map { row ->
      List(colCount) { idx -> row.getOrElse(idx) { "" } }
    }
    var sum = 0.0
    var maxVal = 0.0
    paddedRows.flatten().forEach { cell ->
      val num = cell.replace(",", "").replace("$", "").trim().toDoubleOrNull()
      if (num != null) {
        sum += num
        if (num > maxVal) maxVal = num
      }
    }
    return ViewerContentState.Spreadsheet(
      file = file,
      data = SpreadsheetData(
        headers = colLetters,
        rows = paddedRows,
        numericSum = sum,
        largestValue = maxVal,
        totalCells = paddedRows.size * colCount,
      ),
    )
  }

  private fun parseCsvLine(line: String, delimiter: Char): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    for (ch in line) {
      when {
        ch == '"' -> inQuotes = !inQuotes
        ch == delimiter && !inQuotes -> {
          result += current.toString().trim()
          current.clear()
        }
        else -> current.append(ch)
      }
    }
    result += current.toString().trim()
    return result
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
