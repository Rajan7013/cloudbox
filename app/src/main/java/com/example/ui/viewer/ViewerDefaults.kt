package com.example.ui.viewer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.model.CloudFile
import com.example.model.FileCategory

internal enum class ReaderKind(val label: String, val setting: String, val choices: List<String>) {
  PDF("PDF", "Page direction", listOf("Vertical", "Horizontal")),
  DOCS("Word and Docs", "Reading theme", listOf("Light", "Sepia", "Dark")),
  SLIDES("Slides", "Reading theme", listOf("Light", "Sepia", "Dark")),
  SHEETS("Sheets", "Opening view", listOf("Grid", "Summary")),
  MARKDOWN("Markdown", "Opening view", listOf("Rendered", "Source")),
  TEXT("Text", "Font size", listOf("100%", "120%", "140%", "160%")),
  IMAGE("Image", "Image fit", listOf("Fit", "Fill")),
  VIDEO("Video", "Skip interval", listOf("10 s", "15 s", "30 s")),
  AUDIO("Audio", "Skip interval", listOf("15 s", "10 s", "30 s"));

  companion object {
    fun forFile(file: CloudFile): ReaderKind = when {
      file.mimeType.endsWith(".presentation") || file.name.substringAfterLast('.').lowercase() in setOf("pptx", "odp") -> SLIDES
      file.mimeType.endsWith(".document") || file.name.substringAfterLast('.').lowercase() in setOf("docx", "odt") -> DOCS
      file.name.substringAfterLast('.').lowercase() in setOf("md", "markdown") -> MARKDOWN
      else -> when (file.category) {
        FileCategory.PDF -> PDF
        FileCategory.SPREADSHEET -> SHEETS
        FileCategory.VIDEO -> VIDEO
        FileCategory.AUDIO -> AUDIO
        FileCategory.IMAGE -> IMAGE
        else -> TEXT
      }
    }
  }
}

@Composable
internal fun readerDefault(kind: ReaderKind): String {
  val context = LocalContext.current
  val prefs = remember(context) { context.getSharedPreferences("viewer_defaults", 0) }
  var choice by remember(kind) { mutableStateOf(prefs.getString(kind.name, null) ?: kind.choices.first()) }
  DisposableEffect(prefs, kind) {
    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == kind.name || key == null) choice = prefs.getString(kind.name, null) ?: kind.choices.first()
    }
    prefs.registerOnSharedPreferenceChangeListener(listener)
    onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
  }
  return choice
}

@Composable
internal fun ViewerDefaultsDialog(initial: ReaderKind?, onClose: () -> Unit) {
  val prefs = LocalContext.current.getSharedPreferences("viewer_defaults", 0)
  var kind by remember { mutableStateOf(initial ?: ReaderKind.PDF) }
  val choice = readerDefault(kind)
  AlertDialog(onDismissRequest = onClose, title = { Text("Viewer settings") },
    text = { Column {
      if (initial == null) Row(Modifier.horizontalScroll(rememberScrollState())) {
        ReaderKind.entries.forEach { item ->
          FilterChip(selected = item == kind, onClick = { kind = item }, label = { Text(item.label) })
        }
      }
      Text("${kind.label} · ${kind.setting}")
      kind.choices.forEach { value ->
        FilterChip(selected = value == choice, onClick = { prefs.edit().putString(kind.name, value).apply() }, label = { Text(value) })
      }
    } },
    dismissButton = { TextButton(onClick = { prefs.edit().remove(kind.name).apply() }) { Text("Restore default") } },
    confirmButton = { TextButton(onClick = onClose) { Text("Done") } })
}
