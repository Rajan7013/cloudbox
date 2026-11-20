package com.example.ui.viewer

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalCloudBoxColors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerSettingsDialog(
  initialKind: ReaderKind? = null,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val prefs = remember(context) { context.getSharedPreferences("viewer_defaults", Context.MODE_PRIVATE) }
  val extra = LocalCloudBoxColors.current

  var selectedKind by remember { mutableStateOf(initialKind ?: ReaderKind.PDF) }
  var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", true)) }
  var rememberPosition by remember { mutableStateOf(prefs.getBoolean("remember_position", true)) }
  var saveDestination by remember { mutableStateOf(prefs.getString("save_destination", "Gallery > CloudBox") ?: "Gallery > CloudBox") }

  // Measure cache size
  var cacheSizeBytes by remember {
    mutableLongStateOf(
      File(context.cacheDir, "viewer_cache").walkTopDown().filter { it.isFile }.sumOf { it.length() }
    )
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 10.dp)
        .padding(bottom = 28.dp)
        .verticalScroll(rememberScrollState()),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text("Viewer Settings", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = {
          prefs.edit().clear().apply()
          keepScreenOn = true
          rememberPosition = true
          saveDestination = "Gallery > CloudBox"
        }) {
          Text("Restore all defaults", style = MaterialTheme.typography.labelSmall)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Section 1: General
      Text("GENERAL", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
      Spacer(modifier = Modifier.height(6.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Keep screen on while viewing", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = keepScreenOn,
              onCheckedChange = {
                keepScreenOn = it
                prefs.edit().putBoolean("keep_screen_on", it).apply()
              },
            )
          }
          HorizontalDivider(color = extra.border.copy(alpha = 0.5f))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Remember last reading position", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = rememberPosition,
              onCheckedChange = {
                rememberPosition = it
                prefs.edit().putBoolean("remember_position", it).apply()
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Section 2: Per-Viewer Settings Tabs
      Text("PER VIEWER PREFERENCES", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        ReaderKind.entries.forEach { k ->
          FilterChip(
            selected = selectedKind == k,
            onClick = { selectedKind = k },
            label = { Text(k.label) },
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text("${selectedKind.label} · ${selectedKind.setting}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(8.dp))
          val curChoice = prefs.getString(selectedKind.name, null) ?: selectedKind.choices.first()
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedKind.choices.forEach { opt ->
              FilterChip(
                selected = opt == curChoice,
                onClick = { prefs.edit().putString(selectedKind.name, opt).apply() },
                label = { Text(opt) },
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Section 3: Storage & Cache
      Text("STORAGE & EXPORTS", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
      Spacer(modifier = Modifier.height(6.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text("Viewer Cache", style = MaterialTheme.typography.bodyMedium)
              Text("${(cacheSizeBytes / (1024 * 1024)).coerceAtLeast(1)} MB", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
            }
            OutlinedButton(
              onClick = {
                File(context.cacheDir, "viewer_cache").deleteRecursively()
                cacheSizeBytes = 0L
              },
            ) {
              Text("Clear Cache")
            }
          }
          HorizontalDivider(color = extra.border.copy(alpha = 0.5f))
          Column {
            Text("Save exported images / slides to:", style = MaterialTheme.typography.bodySmall, color = extra.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Pictures/CloudBox (Device Gallery)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}
