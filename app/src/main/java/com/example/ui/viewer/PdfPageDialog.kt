package com.example.ui.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfPageDialog(file: File, name: String, index: Int, count: Int, bookmarks: List<Int>, canExport: Boolean,
                  onJump: (Int) -> Unit, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var number by rememberSaveable { mutableStateOf((index + 1).toString()) }
  var marksOnly by remember { mutableStateOf(false) }
  val page = number.toIntOrNull()?.takeIf { it in 1..count }
  var pending by rememberSaveable { mutableIntStateOf(index) }
  var busy by remember { mutableStateOf(false) }
  var status by remember { mutableStateOf("") }
  fun save(target: Uri?, selected: Int) {
    busy = true
    scope.launch {
      status = try {
        withContext(Dispatchers.IO) { exportPage(context, file, selected, name, target) }
        if (target == null) "Saved to Pictures/CloudBox" else "Page image saved"
      } catch (e: Exception) { "Could not save page image" }
      busy = false
    }
  }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) {
    if (it != null) save(it, pending)
  }
  AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Go to page or extract") },
    text = {
      androidx.compose.foundation.layout.Column {
        OutlinedTextField(number, { number = it }, singleLine = true, label = { Text("Page (1–$count)") },
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
        FilterChip(selected = marksOnly, onClick = { marksOnly = !marksOnly }, label = { Text("Bookmarked pages") })
        val choices = if (marksOnly) bookmarks.sorted().filter { it in 0 until count } else (0 until count).toList()
        if (choices.isEmpty()) Text("No bookmarked pages")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(choices, key = { it }) { i ->
            val bitmap by produceState<Bitmap?>(null, file, i) {
              value = withContext(Dispatchers.IO) {
                try {
                  ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer -> renderer.openPage(i).use { p ->
                      val scale = 160f / maxOf(p.width, p.height)
                      Bitmap.createBitmap((p.width * scale).toInt().coerceAtLeast(1), (p.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.WHITE)
                        p.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                      }
                    } }
                  }
                } catch (_: java.io.IOException) { null }
              }
            }
            Column(Modifier.width(74.dp).clickable(enabled = !busy) { number = (i + 1).toString() }) {
              bitmap?.let { Image(it.asImageBitmap(), "Page ${i + 1}", Modifier.size(72.dp)) }
              Text(if (page == i + 1) "• ${i + 1}" else "${i + 1}")
            }
          }
        }
        if (canExport) {
          TextButton(enabled = page != null && !busy, onClick = {
            pending = page!! - 1
            picker.launch("${name.substringBeforeLast('.')}-page-$page.png")
          }) { Text("Save original page as PNG…") }
          if (Build.VERSION.SDK_INT >= 29) TextButton(enabled = page != null && !busy,
            onClick = { save(null, page!! - 1) }) { Text("Save page to gallery") }
        }
        if (busy) LinearProgressIndicator()
        if (status.isNotEmpty()) Text(status)
      }
    }, confirmButton = { TextButton(enabled = page != null && !busy, onClick = { onJump(page!! - 1); onDismiss() }) { Text("Go") } },
    dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Close") } })
}

private fun exportPage(context: Context, file: File, index: Int, name: String, target: Uri?) {
  val resolver = context.contentResolver
  ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
    PdfRenderer(fd).use { renderer ->
      renderer.openPage(index).use { page ->
        val scale = minOf(2f, 3200f / maxOf(page.width, page.height))
        val image = Bitmap.createBitmap((page.width * scale).toInt().coerceAtLeast(1),
          (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        try {
          image.eraseColor(Color.WHITE)
          page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          val uri = target ?: checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${name.substringBeforeLast('.')}-page-${index + 1}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CloudBox")
            put(MediaStore.Images.Media.IS_PENDING, 1)
          }))
          try {
            checkNotNull(resolver.openOutputStream(uri, "wt")).use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            if (target == null) resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
          } catch (e: Exception) {
            if (target == null) resolver.delete(uri, null, null)
            throw e
          }
        } finally { image.recycle() }
      }
    }
  }
}
