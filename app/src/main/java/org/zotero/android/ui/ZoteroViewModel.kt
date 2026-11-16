package org.zotero.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.zotero.android.data.local.ZoteroDatabase
import org.zotero.android.data.model.CitationStyle
import org.zotero.android.data.model.Creator
import org.zotero.android.data.model.ItemType
import org.zotero.android.data.model.PaperSection
import org.zotero.android.data.model.PdfAnnotation
import org.zotero.android.data.model.SortField
import org.zotero.android.data.model.ZoteroCollection
import org.zotero.android.data.model.ZoteroItem
import org.zotero.android.data.model.ZoteroNote
import org.zotero.android.data.model.ZoteroTag
import org.zotero.android.data.repository.ZoteroRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class LibraryFilter(val key: String, val label: String) {
    ALL("SYSTEM_ALL", "All Items"),
    FAVORITES("SYSTEM_FAVORITES", "Starred"),
    UNFILED("SYSTEM_UNFILED", "Unfiled Items"),
    DUPLICATES("SYSTEM_DUPLICATES", "Duplicate Items"),
    TRASH("SYSTEM_TRASH", "Trash")
}

enum class NotesFilter(val label: String) {
    ALL("All Notes"),
    PAPER_NOTES("Paper Notes"),
    STANDALONE("Standalone")
}

data class SearchFilters(
    val query: String = "",
    val collectionKey: String? = null,
    val authorLastName: String? = null,
    val year: String? = null,
    val itemType: ItemType? = null,
    val tagName: String? = null,
    val onlyPdf: Boolean = false
) {
    val hasActiveFilter: Boolean
        get() = query.isNotBlank() ||
            collectionKey != null ||
            authorLastName != null ||
            year != null ||
            itemType != null ||
            tagName != null ||
            onlyPdf
}

data class ZoteroUiState(
    val allItems: List<ZoteroItem> = emptyList(),
    val filteredItems: List<ZoteroItem> = emptyList(),
    val collections: List<ZoteroCollection> = emptyList(),
    val allTags: List<ZoteroTag> = emptyList(),
    val unifiedNotes: List<ZoteroNote> = emptyList(),
    val activeCollectionKey: String = "COL_CLASSIFICATION",
    val breadcrumbPath: List<Pair<String, String>> = listOf(
        "COL_FYP" to "Final Year Project",
        "COL_CLASSIFICATION" to "Classification"
    ),
    val activeCollectionLabel: String = "Classification",
    val activeTagFilter: String? = null,
    val activeTypeFilter: ItemType? = null,
    val searchQuery: String = "",
    val notesSearchQuery: String = "",
    val notesFilter: NotesFilter = NotesFilter.ALL,
    val isNotesBentoGrid: Boolean = true,
    val isLibraryCompactView: Boolean = false,
    val sortField: SortField = SortField.DATE_ADDED,
    val isSearchScreenOpen: Boolean = false,
    val searchFilters: SearchFilters = SearchFilters(),
    val searchMatchedItems: List<ZoteroItem> = emptyList(),
    val searchMatchedNotes: List<ZoteroNote> = emptyList(),
    val recentSearches: List<String> = listOf(
        "LoRA low-rank adaptation",
        "volcano-seismic transfer learning",
        "Constitutional AI",
        "seismo-acoustic plume height"
    ),
    val readingItemId: Int? = null,
    val metadataSheetItemId: Int? = null,
    val citationSheetItemId: Int? = null,
    val citationStyle: CitationStyle = CitationStyle.APA_7,
    val isLookingUpIdentifier: Boolean = false,
    val statusBannerMessage: String? = null
) {
    val readingItem: ZoteroItem?
        get() = readingItemId?.let { id -> allItems.firstOrNull { it.id == id } }

    val metadataSheetItem: ZoteroItem?
        get() = metadataSheetItemId?.let { id -> allItems.firstOrNull { it.id == id } }

    val citationSheetItem: ZoteroItem?
        get() = citationSheetItemId?.let { id -> allItems.firstOrNull { it.id == id } }
}

