package com.example.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
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
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownViewerPane(
  file: CloudFile,
  mdFile: File,
  rawMarkdown: String,
  onBack: () -> Unit,
  onShare: () -> Unit,
  onDownload: () -> Unit,
  onOpenMoreActions: () -> Unit,
  onSettings: () -> Unit,
  onShowToast: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val extra = LocalCloudBoxColors.current
  val canEdit = file.ownedByMe || file.userRole == AccessRole.EDITOR

  var currentSource by remember(rawMarkdown) { mutableStateOf(rawMarkdown) }
  var viewMode by remember { mutableStateOf("Rendered") } // "Rendered", "Source", "Edit"
  var readerTheme by remember { mutableStateOf(ReaderTheme.LIGHT) }
  var fontScalePercent by remember { mutableIntStateOf(100) }
  var wrapLines by remember { mutableStateOf(true) }
  var showLineNumbers by remember { mutableStateOf(false) }

  // Search
  var showFind by remember { mutableStateOf(false) }
  var findQuery by remember { mutableStateOf("") }
  var matchCase by remember { mutableStateOf(false) }
  var currentMatchIndex by remember { mutableIntStateOf(0) }

  // TOC
  var showToc by remember { mutableStateOf(false) }

  // Parse Headings for TOC
  val headings = remember(currentSource) {
    currentSource.lines().filter { it.trim().startsWith("#") }.take(20)
  }

  // Parse Search Matches
  val matches = remember(currentSource, findQuery, matchCase) {
    val q = findQuery.trim()
    if (q.isEmpty()) emptyList()
    else {
      val flags = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
      Regex(Regex.escape(q), flags).findAll(currentSource).map { it.range }.toList()
    }
  }

  val scrollState = rememberScrollState()

  // Rendered CommonMark HTML
  val renderedHtml = remember(currentSource) {
    try {
      val parser = Parser.builder().build()
      val document = parser.parse(currentSource)
      val renderer = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build()
      renderer.render(document)
    } catch (_: Exception) {
      currentSource
    }
  }

  val paperBg = when (readerTheme) {
    ReaderTheme.LIGHT -> Color.White
    ReaderTheme.SEPIA -> extra.sepiaPaperBg
    ReaderTheme.DARK -> Color(0xFF0D1117)
  }
  val textColor = when (readerTheme) {
    ReaderTheme.LIGHT -> Color(0xFF1F2328)
    ReaderTheme.SEPIA -> extra.sepiaPaperText
    ReaderTheme.DARK -> Color(0xFFE6EDF3)
  }

  fun saveMarkdownChanges(newText: String) {
    currentSource = newText
    try {
      mdFile.writeText(newText)
    } catch (e: Exception) {
      onShowToast("Failed to write to file: ${e.localizedMessage}")
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
          text = "Markdown · ${FormatUtils.formatBytes(file.sizeBytes)} · ${file.userRole.label}",
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

    // Mode Selector Header (Rendered, Source, Edit)
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Rendered", "Source").forEach { m ->
          FilterChip(
            selected = viewMode == m,
            onClick = { viewMode = m },
            label = { Text(m) },
          )
        }
        if (canEdit) {
          FilterChip(
            selected = viewMode == "Edit",
            onClick = { viewMode = if (viewMode == "Edit") "Rendered" else "Edit" },
            label = { Text(if (viewMode == "Edit") "Editing" else "Edit") },
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) },
          )
        }
      }
      if (viewMode == "Edit") {
        Text("Autosave on", style = MaterialTheme.typography.labelSmall, color = extra.success)
      }
    }

    // Search Box if active
    AnimatedVisibility(visible = showFind) {
      Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = findQuery,
          onValueChange = { findQuery = it; currentMatchIndex = 0 },
          placeholder = { Text("Find in markdown…") },
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
    }

    // TOC Headings Strip
    if (headings.isNotEmpty() && showToc) {
      Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        headings.forEach { h ->
          AssistChip(
            onClick = {
              onShowToast("Section: ${h.trim('#', ' ')}")
            },
            label = { Text(h.trim('#', ' '), maxLines = 1) },
          )
        }
      }
    }

    // Content area
    Box(
      modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        color = paperBg,
        border = BorderStroke(1.dp, extra.border),
      ) {
        when (viewMode) {
          "Edit" -> {
            // Live Editor with formatting chips
            Column(modifier = Modifier.fillMaxSize()) {
              // Formatting Chips row
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                listOf(
                  "B" to "**bold**",
                  "I" to "_italic_",
                  "H1" to "# Heading 1\n",
                  "•" to "- List item\n",
                  "[ ]" to "- [ ] Task item\n",
                  "`</>`" to "```kotlin\n// code\n```\n",
                  "Link" to "[title](https://example.com)",
                ).forEach { (lbl, insert) ->
                  FilterChip(
                    selected = false,
                    onClick = {
                      saveMarkdownChanges(currentSource + "\n" + insert)
                    },
                    label = { Text(lbl) },
                  )
                }
              }
              HorizontalDivider(color = extra.border)
              OutlinedTextField(
                value = currentSource,
                onValueChange = { saveMarkdownChanges(it) },
                modifier = Modifier.fillMaxSize().padding(10.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                  fontFamily = FontFamily.Monospace,
                  fontSize = (13 * fontScalePercent / 100f).sp,
                  color = textColor,
                ),
              )
            }
          }

          "Source" -> {
            // Source Code View with Line Numbers
            val lines = currentSource.lines()
            Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)) {
              Column {
                lines.forEachIndexed { idx, line ->
                  Row(modifier = Modifier.fillMaxWidth()) {
                    if (showLineNumbers) {
                      Text(
                        text = "${idx + 1}".padStart(3, ' '),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = extra.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.width(36.dp),
                      )
                    }
                    Text(
                      text = line,
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (13 * fontScalePercent / 100f).sp,
                        color = textColor,
                      ),
                      softWrap = wrapLines,
                    )
                  }
                }
              }
            }
          }

          else -> {
            // Rendered Markdown View with clickable tasks and copy code buttons
            val lines = currentSource.lines()
            Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(18.dp)) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var inCodeBlock = false
                val codeBuffer = StringBuilder()

                lines.forEachIndexed { lineIdx, line ->
                  val trimmed = line.trim()
                  when {
                    trimmed.startsWith("```") -> {
                      if (inCodeBlock) {
                        // End code block
                        val code = codeBuffer.toString()
                        Surface(
                          shape = RoundedCornerShape(10.dp),
                          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                          border = BorderStroke(1.dp, extra.border),
                          modifier = Modifier.fillMaxWidth(),
                        ) {
                          Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                              FilledTonalButton(
                                onClick = {
                                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                  clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
                                  onShowToast("Code copied to clipboard")
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp),
                              ) {
                                Text("Copy", fontSize = 11.sp)
                              }
                            }
                            Text(
                              text = code,
                              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                              color = textColor,
                            )
                          }
                        }
                        codeBuffer.clear()
                        inCodeBlock = false
                      } else {
                        inCodeBlock = true
                      }
                    }

                    inCodeBlock -> {
                      codeBuffer.append(line).append("\n")
                    }

                    trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") -> {
                      // Clickable Task List item!
                      val isChecked = trimmed.startsWith("- [x]")
                      val taskText = trimmed.drop(5).trim()
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                          .fillMaxWidth()
                          .clickable(enabled = canEdit) {
                            val newLines = lines.toMutableList()
                            newLines[lineIdx] = if (isChecked) line.replaceFirst("- [x]", "- [ ]") else line.replaceFirst("- [ ]", "- [x]")
                            saveMarkdownChanges(newLines.joinToString("\n"))
                            onShowToast(if (isChecked) "Task unchecked" else "Task checked")
                          }
                          .padding(vertical = 3.dp),
                      ) {
                        Checkbox(
                          checked = isChecked,
                          onCheckedChange = { checked ->
                            if (canEdit) {
                              val newLines = lines.toMutableList()
                              newLines[lineIdx] = if (checked) line.replaceFirst("- [ ]", "- [x]") else line.replaceFirst("- [x]", "- [ ]")
                              saveMarkdownChanges(newLines.joinToString("\n"))
                            }
                          },
                          enabled = canEdit,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = taskText,
                          style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (15 * fontScalePercent / 100f).sp,
                            color = textColor,
                          ),
                        )
                      }
                    }

                    trimmed.startsWith("# ") -> {
                      Text(
                        text = trimmed.drop(2),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                      )
                      HorizontalDivider(color = extra.border, modifier = Modifier.padding(vertical = 4.dp))
                    }

                    trimmed.startsWith("## ") -> {
                      Text(
                        text = trimmed.drop(3),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor,
                      )
                    }

                    trimmed.startsWith("### ") -> {
                      Text(
                        text = trimmed.drop(4),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor,
                      )
                    }

                    trimmed.startsWith("> ") -> {
                      Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                      ) {
                        Text(
                          text = trimmed.drop(2),
                          style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                          modifier = Modifier.padding(10.dp),
                          color = textColor,
                        )
                      }
                    }

                    else -> {
                      if (trimmed.isNotEmpty()) {
                        Text(
                          text = trimmed,
                          style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (15 * fontScalePercent / 100f).sp,
                            lineHeight = (22 * fontScalePercent / 100f).sp,
                          ),
                          color = textColor,
                        )
                      }
                    }
                  }
                }
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
      ViewerToolTile(Icons.Filled.FormatListBulleted, "TOC", active = showToc) { showToc = !showToc }
      ViewerToolTile(Icons.Filled.ZoomIn, "Size +") { fontScalePercent = (fontScalePercent + 15).coerceAtMost(180) }
      ViewerToolTile(Icons.Filled.ZoomOut, "Size -") { fontScalePercent = (fontScalePercent - 15).coerceAtLeast(80) }
      ViewerToolTile(Icons.Filled.Palette, readerTheme.label) {
        readerTheme = when (readerTheme) {
          ReaderTheme.LIGHT -> ReaderTheme.SEPIA
          ReaderTheme.SEPIA -> ReaderTheme.DARK
          ReaderTheme.DARK -> ReaderTheme.LIGHT
        }
      }
      ViewerToolTile(Icons.Filled.WrapText, if (wrapLines) "Wrap On" else "Wrap Off", active = wrapLines) {
        wrapLines = !wrapLines
      }
      ViewerToolTile(Icons.Filled.FormatListNumbered, "Lines", active = showLineNumbers) {
        showLineNumbers = !showLineNumbers
      }
      if (canEdit) {
        ViewerToolTile(Icons.Filled.Edit, if (viewMode == "Edit") "Preview" else "Edit", active = viewMode == "Edit") {
          viewMode = if (viewMode == "Edit") "Rendered" else "Edit"
        }
      }
    }
  }
}
