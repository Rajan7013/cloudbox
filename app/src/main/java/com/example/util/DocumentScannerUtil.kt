package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentScannerUtil {
  private const val TAG = "DocumentScannerUtil"

  fun createTempCameraImageUri(context: Context): Pair<Uri, File> {
    val dir = File(context.cacheDir, "captured_photos").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return uri to file
  }

  /**
   * Processes a captured document photo into an enhanced high-contrast grayscale PDF page
   * using Android's native PdfDocument API, ready to be uploaded directly to Drive.
   */
  fun convertImageToScannedPdf(context: Context, imageUri: Uri): File? {
    return try {
      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(imageUri)?.use {
        BitmapFactory.decodeStream(it, null, boundsOptions)
      }
      val maxDim = 1800
      var sampleSize = 1
      while (boundsOptions.outWidth / sampleSize > maxDim || boundsOptions.outHeight / sampleSize > maxDim) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val rawBitmap =
        context.contentResolver.openInputStream(imageUri)?.use {
          BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

      // Enhance contrast & crispness for document legibility
      val enhanced = Bitmap.createBitmap(rawBitmap.width, rawBitmap.height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(enhanced)
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      val contrast = 1.35f
      val brightness = -20f
      val matrix =
        ColorMatrix().apply {
          setSaturation(0.15f)
          postConcat(
            ColorMatrix(
              floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f,
              )
            )
          )
        }
      paint.colorFilter = ColorMatrixColorFilter(matrix)
      canvas.drawBitmap(rawBitmap, Matrix(), paint)
      if (enhanced != rawBitmap) rawBitmap.recycle()

      // Fit to A4-proportioned PDF page (595 x 842 pt at 72 dpi, scaled 2x for crispness)
      val pageWidth = 1190
      val pageHeight = 1684
      val margin = 48f
      val scale =
        minOf(
          (pageWidth - margin * 2f) / enhanced.width.toFloat(),
          (pageHeight - margin * 2f) / enhanced.height.toFloat(),
        )
      val drawnWidth = enhanced.width * scale
      val drawnHeight = enhanced.height * scale
      val left = (pageWidth - drawnWidth) / 2f
      val top = (pageHeight - drawnHeight) / 2f

      val pdfDocument = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
      val page = pdfDocument.startPage(pageInfo)
      val pdfMatrix = Matrix().apply {
        postScale(scale, scale)
        postTranslate(left, top)
      }
      page.canvas.drawColor(android.graphics.Color.WHITE)
      page.canvas.drawBitmap(enhanced, pdfMatrix, Paint(Paint.FILTER_BITMAP_FLAG))
      pdfDocument.finishPage(page)
      enhanced.recycle()

      val outDir = File(context.cacheDir, "viewer_cache").apply { mkdirs() }
      val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
      val pdfFile = File(outDir, "Scan_$stamp.pdf")
      FileOutputStream(pdfFile).use { out -> pdfDocument.writeTo(out) }
      pdfDocument.close()
      pdfFile
    } catch (e: Exception) {
      Log.e(TAG, "Failed to create scanned PDF", e)
      null
    }
  }
}
