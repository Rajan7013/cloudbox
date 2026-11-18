package com.example.ui.viewer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.CloudFile
import com.example.model.OfflineSyncStatus

@Composable
internal fun FolderMediaDialog(file: CloudFile, siblings: List<CloudFile>, select: (CloudFile) -> Unit, dismiss: () -> Unit) {
  var query by remember { mutableStateOf("") }
  var sort by remember { mutableStateOf("Name") }
  var descending by remember { mutableStateOf(false) }
  var starred by remember { mutableStateOf(false) }
  var offline by remember { mutableStateOf(false) }
  val files = remember(siblings, file, query, sort, descending, starred, offline) {
    val filtered = (siblings + file).distinctBy { it.id }.filter {
      it.name.contains(query, true) && (!starred || it.starred) && (!offline || it.offlineStatus == OfflineSyncStatus.SAVED)
    }
    val sorted = when (sort) {
      "Size" -> filtered.sortedBy { it.sizeBytes }
      "Modified" -> filtered.sortedBy { it.modifiedTimeEpochMs }
      else -> filtered.sortedBy { it.name.lowercase() }
    }
    if (descending) sorted.asReversed() else sorted
  }
  AlertDialog(onDismissRequest = dismiss, title = { Text("${file.category.label} files in this folder") },
    text = { Column {
      OutlinedTextField(query, { query = it }, label = { Text("Find by name") }, singleLine = true)
      Row(Modifier.horizontalScroll(rememberScrollState())) {
        listOf("Name", "Size", "Modified").forEach { key ->
          FilterChip(selected = sort == key, onClick = { if (key == sort) descending = !descending else { sort = key; descending = false } },
            label = { Text(key + if (key == sort) (if (descending) " ↓" else " ↑") else "") })
        }
      }
      Row {
        FilterChip(selected = starred, onClick = { starred = !starred }, label = { Text("Starred") })
        FilterChip(selected = offline, onClick = { offline = !offline }, label = { Text("Offline") })
      }
      if (files.isEmpty()) Text("No matching files")
      LazyColumn(Modifier.heightIn(max = 320.dp)) {
        items(files, key = { it.id }) { item ->
          TextButton(onClick = { dismiss(); select(item) }) { Text(if (item.id == file.id) "• ${item.name}" else item.name) }
        }
      }
    } }, confirmButton = { TextButton(onClick = dismiss) { Text("Close") } })
}
