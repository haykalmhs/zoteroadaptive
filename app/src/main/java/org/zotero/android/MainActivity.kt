package org.zotero.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.zotero.android.data.model.CitationStyle
import org.zotero.android.data.model.ZoteroCollection
import org.zotero.android.data.model.ZoteroItem
import org.zotero.android.ui.LibraryFilter
import org.zotero.android.ui.ZoteroUiState
import org.zotero.android.ui.ZoteroViewModel
import org.zotero.android.ui.components.CitationBottomSheet
import org.zotero.android.ui.components.CreateOrEditItemDialog
import org.zotero.android.ui.components.IdentifierLookupDialog
import org.zotero.android.ui.components.ItemMetadataBottomSheet
import org.zotero.android.ui.screens.LibraryScreen
import org.zotero.android.ui.screens.NotesScreen
import org.zotero.android.ui.screens.PaperReaderScreen
import org.zotero.android.ui.screens.SearchOverlayScreen
import org.zotero.android.ui.theme.AvatarNavy
import org.zotero.android.ui.theme.OnTealAccentPill
import org.zotero.android.ui.theme.TealAccentPill
import org.zotero.android.ui.theme.ZoteroBrandRed
import org.zotero.android.ui.theme.ZoteroTheme

enum class ZoteroBottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    LIBRARY("library", "Library", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    NOTES("notes", "Notes", Icons.AutoMirrored.Filled.StickyNote2, Icons.AutoMirrored.Outlined.StickyNote2)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZoteroTheme {
                ZoteroAppScreen()
            }
        }
    }
}

