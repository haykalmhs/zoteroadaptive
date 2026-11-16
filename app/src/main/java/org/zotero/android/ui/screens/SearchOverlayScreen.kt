package org.zotero.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.data.model.ItemType
import org.zotero.android.data.model.ZoteroItem
import org.zotero.android.ui.SearchFilters
import org.zotero.android.ui.ZoteroUiState
import org.zotero.android.ui.theme.ZoteroThemeTokens

@Composable
fun SearchOverlayScreen(
    uiState: ZoteroUiState,
    onClose: () -> Unit,
    onUpdateFilters: ((SearchFilters) -> SearchFilters) -> Unit,
    onClearFilters: () -> Unit,
    onRecordRecentSearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onToggleCompactView: () -> Unit,
    onOpenPaperReader: (Int) -> Unit,
    onOpenMetadataSheet: (Int) -> Unit,
    onToggleFavorite: (ZoteroItem) -> Unit
) {
    val extColors = ZoteroThemeTokens.colors
    BackHandler(onBack = onClose)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val sf = uiState.searchFilters
    val activeItems = uiState.allItems.filter { !it.isInTrash }
    val distinctAuthors = remember(activeItems) {
        activeItems.flatMap { it.creators }.map { it.lastName }.distinct().sorted()
    }
    val distinctYears = remember(activeItems) {
        activeItems.map { it.year }.filter { it.isNotBlank() }.distinct().sortedDescending()
    }

    var showCollectionMenu by remember { mutableStateOf(false) }
    var showAuthorMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("full_screen_search_overlay"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top Search Input Row (Gmail Search Screen header + View Switcher)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("search_overlay_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (sf.query.isEmpty()) {
                        Text(
                            text = "Search in Zotero",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = sf.query,
                        onValueChange = { newText ->
                            onUpdateFilters { it.copy(query = newText) }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { onRecordRecentSearch(sf.query) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("search_overlay_input")
                    )
                }

                if (sf.query.isNotEmpty()) {
                    IconButton(
                        onClick = { onUpdateFilters { it.copy(query = "") } },
                        modifier = Modifier.testTag("search_overlay_clear_query")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search query"
                        )
                    }
                }

                // View Type Switcher right in the Search Bar
                IconButton(
                    onClick = onToggleCompactView,
                    modifier = Modifier.testTag("search_overlay_view_switcher")
                ) {
                    Icon(
                        imageVector = if (uiState.isLibraryCompactView) Icons.Default.ViewAgenda else Icons.Default.ViewCompact,
                        contentDescription = "Switch results view density",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            // 2. Gmail-Style Outlined Dropdown Filter Chips Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Collection Filter Chip
                Box {
                    val selectedColName = uiState.collections.firstOrNull { it.key == sf.collectionKey }?.name
                    GmailDropdownFilterChip(
                        label = selectedColName ?: "Collections",
                        selected = sf.collectionKey != null,
                        onClick = { showCollectionMenu = true },
                        testTag = "search_chip_collections"
                    )
                    DropdownMenu(
                        expanded = showCollectionMenu,
                        onDismissRequest = { showCollectionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Collections") },
                            onClick = {
                                onUpdateFilters { it.copy(collectionKey = null) }
                                showCollectionMenu = false
                            }
                        )
                        uiState.collections.forEach { col ->
                            DropdownMenuItem(
                                text = { Text(col.name) },
                                onClick = {
                                    onUpdateFilters { it.copy(collectionKey = col.key) }
                                    showCollectionMenu = false
                                }
                            )
                        }
                    }
                }

                // Author Filter Chip
                Box {
                    GmailDropdownFilterChip(
                        label = sf.authorLastName?.let { "Author: $it" } ?: "Author",
                        selected = sf.authorLastName != null,
                        onClick = { showAuthorMenu = true },
                        testTag = "search_chip_author"
                    )
                    DropdownMenu(
                        expanded = showAuthorMenu,
                        onDismissRequest = { showAuthorMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Any Author") },
                            onClick = {
                                onUpdateFilters { it.copy(authorLastName = null) }
                                showAuthorMenu = false
                            }
                        )
                        distinctAuthors.forEach { author ->
                            DropdownMenuItem(
                                text = { Text(author) },
                                onClick = {
                                    onUpdateFilters { it.copy(authorLastName = author) }
                                    showAuthorMenu = false
                                }
                            )
                        }
                    }
                }

                // Year Filter Chip
                Box {
                    GmailDropdownFilterChip(
                        label = sf.year?.let { "Year: $it" } ?: "Year",
                        selected = sf.year != null,
                        onClick = { showYearMenu = true },
                        testTag = "search_chip_year"
                    )
                    DropdownMenu(
                        expanded = showYearMenu,
                        onDismissRequest = { showYearMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Any Year") },
                            onClick = {
                                onUpdateFilters { it.copy(year = null) }
                                showYearMenu = false
                            }
                        )
                        distinctYears.forEach { yr ->
                            DropdownMenuItem(
                                text = { Text(yr) },
                                onClick = {
                                    onUpdateFilters { it.copy(year = yr) }
                                    showYearMenu = false
                                }
                            )
                        }
                    }
                }

                // Item Type Filter Chip
                Box {
                    GmailDropdownFilterChip(
                        label = sf.itemType?.label ?: "Type",
                        selected = sf.itemType != null,
                        onClick = { showTypeMenu = true },
                        testTag = "search_chip_type"
                    )
                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Any Item Type") },
                            onClick = {
                                onUpdateFilters { it.copy(itemType = null) }
                                showTypeMenu = false
                            }
                        )
                        ItemType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    onUpdateFilters { it.copy(itemType = type) }
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }

                // Tag Filter Chip
                Box {
                    GmailDropdownFilterChip(
                        label = sf.tagName?.let { "#$it" } ?: "Tags",
                        selected = sf.tagName != null,
                        onClick = { showTagMenu = true },
                        testTag = "search_chip_tags"
                    )
                    DropdownMenu(
                        expanded = showTagMenu,
                        onDismissRequest = { showTagMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Any Tag") },
                            onClick = {
                                onUpdateFilters { it.copy(tagName = null) }
                                showTagMenu = false
                            }
                        )
                        uiState.allTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag.name) },
                                onClick = {
                                    onUpdateFilters { it.copy(tagName = tag.name) }
                                    showTagMenu = false
                                }
                            )
                        }
                    }
                }

                // PDF Attachment Toggle Chip
                FilterChip(
                    selected = sf.onlyPdf,
                    onClick = { onUpdateFilters { it.copy(onlyPdf = !it.onlyPdf) } },
                    label = { Text("PDF Attached") },
                    leadingIcon = if (sf.onlyPdf) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = extColors.accentPill,
                        selectedLabelColor = extColors.onAccentPill
                    ),
                    modifier = Modifier.testTag("search_chip_pdf")
                )

                if (sf.hasActiveFilter) {
                    TextButton(
                        onClick = onClearFilters,
                        modifier = Modifier.testTag("search_clear_all_filters")
                    ) {
                        Text("Clear all")
                    }
                }
            }

            // 3. Content Area: Recent Searches (when no filter is active) OR Live Filtered Results
            if (!sf.hasActiveFilter) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Recent Zotero Searches",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(uiState.recentSearches, key = { it }) { recentQuery ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onUpdateFilters { it.copy(query = recentQuery) }
                                    }
                                    .testTag("recent_search_row_$recentQuery")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = recentQuery,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onRemoveRecentSearch(recentQuery) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove recent search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val matchedItemsCount = uiState.searchMatchedItems.size
                val matchedNotesCount = uiState.searchMatchedNotes.size
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(extColors.listCanvasBg)
                        .testTag("search_results_list"),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (uiState.isLibraryCompactView) 4.dp else 6.dp)
                ) {
                    item {
                        Text(
                            text = "Matching Papers ($matchedItemsCount)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                        )
                    }

                    if (uiState.searchMatchedItems.isEmpty()) {
                        item {
                            Text(
                                text = "No papers match the selected search and filter criteria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                            )
                        }
                    } else {
                        itemsIndexed(uiState.searchMatchedItems, key = { _, it -> it.id }) { index, item ->
                            GmailStylePaperRow(
                                item = item,
                                isCompact = uiState.isLibraryCompactView,
                                shape = groupedListItemShape(index, matchedItemsCount),
                                onOpenPaperReader = {
                                    if (sf.query.isNotBlank()) onRecordRecentSearch(sf.query)
                                    onClose()
                                    onOpenPaperReader(item.id)
                                },
                                onOpenMetadataSheet = {
                                    onOpenMetadataSheet(item.id)
                                },
                                onToggleFavorite = { onToggleFavorite(item) }
                            )
                        }
                    }

                    if (uiState.searchMatchedNotes.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Matching Notes ($matchedNotesCount)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                            )
                        }

                        itemsIndexed(uiState.searchMatchedNotes, key = { _, it -> it.id }) { index, note ->
                            val noteShape = groupedListItemShape(index, matchedNotesCount)
                            Surface(
                                shape = noteShape,
                                color = extColors.listCardSurface,
                                tonalElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(noteShape)
                                    .clickable {
                                        note.parentItemId?.let {
                                            onClose()
                                            onOpenPaperReader(it)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
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

@Composable
private fun GmailDropdownFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val extColors = ZoteroThemeTokens.colors
    Surface(
        color = if (selected) extColors.accentPill else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) extColors.onAccentPill.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) extColors.onAccentPill else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (selected) extColors.onAccentPill else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
