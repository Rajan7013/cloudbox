package com.example.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccessRole
import com.example.model.CloudFile
import com.example.model.ReaderTheme
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils
import java.io.File
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFileViewerPane(
  file: CloudFile,
  textFile: File,
  initialContent: String,
  isLog: Boolean = false,
  isJson: Boolean = false,
  onBack: () -> Unit,
  onShare: () -> Unit,
  onDownload: () -> Unit,
  onOpenMoreActions: () -> Unit,
  onSettings: () -> Unit,
  onShowToast: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val extra = LocalCloudBoxColors.current
  val canEdit = file.ownedByMe || file.userRole == AccessRole.EDITOR

  var currentContent by remember(initialContent) { mutableStateOf(initialContent) }
  var isEditing by remember { mutableStateOf(false) }

  // Theme & Font
  var readerTheme by remember { mutableStateOf(if (isLog || isJson) ReaderTheme.DARK else ReaderTheme.LIGHT) }
  var fontSizeSp by remember { mutableIntStateOf(13) }
  var showLineNumbers by remember { mutableStateOf(true) }
  var wordWrap by remember { mutableStateOf(!isLog && !isJson) }
  var activeEncoding by remember { mutableStateOf("UTF-8") }

  // Search
  var showFind by remember { mutableStateOf(false) }
  var findQuery by remember { mutableStateOf("") }
  var matchCase by remember { mutableStateOf(false) }
  var wholeWord by remember { mutableStateOf(false) }
  var useRegex by remember { mutableStateOf(false) }
  var currentMatchIndex by remember { mutableIntStateOf(0) }

  // Go to line
  var showGoToLine by remember { mutableStateOf(false) }
  var targetLineNumber by remember { mutableStateOf("") }
  var flashedLineIndex by remember { mutableIntStateOf(-1) }

  // Log Mode state
  var selectedLogLevel by remember { mutableStateOf("All") } // "All", "Error", "Warn", "Info", "Debug"
  var isTailing by remember { mutableStateOf(false) }

  // JSON Mode state
  var isPrettyJson by remember { mutableStateOf(true) }
  val jsonValidationStatus = remember(currentContent, isJson) {
    if (!isJson) ""
    else {
      try {
        val trimmed = currentContent.trim()
        if (trimmed.startsWith("{")) JSONObject(trimmed)
        else JSONArray(trimmed)
        "✓ Valid JSON"
      } catch (e: Exception) {
        "✗ Invalid JSON: ${e.localizedMessage?.take(30)}"
      }
    }
  }

  // Pre-process formatted content
  val displayContent = remember(currentContent, isJson, isPrettyJson) {
    if (isJson && isPrettyJson) {
      try {
        val trimmed = currentContent.trim()
        if (trimmed.startsWith("{")) JSONObject(trimmed).toString(2)
        else if (trimmed.startsWith("[")) JSONArray(trimmed).toString(2)
        else currentContent
      } catch (_: Exception) {
        currentContent
      }
    } else currentContent
  }

  val lines = remember(displayContent) { displayContent.lines() }
  val listState = rememberLazyListState()

  // Filter lines if log mode
  val filteredLineIndices = remember(lines, isLog, selectedLogLevel) {
    if (!isLog || selectedLogLevel == "All") lines.indices.toList()
    else {
      val targetTag = selectedLogLevel.uppercase()
      lines.indices.filter { idx ->
        lines[idx].contains(targetTag, ignoreCase = true)
      }
    }
  }

  // Tail / follow ticker
  LaunchedEffect(isTailing) {
    if (isTailing && filteredLineIndices.isNotEmpty()) {
      while (isTailing) {
        listState.scrollToItem((filteredLineIndices.size - 1).coerceAtLeast(0))
        delay(1500L)
      }
    }
  }

  // Go to line jump
  LaunchedEffect(flashedLineIndex) {
    if (flashedLineIndex >= 0) {
      listState.animateScrollToItem(flashedLineIndex)
      delay(2000L)
      flashedLineIndex = -1
    }
  }

  // Find matches
  val matches = remember(displayContent, findQuery, matchCase, wholeWord, useRegex) {
    val q = findQuery.trim()
    if (q.isEmpty()) emptyList()
    else {
      try {
        val pattern = if (useRegex) q else (if (wholeWord) "\\b${Regex.escape(q)}\\b" else Regex.escape(q))
        val flags = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
        Regex(pattern, flags).findAll(displayContent).map { it.range }.toList()
      } catch (_: Exception) {
        emptyList()
      }
    }
  }

  val paperBg = when (readerTheme) {
    ReaderTheme.LIGHT -> Color.White
    ReaderTheme.SEPIA -> extra.sepiaPaperBg
    ReaderTheme.DARK -> Color(0xFF0D1117)
  }
  val defaultTextColor = when (readerTheme) {
    ReaderTheme.LIGHT -> Color(0xFF1E2138)
    ReaderTheme.SEPIA -> extra.sepiaPaperText
    ReaderTheme.DARK -> Color(0xFFC9D1D9)
  }

  fun saveContentChanges(newContent: String) {
    currentContent = newContent
    try {
      textFile.writeText(newContent)
    } catch (e: Exception) {
      onShowToast("Failed to save: ${e.localizedMessage}")
    }
  }

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Top Bar (Common shell frame)
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = file.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "$activeEncoding · ${FormatUtils.formatBytes(file.sizeBytes)} · ${lines.size} lines · ${file.userRole.label}",
          style = MaterialTheme.typography.bodySmall,
          color = extra.textSecondary,
        )
      }
      IconButton(onClick = { showFind = !showFind }) {
        Icon(Icons.Filled.Search, contentDescription = "Search", tint = if (showFind) MaterialTheme.colorScheme.primary else LocalContentColor.current)
      }
      if (file.canShare) {
        IconButton(onClick = onShare) { Icon(Icons.Filled.Share, contentDescription = "Share") }
      }
      IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
      IconButton(onClick = onOpenMoreActions) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
    }

    // JSON Validation Header / Log Filter Chips Header
    if (isJson) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(jsonValidationStatus, style = MaterialTheme.typography.labelSmall, color = if (jsonValidationStatus.startsWith("✓")) extra.success else MaterialTheme.colorScheme.error)
          FilterChip(
            selected = isPrettyJson,
            onClick = { isPrettyJson = !isPrettyJson },
            label = { Text("Pretty Print { }") },
          )
        }
      }
    } else if (isLog) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf("All", "Error", "Warn", "Info", "Debug").forEach { lvl ->
          FilterChip(
            selected = selectedLogLevel == lvl,
            onClick = { selectedLogLevel = lvl },
            label = { Text(lvl) },
          )
        }
        FilterChip(
          selected = isTailing,
          onClick = { isTailing = !isTailing },
          label = { Text(if (isTailing) "Following ●" else "Tail") },
        )
      }
    }

    // Search bar if active
    AnimatedVisibility(visible = showFind) {
      Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = findQuery,
            onValueChange = { findQuery = it; currentMatchIndex = 0 },
            placeholder = { Text("Find…") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (matches.isEmpty()) "0" else "${currentMatchIndex + 1}/${matches.size}",
            style = MaterialTheme.typography.bodySmall,
          )
          IconButton(
            onClick = {
              if (matches.isNotEmpty()) currentMatchIndex = (currentMatchIndex - 1 + matches.size) % matches.size
            },
            enabled = matches.isNotEmpty(),
          ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
          }
          IconButton(
            onClick = {
              if (matches.isNotEmpty()) currentMatchIndex = (currentMatchIndex + 1) % matches.size
            },
            enabled = matches.isNotEmpty(),
          ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
          FilterChip(selected = matchCase, onClick = { matchCase = !matchCase }, label = { Text("Aa") })
          FilterChip(selected = wholeWord, onClick = { wholeWord = !wholeWord }, label = { Text("Word") })
          FilterChip(selected = useRegex, onClick = { useRegex = !useRegex }, label = { Text("Regex .*") })
        }
      }
    }

    // Go to line row if active
    AnimatedVisibility(visible = showGoToLine) {
      Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = targetLineNumber,
          onValueChange = { targetLineNumber = it },
          placeholder = { Text("Line number (1–${lines.size})") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = {
            val n = targetLineNumber.toIntOrNull()
            if (n != null && n in 1..lines.size) {
              flashedLineIndex = n - 1
              showGoToLine = false
            } else {
              onShowToast("Enter a line between 1 and ${lines.size}")
            }
          },
          shape = RoundedCornerShape(12.dp),
        ) {
          Text("Go")
        }
      }
    }

    // Main Content Area
    Box(
      modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        color = paperBg,
        border = BorderStroke(1.dp, extra.border),
      ) {
        if (isEditing && canEdit) {
          // Editing mode
          OutlinedTextField(
            value = currentContent,
            onValueChange = { saveContentChanges(it) },
            modifier = Modifier.fillMaxSize().padding(12.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = fontSizeSp.sp,
              color = defaultTextColor,
            ),
          )
        } else {
          // Read-only Code / Text / Log View
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 10.dp),
          ) {
            itemsIndexed(filteredLineIndices) { _, lineIdx ->
              val rawLine = lines[lineIdx]
              val isFlashed = lineIdx == flashedLineIndex

              // Line text color coding
              val lineTextColor = when {
                isLog && rawLine.contains("ERROR", ignoreCase = true) -> Color(0xFFF87171)
                isLog && rawLine.contains("WARN", ignoreCase = true) -> Color(0xFFFBBF24)
                isLog && rawLine.contains("INFO", ignoreCase = true) -> Color(0xFF60A5FA)
                isLog && rawLine.contains("DEBUG", ignoreCase = true) -> Color(0xFF9CA3AF)
                isJson && rawLine.contains(":") -> Color(0xFF79C0FF)
                else -> defaultTextColor
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(if (isFlashed) Color(0xFFFDE047).copy(alpha = 0.35f) else Color.Transparent)
                  .padding(vertical = 1.dp),
              ) {
                if (showLineNumbers) {
                  Text(
                    text = "${lineIdx + 1}".padStart(4, ' '),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = extra.textSecondary.copy(alpha = 0.45f),
                    modifier = Modifier.width(42.dp),
                  )
                }
                Text(
                  text = rawLine,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSizeSp.sp,
                    color = lineTextColor,
                  ),
                  softWrap = wordWrap,
                )
              }
            }
          }
        }
      }
    }

    // Bottom Tool Dock
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 8.dp, vertical = 6.dp)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.Filled.Search, "Find", active = showFind) { showFind = !showFind }
      ViewerToolTile(Icons.Filled.VerticalAlignBottom, "Go to", active = showGoToLine) { showGoToLine = !showGoToLine }
      ViewerToolTile(Icons.Filled.WrapText, if (wordWrap) "Wrap On" else "Wrap Off", active = wordWrap) { wordWrap = !wordWrap }
      ViewerToolTile(Icons.Filled.FormatListNumbered, "Lines", active = showLineNumbers) { showLineNumbers = !showLineNumbers }
      ViewerToolTile(Icons.Filled.ZoomIn, "Size +") { fontSizeSp = (fontSizeSp + 2).coerceAtMost(24) }
      ViewerToolTile(Icons.Filled.ZoomOut, "Size -") { fontSizeSp = (fontSizeSp - 2).coerceAtLeast(8) }
      ViewerToolTile(Icons.Filled.Palette, readerTheme.label) {
        readerTheme = when (readerTheme) {
          ReaderTheme.LIGHT -> ReaderTheme.SEPIA
          ReaderTheme.SEPIA -> ReaderTheme.DARK
          ReaderTheme.DARK -> ReaderTheme.LIGHT
        }
      }
      ViewerToolTile(Icons.Filled.Language, activeEncoding) {
        activeEncoding = when (activeEncoding) {
          "UTF-8" -> "UTF-16"
          "UTF-16" -> "ASCII"
          "ASCII" -> "ISO-8859-1"
          else -> "UTF-8"
        }
        onShowToast("Reopened with $activeEncoding")
      }
      if (canEdit) {
        ViewerToolTile(Icons.Filled.Edit, if (isEditing) "Done" else "Edit", active = isEditing) {
          isEditing = !isEditing
          if (!isEditing) onShowToast("Saved to file")
        }
      }
    }
  }
}
