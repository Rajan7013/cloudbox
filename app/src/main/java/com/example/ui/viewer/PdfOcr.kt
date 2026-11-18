package com.example.ui.viewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await

internal suspend fun recognizePdf(file: File, missing: Set<Int>, onPage: suspend (Int, IndexedPage) -> Unit) {
  val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
  try {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
      PdfRenderer(fd).use { renderer ->
        for (i in missing.sorted()) {
          currentCoroutineContext().ensureActive()
          val bitmap = renderer.openPage(i).use { page ->
            val scale = 1800f / maxOf(page.width, page.height)
            Bitmap.createBitmap((page.width * scale).toInt().coerceAtLeast(1),
              (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).apply {
              eraseColor(Color.WHITE)
              page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
          }
          val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
          val text = StringBuilder()
          val boxes = mutableListOf<RectF?>()
          for (block in result.textBlocks) for (line in block.lines) {
            for (word in line.elements) {
              val bounds = word.boundingBox ?: continue
              val rect = RectF(bounds.left.toFloat() / bitmap.width, bounds.top.toFloat() / bitmap.height,
                bounds.right.toFloat() / bitmap.width, bounds.bottom.toFloat() / bitmap.height)
              text.append(word.text)
              repeat(word.text.length) { boxes.add(rect) }
              text.append(' ')
              boxes.add(null)
            }
            text.append('\n')
            boxes.add(null)
          }
          bitmap.recycle()
          onPage(i, IndexedPage(text.toString(), boxes))
        }
      }
    }
  } finally { recognizer.close() }
}
