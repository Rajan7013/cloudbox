package com.example.ui.viewer

import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Reads the content parts of OOXML packages without evaluating fields, macros or external targets. */
internal class OfficePreview(file: File) : java.io.Closeable {
  private val zip = ZipFile(file)
  private val factory = DocumentBuilderFactory.newInstance().apply {
    isNamespaceAware = true
  }
  private var budget = 24_000_000L
  private fun root(path: String): Element {
    val entry = requireNotNull(zip.getEntry(path)) { "Missing document part" }
    require(entry.size in 0..minOf(budget, 8_000_000L)) { "Document exceeds the local preview limit" }
    budget -= entry.size
    // Reject DTDs before parsing. Office content parts use UTF-8 or UTF-16 XML.
    val bytes = zip.getInputStream(entry).use { it.readBytes() }
    require(!bytes.toString(Charsets.ISO_8859_1).replace("\u0000", "").contains("<!DOCTYPE", true))
    return factory.newDocumentBuilder().apply {
      setEntityResolver { _, _ -> throw org.xml.sax.SAXException("External entities are unsupported") }
    }.parse(bytes.inputStream()).documentElement
  }
  private fun Element.elements(tag: String): List<Element> {
    val nodes = getElementsByTagNameNS("*", tag)
    return (0 until nodes.length).map { nodes.item(it) as Element }
  }
  private fun paragraphs(path: String) = root(path).elements("p").joinToString("\n") { p ->
    p.elements("t").joinToString("") { it.textContent }
  }
  private fun parts(directory: String, main: String, tag: String): List<Pair<Element,String>> {
    val relationships = root("$directory/_rels/$main.rels").elements("Relationship")
      .filter { it.getAttribute("TargetMode") != "External" }.associateBy { it.getAttribute("Id") }
    return root("$directory/$main").elements(tag).map { element ->
      val id = element.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
      val target = requireNotNull(relationships[id]).getAttribute("Target")
      val path = java.net.URI("/$directory/$main").resolve(target).normalize().path.trimStart('/')
      element to path
    }
  }
  fun document() = paragraphs("word/document.xml")
  fun slides() = parts("ppt", "presentation.xml", "sldId").map { paragraphs(it.second) }
  fun workbook(): List<Pair<String, SpreadsheetData>> {
    val strings = if (zip.getEntry("xl/sharedStrings.xml") == null) emptyList() else
      root("xl/sharedStrings.xml").elements("si").map { si -> si.elements("t").joinToString("") { it.textContent } }
    return parts("xl", "workbook.xml", "sheet").map { (sheet, path) ->
      val rows = mutableListOf<MutableList<String>>()
      for (cell in root(path).elements("c")) {
        val address = cell.getAttribute("r")
        val letters = address.takeWhile { it in 'A'..'Z' }
        val r = address.drop(letters.length).toIntOrNull() ?: continue
        val col = letters.fold(0) { n, c -> n * 26 + (c - 'A' + 1) }
        if (r !in 1..500 || col !in 1..128) continue
        while (rows.size < r) rows.add(mutableListOf())
        val row = rows[r - 1]
        while (row.size < col) row.add("")
        val value = cell.elements("v").firstOrNull()?.textContent.orEmpty()
        row[col - 1] = when (cell.getAttribute("t")) {
          "s" -> value.toIntOrNull()?.let(strings::getOrNull).orEmpty()
          "inlineStr" -> cell.elements("t").joinToString("") { it.textContent }
          "b" -> if (value == "1") "TRUE" else "FALSE"
          else -> value
        }
      }
      sheet.getAttribute("name") to spreadsheetData(rows)
    }
  }
  fun openDocument(extension: String): ViewerContentState {
    val content = root("content.xml")
    fun text(element: Element) = element.elements("p").joinToString("\n") { it.textContent }
    return when (extension) {
      "odt" -> ViewerContentState.TextFile(text(content))
      "odp" -> ViewerContentState.Slides(content.elements("page").map(::text))
      else -> {
        val tableNs = "urn:oasis:names:tc:opendocument:xmlns:table:1.0"
        val sheets = content.elements("table").map { table ->
          val rows = mutableListOf<List<String>>()
          for (row in table.elements("table-row")) {
            if (rows.size >= 500) break
            val cells = mutableListOf<String>()
            for (cell in row.elements("table-cell")) {
              if (cells.size >= 128) break
              val repeat = cell.getAttributeNS(tableNs, "number-columns-repeated").toIntOrNull() ?: 1
              val value = text(cell)
              repeat(repeat.coerceIn(0, 128 - cells.size)) { cells.add(value) }
            }
            val repeat = row.getAttributeNS(tableNs, "number-rows-repeated").toIntOrNull() ?: 1
            repeat(repeat.coerceIn(0, 500 - rows.size)) { rows.add(cells) }
          }
          table.getAttributeNS(tableNs, "name") to spreadsheetData(rows)
        }
        ViewerContentState.Workbook(sheets)
      }
    }
  }
  override fun close() = zip.close()
}

internal fun spreadsheetData(rows: List<List<String>>): SpreadsheetData {
  val width = rows.maxOfOrNull { it.size } ?: 0
  val headers = (1..width).map { index ->
    var n = index
    buildString { while (n > 0) { insert(0, 'A' + (n - 1) % 26); n = (n - 1) / 26 } }
  }
  val values = rows.flatten().mapNotNull { it.toDoubleOrNull() }
  return SpreadsheetData(headers, rows.map { it + List(width - it.size) { "" } },
    values.sum(), values.maxOrNull() ?: 0.0, rows.sumOf { it.size })
}
