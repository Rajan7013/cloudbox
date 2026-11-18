package com.example.ui.viewer

import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ViewerContentTest {
  @Test fun searchUsesActualPageAndGlyphPositions() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    PDFBoxResourceLoader.init(context)
    val file = File.createTempFile("cloudbox-test", ".pdf")
    try {
      PDDocument().use { doc ->
        listOf("First page", "Second page second").forEach { text ->
          val page = PDPage()
          doc.addPage(page)
          PDPageContentStream(doc, page).use { stream ->
            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 20f)
            stream.newLineAtOffset(72f, 700f)
            stream.showText(text)
            stream.endText()
          }
        }
        doc.save(file)
      }
      val pages = PdfTextExtractor.index(context, file)
      assertTrue(pages.getValue(0).bounds("second").isEmpty())
      val hits = pages.getValue(1).bounds("second")
      assertEquals(2, hits.size)
      assertTrue(hits[0].right < hits[1].left)
      assertTrue(hits.all { RectF(0f, 0f, 1f, 1f).contains(it) })
    } finally { file.delete() }
  }

  @Test fun sparseWorkbookRetainsCellAddressesAndActualSheetName() {
    val file = File.createTempFile("cloudbox-test", ".xlsx")
    try {
      ZipOutputStream(file.outputStream()).use { zip ->
        fun part(path: String, xml: String) {
          zip.putNextEntry(ZipEntry(path))
          zip.write(xml.toByteArray())
          zip.closeEntry()
        }
        part("xl/workbook.xml", """<workbook xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheet name="Ledger" r:id="r2"/></workbook>""")
        part("xl/_rels/workbook.xml.rels", """<Relationships><Relationship Id="r2" Target="worksheets/sheet2.xml"/></Relationships>""")
        part("xl/worksheets/sheet2.xml", """<worksheet><row r="2"><c r="C2"><v>-5</v></c></row></worksheet>""")
      }
      val (name, sheet) = OfficePreview(file).use { it.workbook().single() }
      assertEquals("Ledger", name)
      assertEquals("-5", sheet.rows[1][2])
      assertEquals(-5.0, sheet.largestValue, 0.0)
    } finally { file.delete() }
  }
}
