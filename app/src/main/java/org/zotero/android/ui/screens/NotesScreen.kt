package org.zotero.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.data.model.ZoteroItem
import org.zotero.android.data.model.ZoteroNote
import org.zotero.android.ui.NotesFilter
import org.zotero.android.ui.ZoteroUiState
import org.zotero.android.ui.components.SpeedDialFabMenu
import org.zotero.android.ui.theme.ZoteroThemeTokens

@Composable
fun NotesScreen(
    uiState: ZoteroUiState,
    onOpenDrawer: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSetNotesFilter: (NotesFilter) -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenPaperReader: (Int) -> Unit,
    onAddPaperNote: (ZoteroItem, String, String) -> Unit,
    onSaveStandaloneNote: (String?, String, String) -> Unit,
    onDeleteNote: (ZoteroNote) -> Unit,
    onShowIdentifierDialog: () -> Unit,
    onShowCreateItemDialog: () -> Unit
) {
    val extColors = ZoteroThemeTokens.colors
    var showCreateNoteDialog by rememberSaveable { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<ZoteroNote?>(null) }
    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Google Keep Style Search Bar with Bento Grid / Single-Column List Switcher on the right
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(50.dp)
                    .testTag("notes_search_bar"),
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
                        modifier = Modifier.testTag("notes_open_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Collections Drawer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (uiState.notesSearchQuery.isEmpty()) {
                            Text(
                                text = "Search your notes",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = uiState.notesSearchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notes_search_input")
                        )
                    }

                    if (uiState.notesSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // View Type Switcher right in the Search Bar (2-Column Bento Grid vs Single-Column List)
                    IconButton(
                        onClick = onToggleViewMode,
                        modifier = Modifier.testTag("notes_view_mode_switcher")
                    ) {
                        Icon(
                            imageVector = if (uiState.isNotesBentoGrid) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = if (uiState.isNotesBentoGrid) "Switch to single-column list" else "Switch to Bento grid",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Filter Chip Row (All Notes | Paper Notes | Standalone)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NotesFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.notesFilter == filter,
                        onClick = { onSetNotesFilter(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extColors.accentPill,
                            selectedLabelColor = extColors.onAccentPill
                        ),
                        modifier = Modifier.testTag("notes_filter_${filter.name}")
                    )
                }
            }

            // 3. Google Keep Bento Grid (2-column staggered masonry) or Single-Column List
            if (uiState.unifiedNotes.isEmpty()) {
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
                            imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = "No research notes found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap the + button to write a standalone research note or attach a note to any paper in your library.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.isNotesBentoGrid) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .testTag("notes_bento_staggered_grid"),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp
                ) {
                    itemsIndexed(uiState.unifiedNotes, key = { _, n -> n.id }) { idx, note ->
                        KeepBentoNoteCard(
                            note = note,
                            colorIndex = idx,
                            onEditNote = {
                                editingNote = note
                                showCreateNoteDialog = true
                            },
                            onOpenLinkedPaper = {
                                note.parentItemId?.let { onOpenPaperReader(it) }
                            },
                            onCopyNote = {
                                clipboard.setText(AnnotatedString("${note.title}\n\n${note.content}"))
                            },
                            onDeleteNote = { onDeleteNote(note) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .testTag("notes_single_column_list"),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(uiState.unifiedNotes, key = { _, n -> n.id }) { idx, note ->
                        KeepBentoNoteCard(
                            note = note,
                            colorIndex = idx,
                            onEditNote = {
                                editingNote = note
                                showCreateNoteDialog = true
                            },
                            onOpenLinkedPaper = {
                                note.parentItemId?.let { onOpenPaperReader(it) }
                            },
                            onCopyNote = {
                                clipboard.setText(AnnotatedString("${note.title}\n\n${note.content}"))
                            },
                            onDeleteNote = { onDeleteNote(note) }
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
            onQuickNote = {
                editingNote = null
                showCreateNoteDialog = true
            }
        )
    }

    if (showCreateNoteDialog) {
        val activePapers = uiState.allItems.filter { !it.isInTrash }
        var title by rememberSaveable(editingNote?.id) { mutableStateOf(editingNote?.title ?: "") }
        var content by rememberSaveable(editingNote?.id) { mutableStateOf(editingNote?.content ?: "") }
        var selectedPaperId by rememberSaveable(editingNote?.id) {
            mutableStateOf<Int?>(editingNote?.parentItemId)
        }

        AlertDialog(
            onDismissRequest = { showCreateNoteDialog = false },
            title = { Text(if (editingNote == null) "New Research Note" else "Edit Research Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Note Title") },
                        placeholder = { Text("e.g. Literature Synthesis & Experimental Setup") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_dialog_title_input")
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Note Content *") },
                        minLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_dialog_content_input")
                    )

                    if (editingNote == null && activePapers.isNotEmpty()) {
                        Text(
                            text = "Attach to paper (or leave as Standalone):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedPaperId == null,
                                onClick = { selectedPaperId = null },
                                label = { Text("Standalone Note") }
                            )
                            activePapers.forEach { paper ->
                                FilterChip(
                                    selected = selectedPaperId == paper.id,
                                    onClick = { selectedPaperId = paper.id },
                                    label = { Text(paper.creatorSummary + " (" + paper.year + ")") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val existing = editingNote
                        if (existing != null && existing.parentItemId == null) {
                            onSaveStandaloneNote(existing.id, title, content)
                        } else if (selectedPaperId != null) {
                            val targetPaper = activePapers.firstOrNull { it.id == selectedPaperId }
                            if (targetPaper != null) {
                                onAddPaperNote(targetPaper, title, content)
                            }
                        } else {
                            onSaveStandaloneNote(null, title, content)
                        }
                        showCreateNoteDialog = false
                    },
                    enabled = content.isNotBlank() || title.isNotBlank(),
                    modifier = Modifier.testTag("confirm_save_note_button")
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun KeepBentoNoteCard(
    note: ZoteroNote,
    colorIndex: Int,
    onEditNote: () -> Unit,
    onOpenLinkedPaper: () -> Unit,
    onCopyNote: () -> Unit,
    onDeleteNote: () -> Unit
) {
    val extColors = ZoteroThemeTokens.colors
    val palette = extColors.bentoNotePalette
    val cardBgColor = palette[colorIndex % palette.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditNote)
            .testTag("bento_note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                maxLines = 7,
                overflow = TextOverflow.Ellipsis
            )

            // Linked Paper Badge (or Standalone Pill)
            if (note.parentItemTitle != null && note.parentItemId != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenLinkedPaper)
                        .testTag("note_open_paper_${note.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = note.parentItemTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.updatedAt,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onCopyNote, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(onClick = onDeleteNote, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
