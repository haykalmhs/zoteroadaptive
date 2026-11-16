package org.zotero.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.data.model.PdfAnnotation
import org.zotero.android.data.model.ZoteroItem
import org.zotero.android.ui.components.parseHexColor
import org.zotero.android.ui.theme.CoralPdfBg
import org.zotero.android.ui.theme.CoralPdfText
import org.zotero.android.ui.theme.OnTealAccentPill
import org.zotero.android.ui.theme.TealAccentPill
import org.zotero.android.ui.theme.VenueChipBg
import org.zotero.android.ui.theme.VenueChipText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaperReaderScreen(
    item: ZoteroItem,
    onBack: () -> Unit,
    onOpenMetadataSheet: () -> Unit,
    onOpenCiteSheet: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddAnnotation: (pageLabel: String, highlightedText: String, comment: String, colorHex: String) -> Unit,
    onDeleteAnnotation: (annotationId: String) -> Unit,
    onAddNote: (title: String, content: String) -> Unit
) {
    BackHandler(onBack = onBack)

    var showHighlightDialog by rememberSaveable { mutableStateOf(false) }
    var prefillHighlightText by rememberSaveable { mutableStateOf("") }
    var prefillHighlightPage by rememberSaveable { mutableStateOf("p. 1") }
    var showQuickNoteDialog by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("paper_reader_screen"),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Library"
                        )
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.clickable { onOpenMetadataSheet() }
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.creatorSummary} (${item.year}) • Tap for Metadata",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Cite & Export button in Reader Top Bar (per user confirmation)
                    IconButton(
                        onClick = onOpenCiteSheet,
                        modifier = Modifier.testTag("reader_top_cite_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Cite & Export"
                        )
                    }

                    // Quick Note button
                    IconButton(
                        onClick = { showQuickNoteDialog = true },
                        modifier = Modifier.testTag("reader_top_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = "Add Research Note"
                        )
                    }

                    // Bookmark button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("reader_top_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark Paper"
                        )
                    }

                    // Paper Information & Metadata Bottom Sheet trigger (per user requirement)
                    IconButton(
                        onClick = onOpenMetadataSheet,
                        modifier = Modifier.testTag("reader_top_info_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Paper Information & Metadata",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    prefillHighlightText = ""
                    prefillHighlightPage = "p. 1"
                    showHighlightDialog = true
                },
                containerColor = TealAccentPill,
                contentColor = OnTealAccentPill,
                icon = {
                    Icon(Icons.Default.BorderColor, contentDescription = null)
                },
                text = { Text("Highlight") },
                modifier = Modifier.testTag("reader_add_highlight_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Manuscript Title & Header Block
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (item.hasPdfAttachment) {
                            Surface(color = CoralPdfBg, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "PDF READING MODE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CoralPdfText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (item.displayVenueBadge.isNotBlank()) {
                            Surface(color = VenueChipBg, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = item.displayVenueBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VenueChipText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (item.doi.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "DOI: ${item.doi}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = item.creators.joinToString(", ") { it.fullName },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick Action Pill Row inside Reader (Paper Info Sheet + Cite & Export)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenMetadataSheet,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("reader_inline_metadata_button")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Info & Metadata")
                        }
                        OutlinedButton(
                            onClick = onOpenCiteSheet,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("reader_inline_cite_button")
                        ) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cite & Export")
                        }
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // 2. Abstract Block
            if (item.abstractNote.isNotBlank()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ABSTRACT",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.abstractNote,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 3. Full-Text Manuscript Sections (tappable paragraphs to quick-highlight)
            items(item.sections) { section ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = section.heading,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = section.pageRange,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    section.paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    prefillHighlightText = paragraph
                                    prefillHighlightPage = section.pageRange
                                    showHighlightDialog = true
                                }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // 4. Highlights & Annotations Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Highlights & Marginalia (${item.annotations.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap any paragraph above to highlight",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.annotations.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No highlights yet. Tap any paragraph in the paper or use the Highlight button to save an annotated passage.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(item.annotations, key = { it.id }) { annotation ->
                    ReaderAnnotationCard(
                        annotation = annotation,
                        item = item,
                        onConvertToNote = {
                            onAddNote(
                                "${item.creatorSummary} (${annotation.pageLabel}) Highlight",
                                "\"${annotation.highlightedText}\" (${item.creatorSummary}, ${item.year}, ${annotation.pageLabel})\n\nNote: ${annotation.comment}"
                            )
                        },
                        onCopyQuote = {
                            val formatted = "\"${annotation.highlightedText}\" (${item.creatorSummary}, ${item.year}, ${annotation.pageLabel})"
                            clipboard.setText(AnnotatedString(formatted))
                        },
                        onDelete = { onDeleteAnnotation(annotation.id) }
                    )
                }
            }
        }
    }

    if (showHighlightDialog) {
        var pageLabel by rememberSaveable(prefillHighlightPage) { mutableStateOf(prefillHighlightPage) }
        var highlightedText by rememberSaveable(prefillHighlightText) { mutableStateOf(prefillHighlightText) }
        var comment by rememberSaveable { mutableStateOf("") }
        var selectedColor by rememberSaveable { mutableStateOf("#FFD400") }
        val highlightColors = listOf(
            "#FFD400" to "Yellow",
            "#FF6666" to "Coral",
            "#5FB236" to "Green",
            "#2EA8E5" to "Blue",
            "#A28AE5" to "Purple"
        )

        AlertDialog(
            onDismissRequest = { showHighlightDialog = false },
            title = { Text("Highlight Passage") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pageLabel,
                        onValueChange = { pageLabel = it },
                        label = { Text("Page Label") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("annotation_page_input")
                    )
                    OutlinedTextField(
                        value = highlightedText,
                        onValueChange = { highlightedText = it },
                        label = { Text("Highlighted Passage *") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("annotation_text_input")
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Comment / Marginalia") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("annotation_comment_input")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color:", style = MaterialTheme.typography.bodySmall)
                        highlightColors.forEach { (hex, _) ->
                            val swatch = parseHexColor(hex) ?: Color.Yellow
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddAnnotation(pageLabel, highlightedText, comment, selectedColor)
                        showHighlightDialog = false
                    },
                    enabled = highlightedText.isNotBlank(),
                    modifier = Modifier.testTag("confirm_add_annotation_button")
                ) {
                    Text("Save Highlight")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHighlightDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQuickNoteDialog) {
        var noteTitle by rememberSaveable { mutableStateOf("") }
        var noteContent by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuickNoteDialog = false },
            title = { Text("Add Note to Paper") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Note Heading") },
                        placeholder = { Text("Key findings & critique") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_note_title_input")
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Note Content *") },
                        minLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_note_content_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddNote(noteTitle, noteContent)
                        showQuickNoteDialog = false
                    },
                    enabled = noteContent.isNotBlank() || noteTitle.isNotBlank(),
                    modifier = Modifier.testTag("confirm_reader_note_button")
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReaderAnnotationCard(
    annotation: PdfAnnotation,
    item: ZoteroItem,
    onConvertToNote: () -> Unit,
    onCopyQuote: () -> Unit,
    onDelete: () -> Unit
) {
    val barColor = parseHexColor(annotation.colorHex) ?: Color(0xFFFFD400)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("annotation_card_${annotation.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                    Text(
                        text = annotation.pageLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(onClick = onCopyQuote, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy quote",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete highlight",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = barColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "\"${annotation.highlightedText}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (annotation.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = annotation.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onConvertToNote,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_highlight_to_note_${annotation.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save to Notes", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
