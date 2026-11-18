package com.example.ui.viewer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.InflaterInputStream

object PdfTextExtractor {
  /**
   * Extracts text strings from PDF content streams.
   * Returns a map of 0-based page index to extracted text.
   */
  fun extractTextByPage(pdfFile: File, totalPages: Int): Map<Int, String> {
    if (totalPages <= 0) return emptyMap()
    return try {
      val bytes = pdfFile.readBytes()
      val decompressedStreams = extractStreams(bytes)
      val pageTexts = mutableMapOf<Int, String>()

      if (decompressedStreams.isNotEmpty()) {
        val streamsPerPage = (decompressedStreams.size / totalPages.coerceAtLeast(1)).coerceAtLeast(1)
        for (page in 0 until totalPages) {
          val startIdx = (page * streamsPerPage).coerceAtMost(decompressedStreams.lastIndex)
          val endIdx = ((page + 1) * streamsPerPage).coerceAtMost(decompressedStreams.size)
          val combined = StringBuilder()
          for (i in startIdx until endIdx) {
            val text = parsePdfStreamText(decompressedStreams[i])
            if (text.isNotBlank()) {
              if (combined.isNotEmpty()) combined.append("\n")
              combined.append(text)
            }
          }
          val pageStr = combined.toString().trim()
          if (pageStr.isNotEmpty()) {
            pageTexts[page] = pageStr
          }
        }
      }
      pageTexts
    } catch (_: Exception) {
      emptyMap()
    }
  }

  private fun extractStreams(pdfBytes: ByteArray): List<ByteArray> {
    val streams = mutableListOf<ByteArray>()
    var index = 0
    val streamKeyword = "stream".toByteArray(Charsets.ISO_8859_1)
    val endstreamKeyword = "endstream".toByteArray(Charsets.ISO_8859_1)

    while (index < pdfBytes.size) {
      val streamStart = indexOf(pdfBytes, streamKeyword, index)
      if (streamStart == -1) break

      var contentStart = streamStart + 6
      if (contentStart < pdfBytes.size && pdfBytes[contentStart] == '\r'.code.toByte()) contentStart++
      if (contentStart < pdfBytes.size && pdfBytes[contentStart] == '\n'.code.toByte()) contentStart++

      val streamEnd = indexOf(pdfBytes, endstreamKeyword, contentStart)
      if (streamEnd == -1) break

      if (streamEnd > contentStart) {
        val rawStream = pdfBytes.copyOfRange(contentStart, streamEnd)
        val decompressed = tryDecompress(rawStream) ?: rawStream
        streams.add(decompressed)
      }

      index = streamEnd + 9
    }
    return streams
  }

  private fun tryDecompress(data: ByteArray): ByteArray? {
    return try {
      val inflater = InflaterInputStream(ByteArrayInputStream(data))
      val bos = ByteArrayOutputStream()
      val buf = ByteArray(4096)
      var read: Int
      while (inflater.read(buf).also { read = it } != -1) {
        bos.write(buf, 0, read)
      }
      bos.toByteArray()
    } catch (_: Exception) {
      null
    }
  }

  private fun parsePdfStreamText(streamBytes: ByteArray): String {
    val text = StringBuilder()
    val str = String(streamBytes, Charsets.ISO_8859_1)

    // Look for BT ... ET text blocks
    val btPattern = Regex("""BT\s+(.*?)\s+ET""", RegexOption.DOT_MATCHES_ALL)
    for (match in btPattern.findAll(str)) {
      val block = match.groupValues[1]

      // Match (literal) Tj
      val tjPattern = Regex("""\((.*?)\)\s*Tj""")
      for (tj in tjPattern.findAll(block)) {
        val content = unescapePdfString(tj.groupValues[1])
        if (content.isNotBlank()) text.append(content).append(" ")
      }

      // Match [(array)] TJ
      val arrayTjPattern = Regex("""\[(.*?)\]\s*TJ""")
      for (atj in arrayTjPattern.findAll(block)) {
        val inner = atj.groupValues[1]
        val partsPattern = Regex("""\((.*?)\)""")
        for (part in partsPattern.findAll(inner)) {
          text.append(unescapePdfString(part.groupValues[1]))
        }
        text.append(" ")
      }
      text.append("\n")
    }

    return text.toString().replace(Regex("""[ \t]+"""), " ").trim()
  }

  private fun unescapePdfString(s: String): String {
    return s.replace("\\n", "\n")
      .replace("\\r", "\r")
      .replace("\\t", "\t")
      .replace("\\(", "(")
      .replace("\\)", ")")
      .replace("\\\\", "\\")
  }

  private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
    if (fromIndex >= source.size) return -1
    val limit = source.size - target.size
    for (i in fromIndex..limit) {
      var match = true
      for (j in target.indices) {
        if (source[i + j] != target[j]) {
          match = false
          break
        }
      }
      if (match) return i
    }
    return -1
  }
}
