package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.util.AppUpdateInfo

@Composable
fun AppUpdateDialog(
  updateInfo: AppUpdateInfo,
  downloadProgress: Float?,
  onDismiss: () -> Unit,
  onStartUpdate: () -> Unit,
) {
  val isDownloading = downloadProgress != null

  AlertDialog(
    onDismissRequest = {
      if (!isDownloading && !updateInfo.isMandatory) onDismiss()
    },
    icon = {
      Icon(
        imageVector = Icons.Filled.SystemUpdate,
        contentDescription = "Update available",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp),
      )
    },
    title = {
      Text(
        text = "Update Available",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.padding(bottom = 12.dp),
        ) {
          Text(
            text = "Version ${updateInfo.versionName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }

        Text(
          text = "What's new:",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = updateInfo.changeLog,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isDownloading) {
          Spacer(modifier = Modifier.height(16.dp))
          val percent = ((downloadProgress ?: 0f) * 100).toInt()
          Text(
            text = "Downloading update ($percent%)...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { downloadProgress ?: 0f },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onStartUpdate,
        enabled = !isDownloading,
      ) {
        Text(if (isDownloading) "Downloading..." else "Update Now")
      }
    },
    dismissButton = {
      if (!isDownloading && !updateInfo.isMandatory) {
        TextButton(onClick = onDismiss) {
          Text("Later")
        }
      }
    },
  )
}
