package com.example.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DocComment(
  val id: String,
  val author: String,
  val text: String,
  val timestamp: String,
  val isOpen: Boolean = true,
)

private data class DocVersion(
  val id: String,
  val date: String,
  val author: String,
  val isCurrent: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDocsViewerPane(
  file: CloudFile,
  docFile: File,
  initialText: String,
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
  val canComment = file.ownedByMe || file.userRole == AccessRole.EDITOR || file.userRole == AccessRole.COMMENTER

  var currentText by remember(initialText) { mutableStateOf(initialText) }
  var isEditing by remember { mutableStateOf(false) }
  var isPrintLayout by remember { mutableStateOf(false) }
  var readerTheme by remember { mutableStateOf(ReaderTheme.LIGHT) }
  var fontScalePercent by remember { mutableIntStateOf(100) }
  var fontFamilyChoice by remember { mutableStateOf("Sans") }
  var lineSpacingChoice by remember { mutableFloatStateOf(1.6f) }
  var marginChoice by remember { mutableStateOf("Normal") }

  // Search in doc
  var showFind by remember { mutableStateOf(false) }
  var findQuery by remember { mutableStateOf("") }
  var matchCase by remember { mutableStateOf(false) }
  var wholeWord by remember { mutableStateOf(false) }
  var currentMatchIndex by remember { mutableIntStateOf(0) }

  // Outline / TOC
  var showOutline by remember { mutableStateOf(false) }
  // Comments & History Sheet
  var showCommentsSheet by remember { mutableStateOf(false) }
  // Text formatting bottom sheet
  var showTextSettingsSheet by remember { mutableStateOf(false) }

  // Mock comments stored in memory for interactive commenting
  val comments = remember {
    mutableStateListOf(
      DocComment("1", "Riya Sen", "Please verify the Q4 numbers in section 2.", "Yesterday", isOpen = true),
      DocComment("2", file.ownerName.ifBlank { "Owner" }, "All project specifications approved.", "2 days ago", isOpen = true),
    )
  }

  // Version history
  val versions = remember {
    listOf(
      DocVersion("v3", SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(file.modifiedTimeEpochMs)), "You", isCurrent = true),
      DocVersion("v2", "Yesterday 16:30", file.ownerName.ifBlank { "Collaborator" }),
      DocVersion("v1", "Nov 12, 10:15", file.ownerName.ifBlank { "Owner" }),
    )
  }

  // Parse words & reading time
  val wordCount = remember(currentText) {
    currentText.split("\\s+".toRegex()).count { it.isNotBlank() }
  }
  val readingTimeMin = remember(wordCount) {
    (wordCount / 200).coerceAtLeast(1)
  }

  // Parse Headings
  val headings = remember(currentText) {
    currentText.lines()
      .filter { it.trim().startsWith("#") || it.trim().matches("^\\d+\\..*".toRegex()) || (it.length in 3..50 && it.all { c -> c.isLetterOrDigit() || c.isWhitespace() } && it.trim().isNotEmpty()) }
      .take(15)
  }

  // Matches for Search
  val matches = remember(currentText, findQuery, matchCase, wholeWord) {
    val q = findQuery.trim()
    if (q.isEmpty()) emptyList()
    else {
      val pattern = if (wholeWord) "\\b${Regex.escape(q)}\\b" else Regex.escape(q)
      val flags = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
      Regex(pattern, flags).findAll(currentText).map { it.range }.toList()
    }
  }

  val matchCount = matches.size
  val scrollState = rememberScrollState()

  // Annotated text with yellow/orange search highlights
  val annotatedContent = remember(currentText, matches, currentMatchIndex) {
    buildAnnotatedString {
      append(currentText)
      matches.forEachIndexed { i, range ->
        val bgColor = if (i == currentMatchIndex) Color(0xFFFFB74D) else Color(0xFFFFF59D)
        addStyle(SpanStyle(background = bgColor, color = Color.Black), range.first, range.last + 1)
      }
    }
  }

  val paperBg = when (readerTheme) {
    ReaderTheme.LIGHT -> Color.White
    ReaderTheme.SEPIA -> extra.sepiaPaperBg
    ReaderTheme.DARK -> Color(0xFF161828)
  }
  val textColor = when (readerTheme) {
    ReaderTheme.LIGHT -> Color(0xFF1E2138)
    ReaderTheme.SEPIA -> extra.sepiaPaperText
    ReaderTheme.DARK -> Color(0xFFEEF0FA)
  }
  val fontFamily = when (fontFamilyChoice) {
    "Serif" -> FontFamily.Serif
    "Mono" -> FontFamily.Monospace
    else -> FontFamily.Default
  }
  val horizontalPadding = when (marginChoice) {
    "Narrow" -> 14.dp
    "Wide" -> 36.dp
    else -> 22.dp
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
          text = "Docs · ${FormatUtils.formatBytes(file.sizeBytes)} · ${file.userRole.label}",
          style = MaterialTheme.typography.bodySmall,
          color = extra.textSecondary,
        )
      }
      IconButton(onClick = { showFind = !showFind }) {
        Icon(Icons.Filled.Search, contentDescription = "Search", tint = if (showFind) MaterialTheme.colorScheme.primary else LocalContentColor.current)
      }
      if (file.canShare) {
        IconButton(onClick = onShare) {
          Icon(Icons.Filled.Share, contentDescription = "Share")
        }
      }
      IconButton(onClick = onSettings) {
        Icon(Icons.Filled.Settings, contentDescription = "Settings")
      }
      IconButton(onClick = onOpenMoreActions) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More")
      }
    }

    // Stats bar: word count & reading time
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "$wordCount words · $readingTimeMin min read",
          style = MaterialTheme.typography.labelSmall,
          color = extra.textSecondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (canEdit && isEditing) {
            Text("Auto-saving", style = MaterialTheme.typography.labelSmall, color = extra.success)
          }
          Text(
            text = if (isPrintLayout) "Print Layout" else "Reflow Reading",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    // Search bar if open
    AnimatedVisibility(visible = showFind) {
      Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = findQuery,
            onValueChange = { findQuery = it; currentMatchIndex = 0 },
            placeholder = { Text("Find in document…") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
              if (findQuery.isNotEmpty()) {
                IconButton(onClick = { findQuery = "" }) { Icon(Icons.Filled.Close, contentDescription = "Clear") }
              }
            },
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (matchCount == 0) "0" else "${currentMatchIndex + 1}/$matchCount",
            style = MaterialTheme.typography.bodySmall,
          )
          IconButton(
            onClick = {
              if (matchCount > 0) currentMatchIndex = (currentMatchIndex - 1 + matchCount) % matchCount
            },
            enabled = matchCount > 0,
          ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
          }
          IconButton(
            onClick = {
              if (matchCount > 0) currentMatchIndex = (currentMatchIndex + 1) % matchCount
            },
            enabled = matchCount > 0,
          ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
          FilterChip(
            selected = matchCase,
            onClick = { matchCase = !matchCase },
            label = { Text("Match case (Aa)") },
          )
          FilterChip(
            selected = wholeWord,
            onClick = { wholeWord = !wholeWord },
            label = { Text("Whole word") },
          )
        }
      }
    }

    // Headings Outline Horizontal Bar
    if (headings.isNotEmpty() && showOutline) {
      Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        headings.forEach { h ->
          AssistChip(
            onClick = {
              val idx = currentText.indexOf(h)
              if (idx >= 0) {
                onShowToast("Jumped to: ${h.take(20)}…")
              }
            },
            label = { Text(h.trimStart('#', ' ').take(24), maxLines = 1) },
          )
        }
      }
    }

    // Main Content Area
    Box(
      modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = if (isPrintLayout) 12.dp else 4.dp, vertical = 6.dp),
    ) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        color = paperBg,
        border = BorderStroke(1.dp, extra.border),
        shadowElevation = if (isPrintLayout) 6.dp else 1.dp,
      ) {
        if (isEditing && canEdit) {
          OutlinedTextField(
            value = currentText,
            onValueChange = {
              currentText = it
              try { docFile.writeText(it) } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxSize().padding(horizontalPadding, 16.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
              fontFamily = fontFamily,
              fontSize = (15 * fontScalePercent / 100f).sp,
              lineHeight = (22 * fontScalePercent / 100f * lineSpacingChoice).sp,
              color = textColor,
            ),
          )
        } else {
          Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = horizontalPadding, vertical = 18.dp)) {
            Text(
              text = annotatedContent,
              style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = fontFamily,
                fontSize = (15 * fontScalePercent / 100f).sp,
                lineHeight = (22 * fontScalePercent / 100f * lineSpacingChoice).sp,
              ),
              color = textColor,
            )
          }
        }
      }
    }

    // Bottom Tool Dock (Spec: 5 visible, horizontal scroll, 64px width tiles with icon + 11px label)
    Row(
      modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 6.dp).horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.Filled.Search, "Find", active = showFind) { showFind = !showFind }
      ViewerToolTile(Icons.Filled.FormatListBulleted, "Outline", active = showOutline) { showOutline = !showOutline }
      ViewerToolTile(Icons.Filled.TextFields, "Text", active = showTextSettingsSheet) { showTextSettingsSheet = true }
      ViewerToolTile(Icons.Filled.Palette, readerTheme.label) {
        readerTheme = when (readerTheme) {
          ReaderTheme.LIGHT -> ReaderTheme.SEPIA
          ReaderTheme.SEPIA -> ReaderTheme.DARK
          ReaderTheme.DARK -> ReaderTheme.LIGHT
        }
      }
      ViewerToolTile(Icons.Filled.MenuBook, if (isPrintLayout) "Print" else "Reflow", active = isPrintLayout) {
        isPrintLayout = !isPrintLayout
      }
      ViewerToolTile(Icons.Filled.Comment, "Comments", active = showCommentsSheet) {
        showCommentsSheet = true
      }
      if (canEdit) {
        ViewerToolTile(Icons.Filled.Edit, if (isEditing) "Done" else "Edit", active = isEditing) {
          isEditing = !isEditing
          if (!isEditing) onShowToast("Changes saved")
        }
      }
    }
  }

  // Text Typography Bottom Sheet
  if (showTextSettingsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showTextSettingsSheet = false },
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 24.dp)) {
        Text("Text & Display Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(14.dp))

        Text("Font Family", style = MaterialTheme.typography.labelMedium, color = extra.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
          listOf("Sans", "Serif", "Mono").forEach { f ->
            FilterChip(selected = fontFamilyChoice == f, onClick = { fontFamilyChoice = f }, label = { Text(f) })
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Size: $fontScalePercent%", style = MaterialTheme.typography.bodyMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { fontScalePercent = (fontScalePercent - 10).coerceAtLeast(70) }, shape = CircleShape, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) {
              Text("−")
            }
            OutlinedButton(onClick = { fontScalePercent = (fontScalePercent + 10).coerceAtMost(200) }, shape = CircleShape, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) {
              Text("+")
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Line Spacing", style = MaterialTheme.typography.labelMedium, color = extra.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
          listOf(1.3f to "Compact", 1.6f to "Normal", 2.0f to "Relaxed").forEach { (v, l) ->
            FilterChip(selected = lineSpacingChoice == v, onClick = { lineSpacingChoice = v }, label = { Text(l) })
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Margins", style = MaterialTheme.typography.labelMedium, color = extra.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
          listOf("Narrow", "Normal", "Wide").forEach { m ->
            FilterChip(selected = marginChoice == m, onClick = { marginChoice = m }, label = { Text(m) })
          }
        }
      }
    }
  }

  // Comments & History Bottom Sheet
  if (showCommentsSheet) {
    var sheetTab by remember { mutableIntStateOf(0) }
    var newCommentText by remember { mutableStateOf("") }

    ModalBottomSheet(
      onDismissRequest = { showCommentsSheet = false },
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 28.dp)) {
        TabRow(selectedTabIndex = sheetTab) {
          Tab(selected = sheetTab == 0, onClick = { sheetTab = 0 }, text = { Text("Comments (${comments.size})") })
          Tab(selected = sheetTab == 1, onClick = { sheetTab = 1 }, text = { Text("Version History") })
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (sheetTab == 0) {
          // Add comment box if commenter or editor
          if (canComment) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                placeholder = { Text("Add a comment…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  if (newCommentText.isNotBlank()) {
                    comments.add(0, DocComment(System.currentTimeMillis().toString(), "You", newCommentText.trim(), "Just now", isOpen = true))
                    newCommentText = ""
                    onShowToast("Comment posted")
                  }
                },
                enabled = newCommentText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
              ) {
                Text("Post")
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
          } else {
            Text("Viewers have read-only access to comments.", style = MaterialTheme.typography.bodySmall, color = extra.textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
          }

          Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
            comments.forEachIndexed { i, c ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(c.author, style = MaterialTheme.typography.labelLarge)
                    Text(c.timestamp, style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(c.text, style = MaterialTheme.typography.bodyMedium)
                  if (canComment) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                      TextButton(onClick = {
                        comments[i] = c.copy(isOpen = !c.isOpen)
                        onShowToast(if (c.isOpen) "Comment resolved" else "Comment reopened")
                      }) {
                        Text(if (c.isOpen) "Resolve" else "Reopen", style = MaterialTheme.typography.labelSmall)
                      }
                    }
                  }
                }
              }
            }
          }
        } else {
          // Version History
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            versions.forEach { v ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (v.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(v.id.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                      if (v.isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        SuggestionChip(onClick = {}, label = { Text("Current", fontSize = 10.sp) })
                      }
                    }
                    Text("${v.date} by ${v.author}", style = MaterialTheme.typography.bodySmall, color = extra.textSecondary)
                  }
                  if (!v.isCurrent && canEdit) {
                    TextButton(onClick = { onShowToast("Restored ${v.id}") }) {
                      Text("Restore")
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
}
