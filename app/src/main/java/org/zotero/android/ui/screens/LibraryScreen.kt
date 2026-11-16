package org.zotero.android.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.data.model.ItemType
import org.zotero.android.data.model.SortField
import org.zotero.android.data.model.ZoteroItem
import org.zotero.android.ui.LibraryFilter
import org.zotero.android.ui.ZoteroUiState
import org.zotero.android.ui.components.SpeedDialFabMenu
import org.zotero.android.ui.theme.CoralPdfBg
import org.zotero.android.ui.theme.CoralPdfText
import org.zotero.android.ui.theme.IconTileBg
import org.zotero.android.ui.theme.IconTileTint
import org.zotero.android.ui.theme.OpenAccessBg
import org.zotero.android.ui.theme.OpenAccessText
import org.zotero.android.ui.theme.VenueChipBg
import org.zotero.android.ui.theme.VenueChipText

fun itemTypeLeadingIcon(itemType: ItemType): ImageVector = when (itemType) {
    ItemType.BOOK, ItemType.THESIS -> Icons.AutoMirrored.Filled.MenuBook
    ItemType.PREPRINT, ItemType.REPORT -> Icons.Default.Description
    else -> Icons.AutoMirrored.Filled.Article
}

@Composable
fun LibraryScreen(
    uiState: ZoteroUiState,
    onOpenDrawer: () -> Unit,
    onOpenFullScreenSearch: () -> Unit,
    onToggleCompactView: () -> Unit,
    onSelectCollection: (String) -> Unit,
    onClearTagFilter: () -> Unit,
    onSetSortField: (SortField) -> Unit,
    onOpenPaperReader: (Int) -> Unit,
    onOpenMetadataSheet: (Int) -> Unit,
    onToggleFavorite: (ZoteroItem) -> Unit,
    onEmptyTrash: () -> Unit,
    onShowIdentifierDialog: () -> Unit,
    onShowCreateItemDialog: () -> Unit,
    onShowQuickNoteDialog: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var fabExpanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Gmail-Style Floating Search Bar (tapping opens Full-Screen Search Overlay; right icon switches view density)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onOpenFullScreenSearch() }
                    .testTag("gmail_search_bar"),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("open_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Collections Drawer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (uiState.searchFilters.hasActiveFilter && uiState.searchFilters.query.isNotBlank()) {
                                uiState.searchFilters.query
                            } else {
                                "Search in Zotero..."
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // View Density Switcher inside Search Bar (Comfortable vs Compact)
                    IconButton(
                        onClick = onToggleCompactView,
                        modifier = Modifier.testTag("library_view_mode_switcher")
                    ) {
                        Icon(
                            imageVector = if (uiState.isLibraryCompactView) Icons.Default.ViewAgenda else Icons.Default.ViewCompact,
                            contentDescription = "Switch list density",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Breadcrumb & Sort Row ("Final Year Project > Signal Processing > Classification")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                        .testTag("collection_breadcrumb_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onOpenDrawer() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    uiState.breadcrumbPath.forEachIndexed { idx, (colKey, colName) ->
                        val isLast = idx == uiState.breadcrumbPath.lastIndex
                        Text(
                            text = colName,
                            style = if (isLast) {
                                MaterialTheme.typography.titleSmall.copy(fontSize = 14.5.sp)
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isLast) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            modifier = Modifier.clickable {
                                if (!isLast) onSelectCollection(colKey) else onOpenDrawer()
                            }
                        )
                        if (!isLast) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(16.dp)
                            )
                        }
                    }

                    if (uiState.activeTagFilter != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = OpenAccessBg,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onClearTagFilter() }
                        ) {
                            Text(
                                text = "#${uiState.activeTagFilter} ×",
                                style = MaterialTheme.typography.labelMedium,
                                color = OpenAccessText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("sort_menu_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort papers",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortField.entries.forEach { field ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = field.label,
                                        fontWeight = if (uiState.sortField == field) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSetSortField(field)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.activeCollectionKey == LibraryFilter.TRASH.key && uiState.filteredItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trash (${uiState.filteredItems.size} items)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onEmptyTrash,
                        modifier = Modifier.testTag("empty_trash_button")
                    ) {
                        Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            // 3. Divider-Separated Gmail-Style Paper List
            if (uiState.filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = "No papers in ${uiState.activeCollectionLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap the + button to import a paper by DOI/arXiv ID or switch collections in the drawer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.activeTagFilter != null) {
                            OutlinedButton(
                                onClick = onClearTagFilter,
                                modifier = Modifier.testTag("reset_filters_button")
                            ) {
                                Text("Clear Tag Filter")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library_item_list"),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(uiState.filteredItems, key = { it.id }) { item ->
                        GmailStylePaperRow(
                            item = item,
                            isCompact = uiState.isLibraryCompactView,
                            onOpenPaperReader = { onOpenPaperReader(item.id) },
                            onOpenMetadataSheet = { onOpenMetadataSheet(item.id) },
                            onToggleFavorite = { onToggleFavorite(item) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                            thickness = 0.8.dp
                        )
                    }
                }
            }
        }

        // 4. Animated Speed-Dial FAB Menu ("+")
        SpeedDialFabMenu(
            expanded = fabExpanded,
            onToggleExpanded = { fabExpanded = !fabExpanded },
            onDismiss = { fabExpanded = false },
            onAddByIdentifier = onShowIdentifierDialog,
            onManualPaper = onShowCreateItemDialog,
            onQuickNote = onShowQuickNoteDialog
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun GmailStylePaperRow(
    item: ZoteroItem,
    isCompact: Boolean = false,
    onOpenPaperReader: () -> Unit,
    onOpenMetadataSheet: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpenPaperReader,
                onLongClick = onOpenMetadataSheet
            )
            .padding(
                horizontal = 16.dp,
                vertical = if (isCompact) 10.dp else 14.dp
            )
            .testTag("item_row_${item.itemKey}"),
        verticalAlignment = Alignment.Top
    ) {
        // Left rounded-square icon tile
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(if (isCompact) 34.dp else 40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IconTileBg)
                .clickable(onClick = onOpenMetadataSheet)
                .testTag("item_icon_info_${item.itemKey}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = itemTypeLeadingIcon(item.itemType),
                contentDescription = "Paper Metadata",
                tint = IconTileTint,
                modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center content column: Title + Author list + Badges
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (isCompact) 14.5.sp else 15.5.sp,
                        lineHeight = if (isCompact) 19.sp else 21.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                // Right column: Year + optional bookmark icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.year.isNotBlank()) {
                        Text(
                            text = item.year,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (item.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Starred",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onToggleFavorite() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Author surnames line ("Hu, Shen, Wallis, Allen-Zhu...")
            Text(
                text = item.creatorListLine,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))

            // Compact Badge Strip: [PDF] [Venue] [Open Access / Tags] [99 Citations] + Info button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (item.hasPdfAttachment) {
                        Surface(
                            color = CoralPdfBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "PDF",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = CoralPdfText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    if (item.displayVenueBadge.isNotBlank()) {
                        Surface(
                            color = VenueChipBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.displayVenueBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = VenueChipText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    // Show "Open Access" pill if tagged
                    item.tags.firstOrNull { it.name.equals("Open Access", ignoreCase = true) }?.let { oaTag ->
                        Surface(
                            color = OpenAccessBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = oaTag.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = OpenAccessText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    // Citation count ("99 3.2k")
                    if (item.citationCount.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = "Citations",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.citationCount,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Subtle bookmark & info icon buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!item.isFavorite) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("bookmark_row_${item.itemKey}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark paper",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenMetadataSheet,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("info_sheet_button_${item.itemKey}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Paper Information & Metadata",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