private data class FilterParams(
    val collectionKey: String,
    val tagFilter: String?,
    val typeFilter: ItemType?,
    val query: String,
    val sort: SortField
)

private data class ModalAndViewParams(
    val readingItemId: Int?,
    val metadataSheetItemId: Int?,
    val citationSheetItemId: Int?,
    val citationStyle: CitationStyle,
    val notesSearchQuery: String,
    val notesFilter: NotesFilter,
    val isNotesBentoGrid: Boolean,
    val isLibraryCompactView: Boolean,
    val isSearchScreenOpen: Boolean,
    val searchFilters: SearchFilters,
    val recentSearches: List<String>,
    val isLookingUpIdentifier: Boolean,
    val statusBannerMessage: String?
)

class ZoteroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ZoteroRepository =
        ZoteroRepository(ZoteroDatabase.getInstance(application).zoteroDao())

    private val _activeCollectionKey = MutableStateFlow("COL_CLASSIFICATION")
    private val _activeTagFilter = MutableStateFlow<String?>(null)
    private val _activeTypeFilter = MutableStateFlow<ItemType?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _notesSearchQuery = MutableStateFlow("")
    private val _notesFilter = MutableStateFlow(NotesFilter.ALL)
    private val _isNotesBentoGrid = MutableStateFlow(true)
    private val _isLibraryCompactView = MutableStateFlow(false)
    private val _sortField = MutableStateFlow(SortField.DATE_ADDED)
    private val _isSearchScreenOpen = MutableStateFlow(false)
    private val _searchFilters = MutableStateFlow(SearchFilters())
    private val _recentSearches = MutableStateFlow(
        listOf(
            "LoRA low-rank adaptation",
            "volcano-seismic transfer learning",
            "Constitutional AI",
            "seismo-acoustic plume height"
        )
    )
    private val _readingItemId = MutableStateFlow<Int?>(null)
    private val _metadataSheetItemId = MutableStateFlow<Int?>(null)
    private val _citationSheetItemId = MutableStateFlow<Int?>(null)
    private val _citationStyle = MutableStateFlow(CitationStyle.APA_7)
    private val _isLookingUpIdentifier = MutableStateFlow(false)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
        }
    }

    private val filterParamsFlow = combine(
        _activeCollectionKey,
        _activeTagFilter,
        _activeTypeFilter,
        _searchQuery,
        _sortField
    ) { col, tag, type, q, sort ->
        FilterParams(col, tag, type, q, sort)
    }

    private val modalParamsFlow = combine(
        combine(_readingItemId, _metadataSheetItemId, _citationSheetItemId, _citationStyle) { rId, mId, cId, style ->
            listOf(rId, mId, cId, style)
        },
        combine(_notesSearchQuery, _notesFilter, _isNotesBentoGrid, _isLibraryCompactView) { nQuery, nFilter, bento, compact ->
            listOf(nQuery, nFilter, bento, compact)
        },
        combine(_isSearchScreenOpen, _searchFilters, _recentSearches, _isLookingUpIdentifier, _statusBannerMessage) { sOpen, sFilters, recent, loading, msg ->
            listOf(sOpen, sFilters, recent, loading, msg)
        }
    ) { part1, part2, part3 ->
        @Suppress("UNCHECKED_CAST")
        ModalAndViewParams(
            readingItemId = part1[0] as Int?,
            metadataSheetItemId = part1[1] as Int?,
            citationSheetItemId = part1[2] as Int?,
            citationStyle = part1[3] as CitationStyle,
            notesSearchQuery = part2[0] as String,
            notesFilter = part2[1] as NotesFilter,
            isNotesBentoGrid = part2[2] as Boolean,
            isLibraryCompactView = part2[3] as Boolean,
            isSearchScreenOpen = part3[0] as Boolean,
            searchFilters = part3[1] as SearchFilters,
            recentSearches = part3[2] as List<String>,
            isLookingUpIdentifier = part3[3] as Boolean,
            statusBannerMessage = part3[4] as String?
        )
    }

    val uiState: StateFlow<ZoteroUiState> = combine(
        repository.allItems,
        repository.allCollections,
        repository.standaloneNotes,
        filterParamsFlow,
        modalParamsFlow
    ) { items, collections, standaloneNotes, filter, modal ->
        val nonTrashItems = items.filter { !it.isInTrash }

        // Gather all descendant collection keys recursively
        val descendantKeys = collectDescendantKeys(filter.collectionKey, collections)

        val collectionFiltered = when (filter.collectionKey) {
            LibraryFilter.ALL.key -> nonTrashItems
            LibraryFilter.FAVORITES.key -> nonTrashItems.filter { it.isFavorite }
            LibraryFilter.UNFILED.key -> nonTrashItems.filter { it.collectionKeys.isEmpty() }
            LibraryFilter.DUPLICATES.key -> findDuplicateItems(nonTrashItems)
            LibraryFilter.TRASH.key -> items.filter { it.isInTrash }
            else -> nonTrashItems.filter { item ->
                item.collectionKeys.any { it in descendantKeys }
            }
        }

        val tagFiltered = if (filter.tagFilter != null) {
            collectionFiltered.filter { item ->
                item.tags.any { it.name.equals(filter.tagFilter, ignoreCase = true) }
            }
        } else {
            collectionFiltered
        }

        val typeFiltered = if (filter.typeFilter != null) {
            tagFiltered.filter { it.itemType == filter.typeFilter }
        } else {
            tagFiltered
        }

        val queryFiltered = if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase()
            typeFiltered.filter { item ->
                matchesQuery(item, q)
            }
        } else {
            typeFiltered
        }

        val sorted = when (filter.sort) {
            SortField.DATE_ADDED -> queryFiltered.sortedByDescending { it.dateAdded }
            SortField.TITLE -> queryFiltered.sortedBy { it.title.lowercase() }
            SortField.CREATOR -> queryFiltered.sortedBy { it.creatorSummary.lowercase() }
            SortField.YEAR -> queryFiltered.sortedByDescending { it.year }
        }

        val distinctTags = nonTrashItems
            .flatMap { it.tags }
            .distinctBy { it.name.lowercase() }
            .sortedWith(compareByDescending<ZoteroTag> { it.colorHex != null }.thenBy { it.name.lowercase() })

        val breadcrumbPath = buildBreadcrumbPath(filter.collectionKey, collections)
        val collectionLabel = breadcrumbPath.lastOrNull()?.second
            ?: LibraryFilter.entries.firstOrNull { it.key == filter.collectionKey }?.label
            ?: LibraryFilter.ALL.label

        // Unified notes (paper-linked + standalone)
        val paperNotes = nonTrashItems.flatMap { item ->
            item.notes.map { note ->
                note.copy(
                    parentItemId = item.id,
                    parentItemTitle = item.title
                )
            }
        }
        val allNotesCombined = (standaloneNotes + paperNotes).sortedByDescending { it.updatedAt }

        val filteredNotes = allNotesCombined
            .filter { note ->
                when (modal.notesFilter) {
                    NotesFilter.ALL -> true
                    NotesFilter.PAPER_NOTES -> note.parentItemId != null
                    NotesFilter.STANDALONE -> note.parentItemId == null
                }
            }
            .filter { note ->
                if (modal.notesSearchQuery.isBlank()) true
                else {
                    val nq = modal.notesSearchQuery.trim().lowercase()
                    note.title.lowercase().contains(nq) ||
                        note.content.lowercase().contains(nq) ||
                        (note.parentItemTitle?.lowercase()?.contains(nq) == true)
                }
            }

        // Full-Screen Search Overlay matching
        val sf = modal.searchFilters
        val searchItems = if (!sf.hasActiveFilter) {
            nonTrashItems
        } else {
            nonTrashItems.filter { item ->
                val qOk = sf.query.isBlank() || matchesQuery(item, sf.query.trim().lowercase())
                val colOk = sf.collectionKey == null || sf.collectionKey in item.collectionKeys
                val authOk = sf.authorLastName == null || item.creators.any {
                    it.lastName.equals(sf.authorLastName, ignoreCase = true)
                }
                val yrOk = sf.year == null || item.year == sf.year
                val typeOk = sf.itemType == null || item.itemType == sf.itemType
                val tagOk = sf.tagName == null || item.tags.any {
                    it.name.equals(sf.tagName, ignoreCase = true)
                }
                val pdfOk = !sf.onlyPdf || item.hasPdfAttachment
                qOk && colOk && authOk && yrOk && typeOk && tagOk && pdfOk
            }
        }

        val searchNotes = if (sf.query.isBlank()) {
            emptyList()
        } else {
            val sq = sf.query.trim().lowercase()
            allNotesCombined.filter { n ->
                n.title.lowercase().contains(sq) ||
                    n.content.lowercase().contains(sq) ||
                    (n.parentItemTitle?.lowercase()?.contains(sq) == true)
            }
        }

        ZoteroUiState(
            allItems = items,
            filteredItems = sorted,
            collections = collections,
            allTags = distinctTags,
            unifiedNotes = filteredNotes,
            activeCollectionKey = filter.collectionKey,
            breadcrumbPath = breadcrumbPath,
            activeCollectionLabel = collectionLabel,
            activeTagFilter = filter.tagFilter,
            activeTypeFilter = filter.typeFilter,
            searchQuery = filter.query,
            notesSearchQuery = modal.notesSearchQuery,
            notesFilter = modal.notesFilter,
            isNotesBentoGrid = modal.isNotesBentoGrid,
            isLibraryCompactView = modal.isLibraryCompactView,
            sortField = filter.sort,
            isSearchScreenOpen = modal.isSearchScreenOpen,
            searchFilters = sf,
            searchMatchedItems = searchItems,
            searchMatchedNotes = searchNotes,
            recentSearches = modal.recentSearches,
            readingItemId = modal.readingItemId,
            metadataSheetItemId = modal.metadataSheetItemId,
            citationSheetItemId = modal.citationSheetItemId,
            citationStyle = modal.citationStyle,
            isLookingUpIdentifier = modal.isLookingUpIdentifier,
            statusBannerMessage = modal.statusBannerMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ZoteroUiState()
    )

    private fun collectDescendantKeys(rootKey: String, collections: List<ZoteroCollection>): Set<String> {
        val result = mutableSetOf(rootKey)
        var added = true
        while (added) {
            added = false
            for (c in collections) {
                if (c.parentKey != null && c.parentKey in result && c.key !in result) {
                    result.add(c.key)
                    added = true
                }
            }
        }
        return result
    }

    private fun buildBreadcrumbPath(
        collectionKey: String,
        collections: List<ZoteroCollection>
    ): List<Pair<String, String>> {
        val sys = LibraryFilter.entries.firstOrNull { it.key == collectionKey }
        if (sys != null) return listOf(sys.key to sys.label)

        val chain = mutableListOf<Pair<String, String>>()
        var current = collections.firstOrNull { it.key == collectionKey }
        var guard = 0
        while (current != null && guard < 6) {
            chain.add(0, current.key to current.name)
            val pKey = current.parentKey
            current = if (pKey != null && pKey != "GROUP_LIBRARY") {
                collections.firstOrNull { it.key == pKey }
            } else null
            guard++
        }
        return chain.ifEmpty { listOf(LibraryFilter.ALL.key to LibraryFilter.ALL.label) }
    }

    private fun matchesQuery(item: ZoteroItem, q: String): Boolean {
        return item.title.lowercase().contains(q) ||
            item.creatorSummary.lowercase().contains(q) ||
            item.creators.any { it.fullName.lowercase().contains(q) } ||
            item.year.lowercase().contains(q) ||
            item.publicationTitle.lowercase().contains(q) ||
            item.venueShort.lowercase().contains(q) ||
            item.doi.lowercase().contains(q) ||
            item.isbn.lowercase().contains(q) ||
            item.abstractNote.lowercase().contains(q) ||
            item.tags.any { it.name.lowercase().contains(q) } ||
            item.notes.any { it.title.lowercase().contains(q) || it.content.lowercase().contains(q) }
    }

    private fun findDuplicateItems(items: List<ZoteroItem>): List<ZoteroItem> {
        val byTitle = items.groupBy { it.title.trim().lowercase() }.filter { it.value.size > 1 }
        val byDoi = items.filter { it.doi.isNotBlank() }
            .groupBy { it.doi.trim().lowercase() }
            .filter { it.value.size > 1 }
        return (byTitle.values.flatten() + byDoi.values.flatten()).distinctBy { it.id }
    }

    fun openSearchScreen(initialQuery: String = "") {
        _searchFilters.value = _searchFilters.value.copy(query = initialQuery)
        _isSearchScreenOpen.value = true
    }

    fun closeSearchScreen() {
        _isSearchScreenOpen.value = false
    }

    fun updateSearchFilters(transform: (SearchFilters) -> SearchFilters) {
        _searchFilters.value = transform(_searchFilters.value)
    }

    fun clearSearchFilters() {
        _searchFilters.value = SearchFilters()
    }

    fun recordRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _recentSearches.value = (listOf(trimmed) + _recentSearches.value.filterNot {
            it.equals(trimmed, ignoreCase = true)
        }).take(8)
    }

    fun removeRecentSearch(query: String) {
        _recentSearches.value = _recentSearches.value.filterNot { it == query }
    }

    fun toggleNotesViewMode() {
        _isNotesBentoGrid.value = !_isNotesBentoGrid.value
    }

    fun toggleLibraryViewMode() {
        _isLibraryCompactView.value = !_isLibraryCompactView.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setNotesSearchQuery(query: String) {
        _notesSearchQuery.value = query
    }

    fun setNotesFilter(filter: NotesFilter) {
        _notesFilter.value = filter
    }

    fun selectCollection(collectionKey: String) {
        _activeCollectionKey.value = collectionKey
    }

    fun toggleTagFilter(tagName: String?) {
        _activeTagFilter.value = if (_activeTagFilter.value == tagName) null else tagName
    }

    fun setItemTypeFilter(itemType: ItemType?) {
        _activeTypeFilter.value = if (_activeTypeFilter.value == itemType) null else itemType
    }

    fun setSortField(sortField: SortField) {
        _sortField.value = sortField
    }

    fun openPaperReader(itemId: Int) {
        _readingItemId.value = itemId
    }

    fun closePaperReader() {
        _readingItemId.value = null
    }

    fun openMetadataBottomSheet(itemId: Int) {
        _metadataSheetItemId.value = itemId
    }

    fun closeMetadataBottomSheet() {
        _metadataSheetItemId.value = null
    }

    fun openCitationBottomSheet(itemId: Int) {
        _citationSheetItemId.value = itemId
    }

    fun closeCitationBottomSheet() {
        _citationSheetItemId.value = null
    }

    fun setCitationStyle(style: CitationStyle) {
        _citationStyle.value = style
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun lookupByIdentifier(identifier: String) {
        viewModelScope.launch {
            _isLookingUpIdentifier.value = true
            val targetCollection = _activeCollectionKey.value.takeIf { !it.startsWith("SYSTEM_") }
            val result = repository.lookupByIdentifier(identifier, targetCollection)
            _isLookingUpIdentifier.value = false
            result.onSuccess { item ->
                _statusBannerMessage.value = "Imported \"${item.title}\" (${item.year})"
            }.onFailure { err ->
                _statusBannerMessage.value = err.message ?: "Could not resolve identifier"
            }
        }
    }

    fun saveManualItem(
        existingItem: ZoteroItem?,
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
    ) {
        if (title.isBlank()) return
        val parsedCreators = creatorsText.split(";", "\n")
            .mapNotNull { raw ->
                val trimmed = raw.trim()
                if (trimmed.isEmpty()) null
                else if (trimmed.contains(",")) {
                    val parts = trimmed.split(",", limit = 2)
                    Creator(firstName = parts[1].trim(), lastName = parts[0].trim())
                } else {
                    val words = trimmed.split(" ")
                    if (words.size == 1) Creator("", words[0])
                    else Creator(words.dropLast(1).joinToString(" "), words.last())
                }
            }
            .ifEmpty { listOf(Creator("Research", "Author")) }

        val existingTagColors = existingItem?.tags?.associate { it.name.lowercase() to it.colorHex }.orEmpty()
        val parsedTags = tagsText.split(",")
            .mapNotNull { raw ->
                val name = raw.trim()
                if (name.isEmpty()) null
                else ZoteroTag(name = name, colorHex = existingTagColors[name.lowercase()] ?: "#059669")
            }

        val defaultSections = existingItem?.sections?.takeIf { it.isNotEmpty() } ?: listOf(
            PaperSection(
                heading = "1. Overview & Motivation",
                pageRange = "pp. 1–3",
                paragraphs = listOf(
                    abstractNote.trim().ifBlank {
                        "Full-text reading view for ${title.trim()}. Use the highlight button below to record passages and page annotations."
                    }
                )
            )
        )

        val itemToSave = (existingItem ?: ZoteroItem(
            itemKey = "ZOT" + UUID.randomUUID().toString().take(5).uppercase(),
            itemType = itemType,
            title = title.trim(),
            creators = parsedCreators,
            year = year.trim(),
            publicationTitle = publicationTitle.trim()
        )).copy(
            itemType = itemType,
            title = title.trim(),
            creators = parsedCreators,
            year = year.trim(),
            publicationTitle = publicationTitle.trim(),
            venueShort = venueShort.trim().ifBlank { publicationTitle.trim().take(14) },
            citationCount = existingItem?.citationCount?.ifBlank { "14" } ?: "14",
            volume = volume.trim(),
            issue = issue.trim(),
            pages = pages.trim(),
            doi = doi.trim(),
            isbn = isbn.trim(),
            url = url.trim(),
            abstractNote = abstractNote.trim(),
            sections = defaultSections,
            tags = parsedTags,
            collectionKeys = selectedCollectionKeys
        )

        viewModelScope.launch {
            repository.saveItem(itemToSave)
            _statusBannerMessage.value = if (existingItem == null) {
                "Added \"${itemToSave.title}\""
            } else {
                "Updated \"${itemToSave.title}\""
            }
        }
    }

    fun toggleFavorite(item: ZoteroItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun moveToTrash(item: ZoteroItem) {
        viewModelScope.launch {
            repository.moveToTrash(item)
            if (_readingItemId.value == item.id) {
                _readingItemId.value = null
            }
            _statusBannerMessage.value = "Moved \"${item.title}\" to Trash"
        }
    }

    fun restoreFromTrash(item: ZoteroItem) {
        viewModelScope.launch {
            repository.restoreFromTrash(item)
            _statusBannerMessage.value = "Restored \"${item.title}\" to Library"
        }
    }

    fun deletePermanently(item: ZoteroItem) {
        viewModelScope.launch {
            repository.deletePermanently(item.id)
            if (_readingItemId.value == item.id) {
                _readingItemId.value = null
            }
            _statusBannerMessage.value = "Permanently deleted \"${item.title}\""
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            _statusBannerMessage.value = "Emptied Trash"
        }
    }

    fun addNoteToItem(item: ZoteroItem, title: String, content: String) {
        if (title.isBlank() && content.isBlank()) return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val newNote = ZoteroNote(
            id = "N_" + UUID.randomUUID().toString().take(6),
            title = title.trim().ifBlank { "Research Note (${item.creatorSummary})" },
            content = content.trim(),
            updatedAt = today,
            parentItemId = item.id,
            parentItemTitle = item.title
        )
        viewModelScope.launch {
            repository.saveItem(item.copy(notes = item.notes + newNote))
            _statusBannerMessage.value = "Saved note to \"${item.title}\""
        }
    }

    fun saveStandaloneNote(id: String?, title: String, content: String) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            repository.saveStandaloneNote(id, title, content)
            _statusBannerMessage.value = "Saved research note"
        }
    }

    fun deleteUnifiedNote(note: ZoteroNote) {
        viewModelScope.launch {
            if (note.parentItemId != null) {
                val parent = uiState.value.allItems.firstOrNull { it.id == note.parentItemId }
                if (parent != null) {
                    repository.saveItem(parent.copy(notes = parent.notes.filterNot { it.id == note.id }))
                }
            } else {
                repository.deleteStandaloneNote(note.id)
            }
            _statusBannerMessage.value = "Deleted note \"${note.title}\""
        }
    }

    fun addAnnotationToItem(
        item: ZoteroItem,
        pageLabel: String,
        highlightedText: String,
        comment: String,
        colorHex: String
    ) {
        if (highlightedText.isBlank()) return
        val newAnnotation = PdfAnnotation(
            id = "A_" + UUID.randomUUID().toString().take(6),
            pageLabel = pageLabel.trim().ifBlank { "p. 1" },
            highlightedText = highlightedText.trim(),
            comment = comment.trim(),
            colorHex = colorHex
        )
        viewModelScope.launch {
            repository.saveItem(item.copy(annotations = item.annotations + newAnnotation))
            _statusBannerMessage.value = "Added highlight on ${newAnnotation.pageLabel}"
        }
    }

    fun deleteAnnotationFromItem(item: ZoteroItem, annotationId: String) {
        viewModelScope.launch {
            repository.saveItem(item.copy(annotations = item.annotations.filterNot { it.id == annotationId }))
        }
    }

    fun addTagToItem(item: ZoteroItem, tagName: String, colorHex: String?) {
        val cleaned = tagName.trim()
        if (cleaned.isBlank()) return
        val updatedTags = item.tags.filterNot { it.name.equals(cleaned, ignoreCase = true) } +
            ZoteroTag(name = cleaned, colorHex = colorHex)
        viewModelScope.launch {
            repository.saveItem(item.copy(tags = updatedTags))
        }
    }

    fun removeTagFromItem(item: ZoteroItem, tagName: String) {
        viewModelScope.launch {
            repository.saveItem(
                item.copy(tags = item.tags.filterNot { it.name.equals(tagName, ignoreCase = true) })
            )
        }
    }

    fun toggleItemInCollection(item: ZoteroItem, collectionKey: String) {
        val updatedKeys = if (collectionKey in item.collectionKeys) {
            item.collectionKeys - collectionKey
        } else {
            item.collectionKeys + collectionKey
        }
        viewModelScope.launch {
            repository.saveItem(item.copy(collectionKeys = updatedKeys))
        }
    }

    fun createCollection(name: String, description: String, parentKey: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createCollection(name, description, parentKey)
            _statusBannerMessage.value = "Created collection \"${name.trim()}\""
        }
    }

    fun deleteCollection(collection: ZoteroCollection) {
        viewModelScope.launch {
            repository.deleteCollection(collection.key)
            if (_activeCollectionKey.value == collection.key) {
                _activeCollectionKey.value = LibraryFilter.ALL.key
            }
            _statusBannerMessage.value = "Deleted collection \"${collection.name}\""
        }
    }
}