@Composable
fun ZoteroAppScreen(
    viewModel: ZoteroViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(ZoteroBottomTab.LIBRARY) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showIdentifierDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateOrEditItemDialog by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ZoteroItem?>(null) }
    var showCreateCollectionDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickNoteDialog by rememberSaveable { mutableStateOf(false) }
    var showAccountSettingsDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    if (drawerState.isOpen) {
        BackHandler {
            scope.launch { drawerState.close() }
        }
    } else if (uiState.readingItem != null) {
        BackHandler {
            viewModel.closePaperReader()
        }
    } else if (uiState.isSearchScreenOpen) {
        BackHandler {
            viewModel.closeSearchScreen()
        }
    } else if (currentTab != ZoteroBottomTab.LIBRARY) {
        BackHandler {
            currentTab = ZoteroBottomTab.LIBRARY
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = uiState.readingItem == null && !uiState.isSearchScreenOpen,
        drawerContent = {
            ZoteroCollectionsDrawerSheet(
                uiState = uiState,
                onSelectCollection = { colKey ->
                    viewModel.selectCollection(colKey)
                    currentTab = ZoteroBottomTab.LIBRARY
                    scope.launch { drawerState.close() }
                },
                onToggleTagFilter = { tagName ->
                    viewModel.toggleTagFilter(tagName)
                    currentTab = ZoteroBottomTab.LIBRARY
                    scope.launch { drawerState.close() }
                },
                onOpenNewCollectionDialog = {
                    scope.launch { drawerState.close() }
                    showCreateCollectionDialog = true
                },
                onOpenAccountSettings = {
                    scope.launch { drawerState.close() }
                    showAccountSettingsDialog = true
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                bottomBar = {
                    // 2-Tab Bottom Navigation Bar (Library | Notes)
                    Column {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            thickness = 0.8.dp
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bottom_navigation_bar")
                        ) {
                            ZoteroBottomTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = OnTealAccentPill,
                                        selectedTextColor = OnTealAccentPill,
                                        indicatorColor = TealAccentPill,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        ZoteroBottomTab.LIBRARY -> LibraryScreen(
                            uiState = uiState,
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            },
                            onOpenFullScreenSearch = {
                                viewModel.openSearchScreen()
                            },
                            onToggleCompactView = viewModel::toggleLibraryViewMode,
                            onSelectCollection = viewModel::selectCollection,
                            onClearTagFilter = { viewModel.toggleTagFilter(null) },
                            onSetSortField = viewModel::setSortField,
                            onOpenPaperReader = { itemId ->
                                viewModel.openPaperReader(itemId)
                            },
                            onOpenMetadataSheet = { itemId ->
                                viewModel.openMetadataBottomSheet(itemId)
                            },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onEmptyTrash = viewModel::emptyTrash,
                            onShowIdentifierDialog = { showIdentifierDialog = true },
                            onShowCreateItemDialog = {
                                editingItem = null
                                showCreateOrEditItemDialog = true
                            },
                            onShowQuickNoteDialog = {
                                showQuickNoteDialog = true
                            }
                        )

                        ZoteroBottomTab.NOTES -> NotesScreen(
                            uiState = uiState,
                            onOpenDrawer = {
                                scope.launch { drawerState.open() }
                            },
                            onSearchQueryChange = viewModel::setNotesSearchQuery,
                            onToggleViewMode = viewModel::toggleNotesViewMode,
                            onSetNotesFilter = viewModel::setNotesFilter,
                            onOpenPaperReader = { itemId ->
                                viewModel.openPaperReader(itemId)
                            },
                            onAddPaperNote = viewModel::addNoteToItem,
                            onSaveStandaloneNote = viewModel::saveStandaloneNote,
                            onDeleteNote = viewModel::deleteUnifiedNote,
                            onShowIdentifierDialog = { showIdentifierDialog = true },
                            onShowCreateItemDialog = {
                                editingItem = null
                                showCreateOrEditItemDialog = true
                            }
                        )
                    }
                }
            }

            // Full-Screen Gmail-Inspired Search Overlay with Filter Chips & Recent Searches
            if (uiState.isSearchScreenOpen) {
                SearchOverlayScreen(
                    uiState = uiState,
                    onClose = { viewModel.closeSearchScreen() },
                    onUpdateFilters = viewModel::updateSearchFilters,
                    onClearFilters = viewModel::clearSearchFilters,
                    onRecordRecentSearch = viewModel::recordRecentSearch,
                    onRemoveRecentSearch = viewModel::removeRecentSearch,
                    onToggleCompactView = viewModel::toggleLibraryViewMode,
                    onOpenPaperReader = { itemId ->
                        viewModel.openPaperReader(itemId)
                    },
                    onOpenMetadataSheet = { itemId ->
                        viewModel.openMetadataBottomSheet(itemId)
                    },
                    onToggleFavorite = viewModel::toggleFavorite
                )
            }

            // Full-Screen Direct Paper Reading Mode Overlay when an item is opened
            val activeReadingItem = uiState.readingItem
            if (activeReadingItem != null) {
                PaperReaderScreen(
                    item = activeReadingItem,
                    onBack = { viewModel.closePaperReader() },
                    onOpenMetadataSheet = {
                        viewModel.openMetadataBottomSheet(activeReadingItem.id)
                    },
                    onOpenCiteSheet = {
                        viewModel.openCitationBottomSheet(activeReadingItem.id)
                    },
                    onToggleFavorite = {
                        viewModel.toggleFavorite(activeReadingItem)
                    },
                    onAddAnnotation = { page, text, comment, colorHex ->
                        viewModel.addAnnotationToItem(activeReadingItem, page, text, comment, colorHex)
                    },
                    onDeleteAnnotation = { annotationId ->
                        viewModel.deleteAnnotationFromItem(activeReadingItem, annotationId)
                    },
                    onAddNote = { title, content ->
                        viewModel.addNoteToItem(activeReadingItem, title, content)
                    }
                )
            }
        }
    }

    // Paper Information & Metadata Bottom Sheet (accessible from Reader & Library rows)
    val activeMetadataItem = uiState.metadataSheetItem
    if (activeMetadataItem != null) {
        ItemMetadataBottomSheet(
            item = activeMetadataItem,
            allCollections = uiState.collections,
            onDismiss = { viewModel.closeMetadataBottomSheet() },
            onEditMetadata = {
                editingItem = activeMetadataItem
                viewModel.closeMetadataBottomSheet()
                showCreateOrEditItemDialog = true
            },
            onOpenCiteSheet = {
                val id = activeMetadataItem.id
                viewModel.closeMetadataBottomSheet()
                viewModel.openCitationBottomSheet(id)
            },
            onToggleFavorite = { viewModel.toggleFavorite(activeMetadataItem) },
            onToggleCollection = { colKey ->
                viewModel.toggleItemInCollection(activeMetadataItem, colKey)
            },
            onAddTag = { name, hex ->
                viewModel.addTagToItem(activeMetadataItem, name, hex)
            },
            onRemoveTag = { name ->
                viewModel.removeTagFromItem(activeMetadataItem, name)
            },
            onMoveToTrash = {
                viewModel.moveToTrash(activeMetadataItem)
                viewModel.closeMetadataBottomSheet()
            },
            onRestoreFromTrash = {
                viewModel.restoreFromTrash(activeMetadataItem)
                viewModel.closeMetadataBottomSheet()
            },
            onDeletePermanently = {
                viewModel.deletePermanently(activeMetadataItem)
                viewModel.closeMetadataBottomSheet()
            }
        )
    }

    // Cite & Export Bottom Sheet (accessible from Metadata Sheet & Reader Top Bar)
    val activeCitationItem = uiState.citationSheetItem
    if (activeCitationItem != null) {
        CitationBottomSheet(
            item = activeCitationItem,
            selectedStyle = uiState.citationStyle,
            onSelectStyle = viewModel::setCitationStyle,
            onDismiss = { viewModel.closeCitationBottomSheet() }
        )
    }

    // Add by Identifier Dialog
    if (showIdentifierDialog) {
        IdentifierLookupDialog(
            isLoading = uiState.isLookingUpIdentifier,
            onDismiss = { showIdentifierDialog = false },
            onLookup = { idStr ->
                viewModel.lookupByIdentifier(idStr)
                showIdentifierDialog = false
            }
        )
    }

    // Create / Edit Paper Dialog
    if (showCreateOrEditItemDialog) {
        CreateOrEditItemDialog(
            existingItem = editingItem,
            collections = uiState.collections,
            defaultCollectionKey = uiState.activeCollectionKey.takeIf { !it.startsWith("SYSTEM_") },
            onDismiss = { showCreateOrEditItemDialog = false },
            onSave = { itemType, title, creators, year, pub, venueShort, vol, iss, pages, doi, isbn, url, abs, tags, cols ->
                viewModel.saveManualItem(
                    editingItem,
                    itemType,
                    title,
                    creators,
                    year,
                    pub,
                    venueShort,
                    vol,
                    iss,
                    pages,
                    doi,
                    isbn,
                    url,
                    abs,
                    tags,
                    cols
                )
                showCreateOrEditItemDialog = false
            }
        )
    }

    // Quick Research Note Dialog (accessible from Speed-Dial FAB in Library)
    if (showQuickNoteDialog) {
        QuickStandaloneNoteDialog(
            onDismiss = { showQuickNoteDialog = false },
            onSave = { title, content ->
                viewModel.saveStandaloneNote(
                    id = null,
                    title = title,
                    content = content
                )
                showQuickNoteDialog = false
            }
        )
    }

    // Create Collection Dialog (from Navigation Drawer)
    if (showCreateCollectionDialog) {
        CreateCollectionDialog(
            existingCollections = uiState.collections.filter { it.parentKey != "GROUP_LIBRARY" },
            onDismiss = { showCreateCollectionDialog = false },
            onCreate = { name, desc, parentKey ->
                viewModel.createCollection(name, desc, parentKey)
                showCreateCollectionDialog = false
            }
        )
    }

    // Account & Settings Dialog (from Gemini-style Navigation Drawer bottom footer)
    if (showAccountSettingsDialog) {
        AccountAndSettingsDialog(
            uiState = uiState,
            onSelectCitationStyle = viewModel::setCitationStyle,
            onToggleLibraryDensity = viewModel::toggleLibraryViewMode,
            onToggleNotesBento = viewModel::toggleNotesViewMode,
            onOpenTrash = {
                showAccountSettingsDialog = false
                viewModel.selectCollection(LibraryFilter.TRASH.key)
                currentTab = ZoteroBottomTab.LIBRARY
            },
            onDismiss = { showAccountSettingsDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoteroCollectionsDrawerSheet(
    uiState: ZoteroUiState,
    onSelectCollection: (String) -> Unit,
    onToggleTagFilter: (String?) -> Unit,
    onOpenNewCollectionDialog: () -> Unit,
    onOpenAccountSettings: () -> Unit
) {
    val rootCollections = uiState.collections.filter { it.parentKey == null }
    val groupLibraries = uiState.collections.filter { it.parentKey == "GROUP_LIBRARY" }

    // Track expanded state for parent collections; default expanded for Final Year Project & Signal Processing
    val expandedMap = remember {
        mutableStateMapOf(
            "COL_FYP" to true,
            "COL_SIGPROC" to true
        )
    }
    var showAllTags by rememberSaveable { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .width(304.dp)
            .testTag("collections_navigation_drawer")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Header: Red "Z" emblem + "Zotero" title + Trash icon button on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Z",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = ZoteroBrandRed
                    )
                    Text(
                        text = "Zotero",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF172B4D)
                    )
                }

                IconButton(
                    onClick = { onSelectCollection(LibraryFilter.TRASH.key) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("drawer_header_trash_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Trash",
                        tint = if (uiState.activeCollectionKey == LibraryFilter.TRASH.key) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Scrollable Middle Section (All Items, Collapsible Collections Tree, Group Libraries, Labels & Tags)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // Compact "All Items" row
                val isAllSelected = uiState.activeCollectionKey == LibraryFilter.ALL.key && uiState.activeTagFilter == null
                CompactDrawerRow(
                    label = LibraryFilter.ALL.label,
                    icon = Icons.Default.CollectionsBookmark,
                    selected = isAllSelected,
                    fontWeight = FontWeight.Medium,
                    onClick = { onSelectCollection(LibraryFilter.ALL.key) },
                    modifier = Modifier.testTag("drawer_system_all")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // COLLECTIONS Header + compact "+" icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COLLECTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onOpenNewCollectionDialog,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("drawer_new_collection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create collection",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Multi-level Collapsible Collections Tree
                rootCollections.forEach { rootCol ->
                    val level1Children = uiState.collections.filter { it.parentKey == rootCol.key }
                    val hasChildren = level1Children.isNotEmpty()
                    val isRootExpanded = expandedMap[rootCol.key] ?: true
                    val isRootSelected = uiState.activeCollectionKey == rootCol.key

                    CompactTreeCollectionRow(
                        collection = rootCol,
                        level = 0,
                        hasChildren = hasChildren,
                        isExpanded = isRootExpanded,
                        isSelected = isRootSelected,
                        onToggleExpand = {
                            expandedMap[rootCol.key] = !isRootExpanded
                        },
                        onClick = { onSelectCollection(rootCol.key) }
                    )

                    if (hasChildren && isRootExpanded) {
                        level1Children.forEach { level1Col ->
                            val level2Children = uiState.collections.filter { it.parentKey == level1Col.key }
                            val hasLevel2 = level2Children.isNotEmpty()
                            val isLevel1Expanded = expandedMap[level1Col.key] ?: true
                            val isLevel1Selected = uiState.activeCollectionKey == level1Col.key

                            CompactTreeCollectionRow(
                                collection = level1Col,
                                level = 1,
                                hasChildren = hasLevel2,
                                isExpanded = isLevel1Expanded,
                                isSelected = isLevel1Selected,
                                onToggleExpand = {
                                    expandedMap[level1Col.key] = !isLevel1Expanded
                                },
                                onClick = { onSelectCollection(level1Col.key) }
                            )

                            // Nested leaf items with vertical hierarchy line (matching image_32c74c63.png)
                            if (hasLevel2 && isLevel1Expanded) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .padding(start = 24.dp)
                                ) {
                                    // Vertical guide line
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                            .padding(vertical = 4.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        level2Children.forEach { leafCol ->
                                            val isLeafSelected = uiState.activeCollectionKey == leafCol.key
                                            CompactLeafCollectionRow(
                                                collection = leafCol,
                                                isSelected = isLeafSelected,
                                                onClick = { onSelectCollection(leafCol.key) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // GROUP LIBRARIES Section
                if (groupLibraries.isNotEmpty()) {
                    Text(
                        text = "GROUP LIBRARIES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    groupLibraries.forEach { groupCol ->
                        val isGroupSelected = uiState.activeCollectionKey == groupCol.key
                        CompactDrawerRow(
                            label = groupCol.name,
                            icon = Icons.Default.Groups,
                            selected = isGroupSelected,
                            fontWeight = FontWeight.Medium,
                            onClick = { onSelectCollection(groupCol.key) },
                            modifier = Modifier.testTag("drawer_col_${groupCol.key}")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // LABELS & TAGS Compact Chip Cloud
                if (uiState.allTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LABELS & TAGS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val visibleTags = if (showAllTags) uiState.allTags else uiState.allTags.take(3)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleTags.forEach { tag ->
                            val isSelected = uiState.activeTagFilter.equals(tag.name, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) TealAccentPill else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) OnTealAccentPill.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleTagFilter(tag.name) }
                                    .testTag("drawer_tag_${tag.name}")
                            ) {
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) OnTealAccentPill else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        if (uiState.allTags.size > 3) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TealAccentPill,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAllTags = !showAllTags }
                                    .testTag("drawer_more_tags_chip")
                            ) {
                                Text(
                                    text = if (showAllTags) "fewer tags" else "more tags",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnTealAccentPill,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 3. Pinned Gemini-Style Bottom Footer: Account ("AL") & Settings
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                thickness = 0.8.dp
            )
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenAccountSettings() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("drawer_footer_account_settings"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AvatarNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AL",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = "Alex Rivera",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Settings & Sync",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenAccountSettings,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("drawer_footer_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDrawerRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    fontWeight: FontWeight = FontWeight.Normal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) TealAccentPill else Color.Transparent
    val contentColor = if (selected) OnTealAccentPill else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) OnTealAccentPill else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else fontWeight,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactTreeCollectionRow(
    collection: ZoteroCollection,
    level: Int,
    hasChildren: Boolean,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) TealAccentPill else Color.Transparent
    val startPadding = (level * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 10.dp)
            .testTag("drawer_col_${collection.key}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasChildren) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleExpand),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(22.dp))
        }

        Icon(
            imageVector = if (isSelected || (hasChildren && isExpanded)) Icons.Default.FolderOpen else Icons.Default.Folder,
            contentDescription = null,
            tint = if (isSelected) {
                OnTealAccentPill
            } else if (hasChildren) {
                Color(0xFF3B6A9A)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = collection.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = when {
                isSelected -> FontWeight.Bold
                level == 0 && hasChildren -> FontWeight.SemiBold
                else -> FontWeight.Medium
            },
            color = if (isSelected) OnTealAccentPill else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactLeafCollectionRow(
    collection: ZoteroCollection,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) TealAccentPill else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(35.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag("drawer_col_${collection.key}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.FolderOpen else Icons.Default.Folder,
            contentDescription = null,
            tint = if (isSelected) OnTealAccentPill else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = collection.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) OnTealAccentPill else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickStandaloneNoteDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Research Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Heading") },
                    placeholder = { Text("e.g. Key Takeaways on Transfer Learning") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_note_title_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Research Note Content *") },
                    placeholder = { Text("Write observations, equations, or follow-up citations...") },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_note_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, content) },
                enabled = content.isNotBlank() || title.isNotBlank(),
                modifier = Modifier.testTag("confirm_quick_note_button")
            ) {
                Text("Save Note")
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
private fun AccountAndSettingsDialog(
    uiState: ZoteroUiState,
    onSelectCitationStyle: (CitationStyle) -> Unit,
    onToggleLibraryDensity: () -> Unit,
    onToggleNotesBento: () -> Unit,
    onOpenTrash: () -> Unit,
    onDismiss: () -> Unit
) {
    val activePaperCount = uiState.allItems.count { !it.isInTrash }
    val trashedCount = uiState.allItems.count { it.isInTrash }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AvatarNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column {
                    Text(
                        text = "Alex Rivera",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "alex.rivera@university.edu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = activePaperCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Papers", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.collections.size.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Collections", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.unifiedNotes.size.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Notes", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Text(
                    text = "Default Citation Format",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CitationStyle.entries.forEach { style ->
                        FilterChip(
                            selected = uiState.citationStyle == style,
                            onClick = { onSelectCitationStyle(style) },
                            label = { Text(style.shortLabel) }
                        )
                    }
                }

                Text(
                    text = "Display Preferences",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.isLibraryCompactView,
                        onClick = onToggleLibraryDensity,
                        label = { Text("Compact Library Rows") }
                    )
                    FilterChip(
                        selected = uiState.isNotesBentoGrid,
                        onClick = onToggleNotesBento,
                        label = { Text("Keep Bento Notes") }
                    )
                }

                if (trashedCount > 0) {
                    TextButton(
                        onClick = onOpenTrash,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Trash ($trashedCount items)")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateCollectionDialog(
    existingCollections: List<ZoteroCollection>,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, parentKey: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedParentKey by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Collection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Collection Name *") },
                    placeholder = { Text("e.g. Baselines & Benchmarks") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collection_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collection_desc_input")
                )

                if (existingCollections.isNotEmpty()) {
                    Text(
                        text = "Parent Collection (Optional):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedParentKey == null,
                            onClick = { selectedParentKey = null },
                            label = { Text("Top-Level") }
                        )
                        existingCollections.take(6).forEach { parent ->
                            FilterChip(
                                selected = selectedParentKey == parent.key,
                                onClick = { selectedParentKey = parent.key },
                                label = { Text(parent.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description, selectedParentKey) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_collection_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
