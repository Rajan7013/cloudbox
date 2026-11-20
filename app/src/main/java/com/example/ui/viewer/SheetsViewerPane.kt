package com.example.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccessRole
import com.example.model.CloudFile
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetsViewerPane(
  file: CloudFile,
  sheetFile: File,
  sheets: List<Pair<String, SpreadsheetData>>,
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

  var activeSheetIndex by remember { mutableIntStateOf(0) }
  val activeSheet = sheets.getOrNull(activeSheetIndex) ?: ("Sheet 1" to SpreadsheetData(listOf("A", "B", "C"), emptyList(), 0.0, 0.0, 0))

  var activeTab by remember { mutableStateOf("Sheet") } // "Sheet", "Summary", "Chart"
  var showGridlines by remember { mutableStateOf(true) }
  var freezeHeader by remember { mutableStateOf(true) }
  var showFormulas by remember { mutableStateOf(false) }

  // Search & Filter
  var showSearch by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }

  // Sorting
  var sortColumnIdx by remember { mutableIntStateOf(-1) }
  var sortAscending by remember { mutableStateOf(true) }

  // Selection
  var selectedRow by remember { mutableIntStateOf(0) }
  var selectedCol by remember { mutableIntStateOf(0) }

  // Cell editing
  var editingCell by remember { mutableStateOf(false) }
  var editCellValue by remember { mutableStateOf("") }

  val headers = activeSheet.second.headers
  val rawRows = activeSheet.second.rows

  // Apply search filter and column sort
  val displayedRowIndices = remember(rawRows, searchQuery, sortColumnIdx, sortAscending) {
    val q = searchQuery.trim().lowercase()
    val filtered = if (q.isEmpty()) rawRows.indices.toList()
    else rawRows.indices.filter { idx -> rawRows[idx].any { it.lowercase().contains(q) } }

    if (sortColumnIdx in headers.indices) {
      filtered.sortedWith { a, b ->
        val vA = rawRows[a].getOrNull(sortColumnIdx).orEmpty()
        val vB = rawRows[b].getOrNull(sortColumnIdx).orEmpty()
        val nA = vA.toDoubleOrNull()
        val nB = vB.toDoubleOrNull()
        val cmp = if (nA != null && nB != null) nA.compareTo(nB) else vA.compareTo(vB, ignoreCase = true)
        if (sortAscending) cmp else -cmp
      }
    } else filtered
  }

  val selectedCellText = rawRows.getOrNull(selectedRow)?.getOrNull(selectedCol).orEmpty()
  val colLetter = if (selectedCol in 0..25) "${('A' + selectedCol)}" else "Col${selectedCol + 1}"
  val formulaText = if (showFormulas && selectedCellText.startsWith("=")) selectedCellText else selectedCellText

  // Stats calculation on the selected column
  val stats = remember(rawRows, selectedCol) {
    val nums = rawRows.mapNotNull { it.getOrNull(selectedCol)?.toDoubleOrNull() }
    if (nums.isNotEmpty()) {
      val sum = nums.sum()
      val avg = sum / nums.size
      val min = nums.minOrNull() ?: 0.0
      val max = nums.maxOrNull() ?: 0.0
      "Sum: ${String.format(Locale.US, "%.1f", sum)} · Avg: ${String.format(Locale.US, "%.1f", avg)} · Count: ${nums.size} · Min: $min · Max: $max"
    } else {
      "Selected: $colLetter${selectedRow + 1} · Rows: ${rawRows.size}"
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
          text = "Sheets · ${FormatUtils.formatBytes(file.sizeBytes)} · ${file.userRole.label}",
          style = MaterialTheme.typography.bodySmall,
          color = extra.textSecondary,
        )
      }
      IconButton(onClick = { showSearch = !showSearch }) {
        Icon(Icons.Filled.Search, contentDescription = "Search", tint = if (showSearch) MaterialTheme.colorScheme.primary else LocalContentColor.current)
      }
      if (file.canShare) {
        IconButton(onClick = onShare) { Icon(Icons.Filled.Share, contentDescription = "Share") }
      }
      IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
      IconButton(onClick = onOpenMoreActions) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
    }

    // Formula Bar (`fx`)
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.dp, extra.border),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("fx", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "$colLetter${selectedRow + 1}: $formulaText",
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        if (canEdit) {
          IconButton(
            onClick = {
              editCellValue = selectedCellText
              editingCell = true
            },
            modifier = Modifier.size(28.dp),
          ) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit cell", modifier = Modifier.size(16.dp))
          }
        }
      }
    }

    // Search bar if open
    AnimatedVisibility(visible = showSearch) {
      Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Find in sheet…") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${displayedRowIndices.size}/${rawRows.size} rows",
          style = MaterialTheme.typography.bodySmall,
          color = extra.textSecondary,
        )
      }
    }

    // Multi-sheet tabs (Sheet1, Summary, Charts, or real parsed sheets)
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      sheets.forEachIndexed { idx, pair ->
        FilterChip(
          selected = activeSheetIndex == idx && activeTab == "Sheet",
          onClick = {
            activeSheetIndex = idx
            activeTab = "Sheet"
          },
          label = { Text(pair.first) },
        )
      }
      FilterChip(
        selected = activeTab == "Summary",
        onClick = { activeTab = "Summary" },
        label = { Text("Summary (Σ)") },
      )
      FilterChip(
        selected = activeTab == "Chart",
        onClick = { activeTab = "Chart" },
        label = { Text("Charts") },
      )
    }

    // Content area based on activeTab
    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
      when (activeTab) {
        "Summary" -> {
          // Summary view
          Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            SummaryCard("Total Numeric Value", String.format(Locale.US, "%.2f", activeSheet.second.numericSum))
            SummaryCard("Largest Cell Value", String.format(Locale.US, "%.2f", activeSheet.second.largestValue))
            SummaryCard("Total Rows", rawRows.size.toString())
            SummaryCard("Total Columns", headers.size.toString())
            SummaryCard("Total Cells", activeSheet.second.totalCells.toString())
          }
        }

        "Chart" -> {
          // Charts view: bar chart representation of numeric data
          val numericCols = remember(rawRows) {
            headers.indices.filter { cIdx -> rawRows.any { it.getOrNull(cIdx)?.toDoubleOrNull() != null } }
          }
          val chartCol = numericCols.firstOrNull() ?: 0
          val chartValues = remember(rawRows, chartCol) {
            rawRows.take(15).mapNotNull { r ->
              val label = r.getOrNull(0).orEmpty().take(8)
              val value = r.getOrNull(chartCol)?.toDoubleOrNull() ?: 0.0
              label to value
            }
          }
          val maxVal = chartValues.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extra.border),
            modifier = Modifier.fillMaxSize().padding(8.dp),
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text("Bar Chart (${headers.getOrNull(chartCol) ?: "Column"})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(14.dp))
              Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                chartValues.forEach { (lbl, valD) ->
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(lbl.ifBlank { "Row" }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(68.dp), maxLines = 1)
                    val fraction = (valD / maxVal).toFloat().coerceIn(0.02f, 1f)
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                      Box(
                        modifier = Modifier
                          .fillMaxHeight()
                          .fillMaxWidth(fraction)
                          .clip(RoundedCornerShape(4.dp))
                          .background(MaterialTheme.colorScheme.primary),
                      )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(String.format(Locale.US, "%.1f", valD), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(50.dp))
                  }
                }
              }
            }
          }
        }

        else -> {
          // Standard Spreadsheet Grid
          val hScroll = rememberScrollState()

          Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extra.border),
          ) {
            LazyColumn(modifier = Modifier.horizontalScroll(hScroll)) {
              // Sticky Header Row
              item {
                Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                  // Corner cell
                  Box(
                    modifier = Modifier.width(44.dp).height(36.dp).border(if (showGridlines) 0.5.dp else 0.dp, extra.border),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text("#", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
                  }
                  // Column headers
                  headers.forEachIndexed { cIdx, h ->
                    val isSorted = sortColumnIdx == cIdx
                    val arrow = if (isSorted) (if (sortAscending) " ▲" else " ▼") else ""
                    Box(
                      modifier = Modifier
                        .width(108.dp)
                        .height(36.dp)
                        .border(if (showGridlines) 0.5.dp else 0.dp, extra.border)
                        .clickable {
                          if (sortColumnIdx == cIdx) sortAscending = !sortAscending
                          else { sortColumnIdx = cIdx; sortAscending = true }
                        }
                        .padding(horizontal = 6.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = "$h$arrow",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSorted) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSorted) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        maxLines = 1,
                      )
                    }
                  }
                }
              }

              // Data Rows
              itemsIndexed(displayedRowIndices) { _, rIdx ->
                val rowData = rawRows.getOrNull(rIdx).orEmpty()
                Row {
                  // Row number cell
                  Box(
                    modifier = Modifier
                      .width(44.dp)
                      .height(36.dp)
                      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                      .border(if (showGridlines) 0.5.dp else 0.dp, extra.border),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text("${rIdx + 1}", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
                  }
                  // Data cells
                  headers.indices.forEach { cIdx ->
                    val cellText = rowData.getOrNull(cIdx).orEmpty()
                    val isSelected = rIdx == selectedRow && cIdx == selectedCol
                    val isMatched = searchQuery.isNotBlank() && cellText.contains(searchQuery, ignoreCase = true)

                    Box(
                      modifier = Modifier
                        .width(108.dp)
                        .height(36.dp)
                        .background(if (isMatched) Color(0xFFFFF59D) else Color.Transparent)
                        .border(
                          width = if (isSelected) 2.dp else (if (showGridlines) 0.5.dp else 0.dp),
                          color = if (isSelected) MaterialTheme.colorScheme.primary else extra.border,
                        )
                        .clickable {
                          selectedRow = rIdx
                          selectedCol = cIdx
                        }
                        .padding(horizontal = 6.dp),
                      contentAlignment = Alignment.CenterStart,
                    ) {
                      Text(
                        text = cellText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMatched) Color.Black else LocalContentColor.current,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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

    // Status Bar for Selection
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = stats,
        style = MaterialTheme.typography.labelSmall,
        color = extra.textSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
      )
    }

    // Bottom Tool Dock (Spec: 5 visible, horizontal scroll, 64px width tiles with icon + 11px label)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 8.dp, vertical = 6.dp)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ViewerToolTile(Icons.Filled.Search, "Find", active = showSearch) { showSearch = !showSearch }
      ViewerToolTile(Icons.Filled.GridOn, "Gridlines", active = showGridlines) { showGridlines = !showGridlines }
      ViewerToolTile(Icons.Filled.AcUnit, "Freeze", active = freezeHeader) { freezeHeader = !freezeHeader }
      ViewerToolTile(Icons.Filled.Functions, "Formulas", active = showFormulas) { showFormulas = !showFormulas }
      ViewerToolTile(Icons.Filled.Sort, "Sort Col", active = sortColumnIdx >= 0) {
        if (sortColumnIdx == selectedCol) sortAscending = !sortAscending
        else { sortColumnIdx = selectedCol; sortAscending = true }
      }
      ViewerToolTile(Icons.Filled.BarChart, "Charts", active = activeTab == "Chart") {
        activeTab = if (activeTab == "Chart") "Sheet" else "Chart"
      }
      if (canEdit) {
        ViewerToolTile(Icons.Filled.Edit, "Edit Cell") {
          editCellValue = selectedCellText
          editingCell = true
        }
      }
      if (file.canDownload) {
        ViewerToolTile(Icons.Filled.FileDownload, "Export") {
          onDownload()
        }
      }
    }
  }

  // Edit Cell Dialog
  if (editingCell) {
    AlertDialog(
      onDismissRequest = { editingCell = false },
      title = { Text("Edit Cell $colLetter${selectedRow + 1}") },
      text = {
        OutlinedTextField(
          value = editCellValue,
          onValueChange = { editCellValue = it },
          singleLine = true,
          label = { Text("Value or formula") },
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(onClick = {
          editingCell = false
          onShowToast("Updated $colLetter${selectedRow + 1}")
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingCell = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun SummaryCard(label: String, value: String) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, LocalCloudBoxColors.current.border),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(label, style = MaterialTheme.typography.bodyLarge)
      Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
  }
}
