package com.example.ui.viewer

import android.content.Context
import android.graphics.RectF
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import java.io.Writer

data class IndexedPage(val text: String, private val boxes: List<RectF?>) {
  fun bounds(query: String): List<RectF> = buildList {
    if (query.isBlank()) return@buildList
    var start = text.indexOf(query, ignoreCase = true)
    while (start >= 0) {
      val rect = RectF()
      for (i in start until start + query.length) boxes[i]?.let(rect::union)
      if (!rect.isEmpty) add(rect)
      start = text.indexOf(query, start + query.length, ignoreCase = true)
    }
  }
}

object PdfTextExtractor {
  fun index(context: Context, file: File): Map<Int, IndexedPage> {
    PDFBoxResourceLoader.init(context.applicationContext)
    return PDDocument.load(file).use { document ->
      val pages = mutableMapOf<Int, IndexedPage>()
      val stripper = object : PDFTextStripper() {
        val lineText = StringBuilder()
        val boxes = mutableListOf<RectF?>()
        override fun writeString(text: String, positions: MutableList<TextPosition>) {
          for (pos in positions) {
            val rotated = pos.rotation % 180 != 0
            val w = if (rotated) pos.pageHeight else pos.pageWidth
            val h = if (rotated) pos.pageWidth else pos.pageHeight
            val box = RectF(pos.x / w, (pos.y - pos.height) / h, (pos.x + pos.width) / w, pos.y / h)
            val chars = pos.unicode ?: continue
            lineText.append(chars)
            repeat(chars.length) { boxes.add(box) }
          }
        }
        override fun writeWordSeparator() { lineText.append(' '); boxes.add(null) }
        override fun writeLineSeparator() { lineText.append('\n'); boxes.add(null) }
        override fun endPage(page: PDPage) {
          pages[currentPageNo - 1] = IndexedPage(lineText.toString(), boxes.toList())
          lineText.clear()
          boxes.clear()
          super.endPage(page)
        }
      }
      stripper.sortByPosition = true
      stripper.writeText(document, object : Writer() {
        override fun write(chars: CharArray, offset: Int, length: Int) {}
        override fun flush() {}
        override fun close() {}
      })
      pages
    }
  }
}
