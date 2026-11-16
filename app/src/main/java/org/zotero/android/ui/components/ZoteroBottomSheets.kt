package org.zotero.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.zotero.android.data.model.CitationFormatter
import org.zotero.android.data.model.CitationStyle
import org.zotero.android.data.model.ItemType
import org.zotero.android.data.model.ZoteroCollection
import org.zotero.android.data.model.ZoteroItem

fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemMetadataBottomSheet(
    item: ZoteroItem,
    allCollections: List<ZoteroCollection>,
    onDismiss: () -> Unit,
    onEditMetadata: () -> Unit,
    onOpenCiteSheet: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCollection: (String) -> Unit,
    onAddTag: (String, String?) -> Unit,
    onRemoveTag: (String) -> Unit,
    onMoveToTrash: () -> Unit,
    onRestoreFromTrash: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newTagName by remember { mutableStateOf("") }
    var selectedTagColor by remember { mutableStateOf<String?>("#059669") }
    val tagColors = listOf("#059669", "#1E88E5", "#E53935", "#FB8C00", "#8E24AA", "#43A047", null)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("item_metadata_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row: Item Type pill + Action buttons (Cite & Export, Edit, Star)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${item.itemType.label} • ${item.itemKey}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("metadata_sheet_star_button")
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onEditMetadata,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("metadata_sheet_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }

                    Button(
                        onClick = onOpenCiteSheet,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("metadata_sheet_cite_button")
                    ) {
                        Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cite & Export")
                    }
                }
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Creators list
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item.creators.forEach { creator ->
                    Text(
                        text = "${creator.creatorType.replaceFirstChar { it.uppercase() }}: ${creator.lastName}, ${creator.firstName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bibliographic Information Grid
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = "Paper Information & Metadata",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.publicationTitle.isNotBlank()) {
                        MetadataFieldRow("Publication", item.publicationTitle)
                    }
                    if (item.venueShort.isNotBlank()) {
                        MetadataFieldRow("Venue Badge", item.venueShort, isMono = true)
                    }
                    if (item.year.isNotBlank()) {
                        MetadataFieldRow("Year", item.year)
                    }
                    if (item.citationCount.isNotBlank()) {
                        MetadataFieldRow("Citations", item.citationCount)
                    }
                    if (item.volume.isNotBlank() || item.issue.isNotBlank() || item.pages.isNotBlank()) {
                        MetadataFieldRow(
                            "Pages / Vol",
                            listOfNotNull(
                                item.volume.takeIf { it.isNotBlank() }?.let { "Vol. $it" },
                                item.issue.takeIf { it.isNotBlank() }?.let { "No. $it" },
                                item.pages.takeIf { it.isNotBlank() }?.let { "pp. $it" }
                            ).joinToString(", ")
                        )
                    }
                    if (item.doi.isNotBlank()) {
                        MetadataFieldRow("DOI", item.doi, isMono = true)
                    }
                    if (item.isbn.isNotBlank()) {
                        MetadataFieldRow("ISBN", item.isbn, isMono = true)
                    }
                    MetadataFieldRow("Citekey", item.citeKey, isMono = true)
                    MetadataFieldRow("Attachment", if (item.hasPdfAttachment) "Full-Text PDF Attached" else "Metadata Only")
                }
            }

            if (item.abstractNote.isNotBlank()) {
                Text(
                    text = "Abstract",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.abstractNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Collections Membership
            if (allCollections.isNotEmpty()) {
                Text(
                    text = "Collections",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allCollections.forEach { col ->
                        val inCollection = col.key in item.collectionKeys
                        FilterChip(
                            selected = inCollection,
                            onClick = { onToggleCollection(col.key) },
                            label = { Text(col.name) }
                        )
                    }
                }
            }

            // Tags & Colored Tag Editor
            Text(
                text = "Tags (${item.tags.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.tags.forEach { tag ->
                    val tagColor = parseHexColor(tag.colorHex)
                    AssistChip(
                        onClick = { onRemoveTag(tag.name) },
                        label = { Text("${tag.name} ×") },
                        leadingIcon = if (tagColor != null) {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(tagColor)
                                )
                            }
                        } else null
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { Text("Add tag (e.g. Open Access)…") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metadata_new_tag_input")
                )
                FilledTonalButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            onAddTag(newTagName, selectedTagColor)
                            newTagName = ""
                        }
                    },
                    modifier = Modifier.testTag("metadata_add_tag_button")
                ) {
                    Text("Add")
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tag color:", style = MaterialTheme.typography.bodySmall)
                tagColors.forEach { hex ->
                    val color = parseHexColor(hex) ?: MaterialTheme.colorScheme.outline
                    val isSelected = selectedTagColor == hex
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedTagColor = hex }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isInTrash) {
                    OutlinedButton(
                        onClick = onRestoreFromTrash,
                        modifier = Modifier.testTag("restore_item_button")
                    ) {
                        Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDeletePermanently,
                        modifier = Modifier.testTag("delete_permanent_button")
                    ) {
                        Text("Delete Forever")
                    }
                } else {
                    TextButton(
                        onClick = onMoveToTrash,
                        modifier = Modifier.testTag("move_to_trash_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Move to Trash", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataFieldRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(105.dp)
        )
        Text(
            text = value,
            style = if (isMono) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitationBottomSheet(
    item: ZoteroItem,
    selectedStyle: CitationStyle,
    onSelectStyle: (CitationStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var copiedMessage by remember { mutableStateOf<String?>(null) }

    val inTextCitation = remember(item, selectedStyle) {
        CitationFormatter.formatInTextCitation(item, selectedStyle, 1)
    }
    val bibliographyEntry = remember(item, selectedStyle) {
        CitationFormatter.formatBibliography(item, selectedStyle, 1)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("citation_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cite & Export",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Style selector strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CitationStyle.entries.forEach { style ->
                    FilterChip(
                        selected = selectedStyle == style,
                        onClick = {
                            onSelectStyle(style)
                            copiedMessage = null
                        },
                        label = { Text(style.shortLabel) },
                        modifier = Modifier.testTag("cite_sheet_style_${style.name}")
                    )
                }
            }

            if (copiedMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = copiedMessage!!,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // In-text Citation Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "In-Text Citation (${selectedStyle.shortLabel})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = inTextCitation,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(inTextCitation))
                            copiedMessage = "Copied in-text citation: $inTextCitation"
                        },
                        modifier = Modifier.testTag("copy_intext_citation_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                }
            }

            // Full Bibliography / Export Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedStyle.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(bibliographyEntry))
                                copiedMessage = "Copied ${selectedStyle.shortLabel} entry to clipboard"
                            },
                            modifier = Modifier.testTag("copy_bibliography_entry_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Entry")
                        }
                    }

                    Text(
                        text = bibliographyEntry,
                        style = if (selectedStyle == CitationStyle.BIBTEX || selectedStyle == CitationStyle.RIS) {
                            MaterialTheme.typography.labelSmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun IdentifierLookupDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onLookup: (String) -> Unit
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    val sampleIdentifiers = listOf(
        "10.1038/s41586-021-03819-2" to "AlphaFold (Nature DOI)",
        "arXiv:2005.11401" to "RAG (arXiv ID)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Paper by Identifier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter a DOI, ISBN, or arXiv ID to automatically fetch paper metadata.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("DOI, ISBN, or arXiv ID") },
                    placeholder = { Text("e.g. 10.1038/s41586-021-03819-2") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("identifier_input")
                )
                sampleIdentifiers.forEach { (idVal, label) ->
                    AssistChip(
                        onClick = { identifier = idVal },
                        label = { Text("$label: $idVal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AnimatedVisibility(visible = isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Resolving metadata…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLookup(identifier) },
                enabled = identifier.isNotBlank() && !isLoading,
                modifier = Modifier.testTag("confirm_identifier_lookup_button")
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditItemDialog(
    existingItem: ZoteroItem?,
    collections: List<ZoteroCollection>,
    defaultCollectionKey: String?,
    onDismiss: () -> Unit,
    onSave: (
        itemType: ItemType,
        title: String,
        creatorsText: String,
        year: String,
        publicationTitle: String,
        venueShort: String,
        volume: String,
        issue: String,
        pages: String,
        doi: String,
        isbn: String,
        url: String,
        abstractNote: String,
        tagsText: String,
        selectedCollectionKeys: List<String>
    ) -> Unit
) {
    var itemType by remember { mutableStateOf(existingItem?.itemType ?: ItemType.CONFERENCE_PAPER) }
    var title by rememberSaveable { mutableStateOf(existingItem?.title ?: "") }
    var creatorsText by rememberSaveable {
        mutableStateOf(
            existingItem?.creators?.joinToString("; ") { "${it.lastName}, ${it.firstName}" }
                ?: ""
        )
    }
    var year by rememberSaveable { mutableStateOf(existingItem?.year ?: "2025") }
    var publicationTitle by rememberSaveable { mutableStateOf(existingItem?.publicationTitle ?: "") }
    var venueShort by rememberSaveable { mutableStateOf(existingItem?.venueShort ?: "") }
    var volume by rememberSaveable { mutableStateOf(existingItem?.volume ?: "") }
    var issue by rememberSaveable { mutableStateOf(existingItem?.issue ?: "") }
    var pages by rememberSaveable { mutableStateOf(existingItem?.pages ?: "") }
    var doi by rememberSaveable { mutableStateOf(existingItem?.doi ?: "") }
    var isbn by rememberSaveable { mutableStateOf(existingItem?.isbn ?: "") }
    var url by rememberSaveable { mutableStateOf(existingItem?.url ?: "") }
    var abstractNote by rememberSaveable { mutableStateOf(existingItem?.abstractNote ?: "") }
    var tagsText by rememberSaveable {
        mutableStateOf(existingItem?.tags?.joinToString(", ") { it.name } ?: "")
    }
    var selectedCols by remember {
        mutableStateOf(
            existingItem?.collectionKeys
                ?: listOfNotNull(defaultCollectionKey)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingItem == null) "New Research Paper" else "Edit Paper Metadata") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Item Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ItemType.entries.forEach { type ->
                        FilterChip(
                            selected = itemType == type,
                            onClick = { itemType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_title_input")
                )

                OutlinedTextField(
                    value = creatorsText,
                    onValueChange = { creatorsText = it },
                    label = { Text("Authors (LastName, FirstName; …)") },
                    placeholder = { Text("Hu, Edward J.; Shen, Yelong") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_creators_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Year") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = venueShort,
                        onValueChange = { venueShort = it },
                        label = { Text("Venue Badge") },
                        placeholder = { Text("NeurIPS '24") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                OutlinedTextField(
                    value = publicationTitle,
                    onValueChange = { publicationTitle = it },
                    label = { Text("Full Journal / Conference Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = doi,
                    onValueChange = { doi = it },
                    label = { Text("DOI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Tags (comma-separated)") },
                    placeholder = { Text("Open Access, PEFT") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = abstractNote,
                    onValueChange = { abstractNote = it },
                    label = { Text("Abstract / Summary") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (collections.isNotEmpty()) {
                    Text("Collections", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        collections.forEach { col ->
                            val isSelected = col.key in selectedCols
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCols = if (isSelected) selectedCols - col.key else selectedCols + col.key
                                },
                                label = { Text(col.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        itemType,
                        title,
                        creatorsText,
                        year,
                        publicationTitle,
                        venueShort,
                        volume,
                        issue,
                        pages,
                        doi,
                        isbn,
                        url,
                        abstractNote,
                        tagsText,
                        selectedCols
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_item_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
