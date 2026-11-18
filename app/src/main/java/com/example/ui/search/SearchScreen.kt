package com.example.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.model.CloudFile
import com.example.model.FileCategory
import com.example.model.ScreenDataState
import com.example.ui.components.CloudBoxEmptyState
import com.example.ui.components.CloudBoxErrorCard
import com.example.ui.components.FileListSkeleton
import com.example.ui.components.FileTypeTile
import com.example.ui.theme.LocalCloudBoxColors
import com.example.util.FormatUtils

@Composable
fun SearchScreen(
  query: String,
  selectedCategory: FileCategory?,
  recentSearches: List<String>,
  searchState: ScreenDataState<List<CloudFile>>,
  isOnline: Boolean,
  imageLoader: ImageLoader,
  onBack: () -> Unit,
  onQueryChange: (String) -> Unit,
  onSubmitSearch: (String) -> Unit,
  onRemoveRecentSearch: (String) -> Unit,
  onSelectCategory: (FileCategory?) -> Unit,
  onOpenFile: (CloudFile) -> Unit,
  onFileMoreClick: (CloudFile) -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    runCatching { focusRequester.requestFocus() }
  }

  Column(modifier = Modifier.fillMaxSize().testTag("search_screen")) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("search_back_button")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search files or folders…") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmitSearch(query) }),
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.testTag("search_clear_button")) {
              Icon(Icons.Filled.Close, contentDescription = "Clear search")
            }
          }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = extra.border,
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.weight(1f).focusRequester(focusRequester).testTag("search_input"),
      )
    }

    if (!isOnline) {
      Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = extra.warning.copy(alpha = 0.14f),
      ) {
        Text(
          text = "Searching saved offline files only",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
    }

    // Type filter chips
    val categories =
      listOf<Pair<FileCategory?, String>>(
        null to "All",
        FileCategory.DOCUMENT to "Docs",
        FileCategory.PDF to "PDF",
        FileCategory.IMAGE to "Images",
        FileCategory.VIDEO to "Video",
        FileCategory.AUDIO to "Audio",
        FileCategory.FOLDER to "Folders",
      )
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      categories.forEach { (cat, label) ->
        val selected = selectedCategory == cat
        FilterChip(
          selected = selected,
          onClick = { onSelectCategory(cat) },
          label = { Text(label) },
          shape = RoundedCornerShape(20.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
          ),
        )
      }
    }

    if (query.isBlank() && selectedCategory == null) {
      // Idle state: Recent searches + Suggested quick filters
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (recentSearches.isNotEmpty()) {
          item {
            Text("RECENT SEARCHES", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
          }
          items(recentSearches.size, key = { idx -> "rec_${recentSearches[idx]}" }) { idx ->
            val term = recentSearches[idx]
            Surface(
              onClick = { onSubmitSearch(term) },
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, extra.border.copy(alpha = 0.6f)),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = extra.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(term, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemoveRecentSearch(term) }, modifier = Modifier.size(36.dp)) {
                  Icon(Icons.Filled.Close, contentDescription = "Remove recent search", modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
        item {
          Spacer(modifier = Modifier.height(6.dp))
          Text("SUGGESTED FILTERS", style = MaterialTheme.typography.labelSmall, color = extra.textSecondary)
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = false,
              onClick = { onSelectCategory(FileCategory.PDF) },
              label = { Text("PDF documents") },
              shape = RoundedCornerShape(20.dp),
            )
            FilterChip(
              selected = false,
              onClick = { onSelectCategory(FileCategory.IMAGE) },
              label = { Text("Photos & Images") },
              shape = RoundedCornerShape(20.dp),
            )
          }
        }
      }
    } else {
      when (searchState) {
        is ScreenDataState.Loading -> {
          Box(modifier = Modifier.fillMaxSize().padding(16.dp)) { FileListSkeleton(5) }
        }
        is ScreenDataState.Error -> {
          Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CloudBoxErrorCard(message = searchState.message, onRetry = { onSubmitSearch(query) })
          }
        }
        is ScreenDataState.Empty -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CloudBoxEmptyState(
              title = if (query.isNotBlank()) "No results for ‘$query’" else "No matching files",
              subtitle = "Check the spelling or try a different type filter.",
              primaryActionLabel = "Clear filters",
              onPrimaryAction = {
                onQueryChange("")
                onSelectCategory(null)
              },
              icon = Icons.Filled.Search,
            )
          }
        }
        is ScreenDataState.Success -> {
          val results = searchState.data
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            item {
              Text(
                text = "${results.size} ${if (results.size == 1) "result" else "results"}",
                style = MaterialTheme.typography.bodySmall,
                color = extra.textSecondary,
              )
            }
            items(results.size, key = { idx -> results[idx].id }) { idx ->
              val file = results[idx]
              SearchResultRow(
                file = file,
                query = query,
                imageLoader = imageLoader,
                onClick = { onOpenFile(file) },
                onMoreClick = { onFileMoreClick(file) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SearchResultRow(
  file: CloudFile,
  query: String,
  imageLoader: ImageLoader,
  onClick: () -> Unit,
  onMoreClick: () -> Unit,
) {
  val extra = LocalCloudBoxColors.current
  val primaryColor = MaterialTheme.colorScheme.primary
  val highlightedName =
    remember(file.name, query, primaryColor) {
      buildAnnotatedString {
        val q = query.trim()
        val idx = if (q.isNotEmpty()) file.name.indexOf(q, ignoreCase = true) else -1
        if (idx >= 0) {
          append(file.name.substring(0, idx))
          withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
            append(file.name.substring(idx, idx + q.length))
          }
          append(file.name.substring(idx + q.length))
        } else {
          append(file.name)
        }
      }
    }

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, extra.border.copy(alpha = 0.7f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FileTypeTile(category = file.category, thumbnailLink = file.thumbnailLink, imageLoader = imageLoader, size = 44.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = highlightedName,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${file.category.label} · ${FormatUtils.formatRelativeDate(file.modifiedTimeEpochMs)}",
          style = MaterialTheme.typography.bodySmall,
          color = extra.textSecondary,
        )
      }
      IconButton(onClick = onMoreClick, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Filled.MoreVert, contentDescription = "Actions")
      }
    }
  }
}
