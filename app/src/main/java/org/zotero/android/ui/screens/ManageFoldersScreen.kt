package org.zotero.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.zotero.android.data.model.ZoteroCollection
import org.zotero.android.ui.FolderDropPlacement
import org.zotero.android.ui.components.parseHexColor
import org.zotero.android.ui.theme.ZoteroThemeTokens

val FolderColorPalette = listOf(
    null to "Default",
    "#2563EB" to "Royal Blue",
    "#059669" to "Emerald",
    "#D97706" to "Warm Amber",
    "#E11D48" to "Crimson Rose",
    "#7C3AED" to "Violet",
    "#0D9488" to "Teal",
    "#4F46E5" to "Indigo",
    "#475569" to "Slate"
)

private data class FlatFolderNode(
    val collection: ZoteroCollection,
    val level: Int,
    val hasChildren: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean
)

private data class ActiveFolderDropTarget(
    val targetKey: String?,
    val targetName: String,
    val placement: FolderDropPlacement
)

private const val ROOT_HEADER_BOUNDS_KEY = "__ROOT_FOLDERS_HEADER__"

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ManageFoldersScreen(
    collections: List<ZoteroCollection>,
    onBack: () -> Unit,
    onCreateFolder: (name: String, parentKey: String?, colorHex: String?) -> Unit,
    onRenameFolder: (key: String, newName: String) -> Unit,
    onUpdateFolderColor: (keys: Set<String>, colorHex: String?) -> Unit,
    onMoveFolders: (keys: Set<String>, targetParentKey: String?) -> Unit,
    onReorderFolder: (key: String, moveUp: Boolean) -> Unit,
    onDropReorderFolder: (draggedKey: String, targetKey: String?, placement: FolderDropPlacement) -> Unit = { _, _, _ -> },
    onDeleteFolders: (keys: Set<String>) -> Unit
) {
    val extColors = ZoteroThemeTokens.colors
    val userCollections = remember(collections) {
        collections.filter { !it.isSystem && it.parentKey != "GROUP_LIBRARY" }
    }

    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }

    // Keep track of expanded parent folders; default all parents expanded for clear management
    val expandedMap = remember {
        mutableStateMapOf(
            "COL_FYP" to true,
            "COL_SIGPROC" to true
        )
    }

    // Drag-and-drop state
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val rowBoundsMap = remember { mutableStateMapOf<String, Pair<Float, Float>>() }

    // Dialog states
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createParentKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showColorDialog by rememberSaveable { mutableStateOf(false) }
    var showMoveDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        when {
            draggedKey != null -> {
                draggedKey = null
                dragOffsetY = 0f
                dragOffsetX = 0f
            }
            isSelectionMode -> {
                isSelectionMode = false
                selectedKeys = emptySet()
            }
            else -> onBack()
        }
    }

    // Build flattened visible tree nodes respecting expandedMap and sortOrder
    val visibleNodes = remember(userCollections, expandedMap.toMap()) {
        val result = mutableListOf<FlatFolderNode>()
        fun traverse(parentKey: String?, level: Int) {
            val children = userCollections
                .filter { it.parentKey == parentKey }
                .sortedWith(compareBy<ZoteroCollection> { it.sortOrder }.thenBy { it.name.lowercase() })
            children.forEachIndexed { idx, col ->
                val subChildren = userCollections.filter { it.parentKey == col.key }
                val hasChildren = subChildren.isNotEmpty()
                result.add(
                    FlatFolderNode(
                        collection = col,
                        level = level,
                        hasChildren = subChildren.isNotEmpty(),
                        canMoveUp = idx > 0,
                        canMoveDown = idx < children.lastIndex
                    )
                )
                val isExpanded = expandedMap[col.key] ?: true
                if (hasChildren && isExpanded) {
                    traverse(col.key, level + 1)
                }
            }
        }
        traverse(null, 0)
        result
    }

    // Descendants of the currently dragged folder (forbidden drop targets to prevent circular nesting)
    val draggedForbiddenKeys = remember(draggedKey, userCollections) {
        val root = draggedKey ?: return@remember emptySet<String>()
        val result = mutableSetOf(root)
        var added = true
        while (added) {
            added = false
            for (c in userCollections) {
                if (c.parentKey != null && c.parentKey in result && c.key !in result) {
                    result.add(c.key)
                    added = true
                }
            }
        }
        result
    }

    // Real-time drop target calculation based on dragged row's center Y and horizontal offset X
    val activeDropTarget by remember(
        draggedKey,
        dragOffsetY,
        dragOffsetX,
        visibleNodes,
        draggedForbiddenKeys
    ) {
        derivedStateOf {
            val currentDragKey = draggedKey ?: return@derivedStateOf null
            val startBounds = rowBoundsMap[currentDragKey] ?: return@derivedStateOf null
            val currentCenterY = (startBounds.first + startBounds.second) / 2f + dragOffsetY

            val rootBounds = rowBoundsMap[ROOT_HEADER_BOUNDS_KEY]
            if (rootBounds != null && currentCenterY <= rootBounds.second) {
                return@derivedStateOf ActiveFolderDropTarget(
                    targetKey = null,
                    targetName = "Top-Level Folders",
                    placement = FolderDropPlacement.ROOT_TOP
                )
            }

            for (node in visibleNodes) {
                val bounds = rowBoundsMap[node.collection.key] ?: continue
                if (currentCenterY in bounds.first..bounds.second) {
                    if (node.collection.key == currentDragKey) {
                        // Allow dragging a nested folder left in-place to promote it to Top-Level
                        if (node.level > 0 && dragOffsetX < -42f) {
                            return@derivedStateOf ActiveFolderDropTarget(
                                targetKey = null,
                                targetName = "Top-Level Folders",
                                placement = FolderDropPlacement.ROOT_TOP
                            )
                        }
                        return@derivedStateOf null
                    }
                    if (node.collection.key in draggedForbiddenKeys) {
                        return@derivedStateOf null
                    }

                    val height = (bounds.second - bounds.first).coerceAtLeast(1f)
                    val relativeY = ((currentCenterY - bounds.first) / height).coerceIn(0f, 1f)

                    val placement = when {
                        dragOffsetX > 36f || relativeY in 0.32f..0.68f -> FolderDropPlacement.INSIDE
                        relativeY < 0.32f -> FolderDropPlacement.ABOVE
                        else -> FolderDropPlacement.BELOW
                    }
                    return@derivedStateOf ActiveFolderDropTarget(
                        targetKey = node.collection.key,
                        targetName = node.collection.name,
                        placement = placement
                    )
                }
            }

            // If dragged below the last visible row, place BELOW the last valid visible node
            val lastValidNode = visibleNodes.lastOrNull { it.collection.key !in draggedForbiddenKeys }
            val lastBounds = lastValidNode?.let { rowBoundsMap[it.collection.key] }
            if (lastValidNode != null && lastBounds != null && currentCenterY > lastBounds.second) {
                return@derivedStateOf ActiveFolderDropTarget(
                    targetKey = lastValidNode.collection.key,
                    targetName = lastValidNode.collection.name,
                    placement = FolderDropPlacement.BELOW
                )
            }

            null
        }
    }

    val currentDropTargetState by rememberUpdatedState(activeDropTarget)

    val allKeys = remember(userCollections) { userCollections.map { it.key }.toSet() }
    val isAllSelected = allKeys.isNotEmpty() && selectedKeys.containsAll(allKeys)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("manage_folders_screen"),
        color = extColors.listCanvasBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // 1. Samsung Notes Style Top Bar (Browse Mode vs Selection Mode)
                if (!isSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("manage_folders_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Manage folders",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        TextButton(
                            onClick = {
                                isSelectionMode = true
                                selectedKeys = emptySet()
                            },
                            modifier = Modifier.testTag("manage_folders_select_button")
                        ) {
                            Text(
                                text = "Select",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Circular "All" Checkbox matching Samsung Notes
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedKeys = if (isAllSelected) emptySet() else allKeys
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("manage_folders_select_all")
                            ) {
                                SamsungCircularCheckbox(
                                    checked = isAllSelected,
                                    size = 24.dp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "All",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = if (selectedKeys.isEmpty()) {
                                    "Select folders"
                                } else {
                                    "${selectedKeys.size} selected"
                                },
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        TextButton(
                            onClick = {
                                isSelectionMode = false
                                selectedKeys = emptySet()
                            },
                            modifier = Modifier.testTag("manage_folders_done_selection_button")
                        ) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 2. Main Rounded Folder Tree Card (matching Samsung Notes card container)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState(), enabled = draggedKey == null)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = if (isSelectionMode || draggedKey != null) 120.dp else 32.dp
                        )
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = extColors.listCardSurface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val isRootDropHighlighted =
                                activeDropTarget?.placement == FolderDropPlacement.ROOT_TOP

                            // Root "Folders" Header Row (also serves as Top-Level drop zone when dragging)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coords ->
                                        val topY = coords.positionInRoot().y
                                        val bottomY = topY + coords.size.height
                                        rowBoundsMap[ROOT_HEADER_BOUNDS_KEY] = topY to bottomY
                                    }
                                    .background(
                                        if (isRootDropHighlighted) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .padding(
                                        start = if (isSelectionMode) 60.dp else 22.dp,
                                        end = 22.dp,
                                        top = 18.dp,
                                        bottom = 16.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        tint = if (isRootDropHighlighted) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Folders",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = if (isRootDropHighlighted) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                if (isRootDropHighlighted) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Drop for Top-Level",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = if (isRootDropHighlighted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                },
                                thickness = if (isRootDropHighlighted) 2.dp else 0.7.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )

                            // Hierarchical Folder Rows with Drag-and-Drop Reordering & Nesting
                            visibleNodes.forEach { node ->
                                val col = node.collection
                                val isSelected = col.key in selectedKeys
                                val isExpanded = expandedMap[col.key] ?: true
                                val isDraggingThis = draggedKey == col.key
                                val isDropTargetThis = activeDropTarget?.targetKey == col.key
                                val dropPlacementOnThis = if (isDropTargetThis) activeDropTarget?.placement else null
                                var showReorderMenu by remember { mutableStateOf(false) }

                                val customFolderColor = parseHexColor(col.colorHex)
                                val folderIconTint = customFolderColor
                                    ?: if (node.hasChildren) extColors.folderAccentTint else MaterialTheme.colorScheme.onSurfaceVariant

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isDraggingThis) 12f else 0f)
                                        .onGloballyPositioned { coords ->
                                            if (!isDraggingThis) {
                                                val topY = coords.positionInRoot().y
                                                val bottomY = topY + coords.size.height
                                                rowBoundsMap[col.key] = topY to bottomY
                                            }
                                        }
                                        .graphicsLayer {
                                            if (isDraggingThis) {
                                                translationY = dragOffsetY
                                                translationX = (dragOffsetX * 0.25f).coerceIn(-24f, 36f)
                                                scaleX = 1.02f
                                                scaleY = 1.02f
                                                shadowElevation = 18f
                                            }
                                        }
                                ) {
                                    // Top Insertion Line Indicator when dropping ABOVE this folder
                                    if (dropPlacementOnThis == FolderDropPlacement.ABOVE) {
                                        DropInsertionLineIndicator(
                                            indentDp = (node.level * 22 + 24).dp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                when {
                                                    isDraggingThis -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                                                    dropPlacementOnThis == FolderDropPlacement.INSIDE ->
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = when {
                                                    isDraggingThis -> 1.2.dp
                                                    dropPlacementOnThis == FolderDropPlacement.INSIDE -> 1.5.dp
                                                    else -> 0.dp
                                                },
                                                color = when {
                                                    isDraggingThis -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                                    dropPlacementOnThis == FolderDropPlacement.INSIDE ->
                                                        MaterialTheme.colorScheme.primary
                                                    else -> Color.Transparent
                                                },
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        selectedKeys = if (isSelected) {
                                                            selectedKeys - col.key
                                                        } else {
                                                            selectedKeys + col.key
                                                        }
                                                    } else if (node.hasChildren) {
                                                        expandedMap[col.key] = !isExpanded
                                                    } else {
                                                        isSelectionMode = true
                                                        selectedKeys = setOf(col.key)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!isSelectionMode) {
                                                        isSelectionMode = true
                                                        selectedKeys = selectedKeys + col.key
                                                    }
                                                }
                                            )
                                            .padding(
                                                start = 18.dp,
                                                end = 14.dp,
                                                top = 14.dp,
                                                bottom = 14.dp
                                            )
                                            .testTag("manage_folder_row_${col.key}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Selection Checkbox in Selection Mode
                                        if (isSelectionMode) {
                                            SamsungCircularCheckbox(
                                                checked = isSelected,
                                                size = 24.dp
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                        }

                                        // Hierarchy Indentation
                                        val indentDp = (node.level * 22).dp
                                        if (indentDp > 0.dp) {
                                            Spacer(modifier = Modifier.width(indentDp))
                                        }

                                        // Expand / Collapse Chevron
                                        if (node.hasChildren) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        expandedMap[col.key] = !isExpanded
                                                    }
                                                    .testTag("manage_folder_expand_${col.key}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) {
                                                        Icons.Default.KeyboardArrowDown
                                                    } else {
                                                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                                                    },
                                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(26.dp))
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Folder Icon (Filled if custom color is set, Outlined otherwise like Samsung Notes)
                                        Icon(
                                            imageVector = if (customFolderColor != null) {
                                                Icons.Default.Folder
                                            } else {
                                                Icons.Outlined.Folder
                                            },
                                            contentDescription = null,
                                            tint = folderIconTint,
                                            modifier = Modifier.size(23.dp)
                                        )

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // Folder Name (no paper counts per user preference)
                                        Text(
                                            text = col.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 16.sp,
                                                fontWeight = if (node.level == 0 && node.hasChildren) {
                                                    FontWeight.Medium
                                                } else {
                                                    FontWeight.Normal
                                                }
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (dropPlacementOnThis == FolderDropPlacement.INSIDE) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SubdirectoryArrowRight,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "Nest inside",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }

                                        // Drag-and-Drop + Quick Reorder Handle (`<>` UnfoldMore icon)
                                        Box {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .clickable(enabled = !isSelectionMode) {
                                                        showReorderMenu = true
                                                    }
                                                    .pointerInput(col.key, isSelectionMode) {
                                                        if (!isSelectionMode) {
                                                            detectDragGestures(
                                                                onDragStart = {
                                                                    showReorderMenu = false
                                                                    draggedKey = col.key
                                                                    dragOffsetY = 0f
                                                                    dragOffsetX = 0f
                                                                },
                                                                onDrag = { change, dragAmount ->
                                                                    change.consume()
                                                                    dragOffsetY += dragAmount.y
                                                                    dragOffsetX += dragAmount.x
                                                                },
                                                                onDragEnd = {
                                                                    val target = currentDropTargetState
                                                                    val activeKey = draggedKey
                                                                    if (activeKey != null && target != null) {
                                                                        if (target.placement == FolderDropPlacement.INSIDE && target.targetKey != null) {
                                                                            expandedMap[target.targetKey] = true
                                                                        }
                                                                        onDropReorderFolder(
                                                                            activeKey,
                                                                            target.targetKey,
                                                                            target.placement
                                                                        )
                                                                    }
                                                                    draggedKey = null
                                                                    dragOffsetY = 0f
                                                                    dragOffsetX = 0f
                                                                },
                                                                onDragCancel = {
                                                                    draggedKey = null
                                                                    dragOffsetY = 0f
                                                                    dragOffsetX = 0f
                                                                }
                                                            )
                                                        }
                                                    }
                                                    .testTag("manage_folder_reorder_${col.key}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.UnfoldMore,
                                                    contentDescription = "Drag or tap to reorder ${col.name}",
                                                    tint = if (isDraggingThis) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.size(21.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showReorderMenu,
                                                onDismissRequest = { showReorderMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Move up") },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowUpward,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    },
                                                    enabled = node.canMoveUp,
                                                    onClick = {
                                                        showReorderMenu = false
                                                        onReorderFolder(col.key, true)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Move down") },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDownward,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    },
                                                    enabled = node.canMoveDown,
                                                    onClick = {
                                                        showReorderMenu = false
                                                        onReorderFolder(col.key, false)
                                                    }
                                                )
                                                if (node.level > 0) {
                                                    DropdownMenuItem(
                                                        text = { Text("Move to Top-Level") },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Default.FolderSpecial,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        },
                                                        onClick = {
                                                            showReorderMenu = false
                                                            onDropReorderFolder(
                                                                col.key,
                                                                null,
                                                                FolderDropPlacement.ROOT_TOP
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Bottom Insertion Line Indicator when dropping BELOW this folder
                                    if (dropPlacementOnThis == FolderDropPlacement.BELOW) {
                                        DropInsertionLineIndicator(
                                            indentDp = (node.level * 22 + 24).dp
                                        )
                                    } else {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                            thickness = 0.7.dp,
                                            modifier = Modifier.padding(horizontal = 20.dp)
                                        )
                                    }
                                }
                            }

                            // Bottom "+ Create folder" Row (dimmed in Selection Mode like Samsung Notes)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (isSelectionMode) 0.42f else 1f)
                                    .clickable(enabled = !isSelectionMode) {
                                        createParentKey = null
                                        showCreateDialog = true
                                    }
                                    .padding(
                                        start = if (isSelectionMode) 56.dp else 44.dp,
                                        end = 22.dp,
                                        top = 18.dp,
                                        bottom = 18.dp
                                    )
                                    .testTag("manage_folders_create_row"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Create folder",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "Create folder",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (!isSelectionMode) {
                        Text(
                            text = "Tip: Drag the ↕ handle to reorder folders or drop onto another folder to nest it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Live Floating Drag Status Pill while dragging a folder
            AnimatedVisibility(
                visible = draggedKey != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                val draggedFolder = userCollections.firstOrNull { it.key == draggedKey }
                val target = activeDropTarget
                val statusLabel = when {
                    draggedFolder == null -> ""
                    target == null -> "Dragging \"${draggedFolder.name}\" — move over a folder to reorder or nest"
                    target.placement == FolderDropPlacement.ROOT_TOP ->
                        "Release to move \"${draggedFolder.name}\" to Top-Level"
                    target.placement == FolderDropPlacement.INSIDE ->
                        "Release to nest \"${draggedFolder.name}\" inside \"${target.targetName}\""
                    target.placement == FolderDropPlacement.ABOVE ->
                        "Release to place \"${draggedFolder.name}\" above \"${target.targetName}\""
                    else ->
                        "Release to place \"${draggedFolder.name}\" below \"${target.targetName}\""
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.testTag("manage_folders_drag_status_pill")
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }
            }

            // 3. Samsung Notes Floating Bottom 5-Action Pill Bar (Selection Mode)
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                val hasSelection = selectedKeys.isNotEmpty()
                val singleSelected = selectedKeys.size == 1

                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = extColors.listCardSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        width = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manage_folders_bottom_action_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FolderBottomActionItem(
                            icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                            label = "Move",
                            enabled = hasSelection,
                            testTag = "folder_action_move",
                            onClick = { showMoveDialog = true }
                        )
                        FolderBottomActionItem(
                            icon = Icons.Outlined.CreateNewFolder,
                            label = "Create subfolder",
                            enabled = singleSelected,
                            testTag = "folder_action_create_subfolder",
                            onClick = {
                                createParentKey = selectedKeys.firstOrNull()
                                showCreateDialog = true
                            }
                        )
                        FolderBottomActionItem(
                            icon = Icons.Outlined.Palette,
                            label = "Folder color",
                            enabled = hasSelection,
                            testTag = "folder_action_color",
                            onClick = { showColorDialog = true }
                        )
                        FolderBottomActionItem(
                            icon = Icons.Outlined.Edit,
                            label = "Rename",
                            enabled = singleSelected,
                            testTag = "folder_action_rename",
                            onClick = { showRenameDialog = true }
                        )
                        FolderBottomActionItem(
                            icon = Icons.Outlined.DeleteOutline,
                            label = "Delete",
                            enabled = hasSelection,
                            testTag = "folder_action_delete",
                            onClick = { showDeleteConfirmDialog = true }
                        )
                    }
                }
            }
        }
    }

    // --- Dialog 1: Create Folder / Create Subfolder ---
    if (showCreateDialog) {
        val parentFolder = userCollections.firstOrNull { it.key == createParentKey }
        var folderName by rememberSaveable { mutableStateOf("") }
        var selectedColorHex by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    if (parentFolder != null) {
                        "Create subfolder in \"${parentFolder.name}\""
                    } else {
                        "Create folder"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder name") },
                        placeholder = { Text("e.g. Literature Review") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manage_create_folder_input")
                    )

                    Text(
                        text = "Folder color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FolderColorSwatchRow(
                        selectedHex = selectedColorHex,
                        onSelectHex = { selectedColorHex = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateFolder(folderName, createParentKey, selectedColorHex)
                        createParentKey?.let { expandedMap[it] = true }
                        showCreateDialog = false
                        isSelectionMode = false
                        selectedKeys = emptySet()
                    },
                    enabled = folderName.isNotBlank(),
                    modifier = Modifier.testTag("manage_confirm_create_folder")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog 2: Rename Folder ---
    if (showRenameDialog && selectedKeys.size == 1) {
        val targetFolder = userCollections.firstOrNull { it.key == selectedKeys.first() }
        if (targetFolder != null) {
            var newName by rememberSaveable(targetFolder.key) { mutableStateOf(targetFolder.name) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename folder") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Folder name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manage_rename_folder_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onRenameFolder(targetFolder.key, newName)
                            showRenameDialog = false
                            isSelectionMode = false
                            selectedKeys = emptySet()
                        },
                        enabled = newName.isNotBlank(),
                        modifier = Modifier.testTag("manage_confirm_rename_folder")
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // --- Dialog 3: Folder Color Picker ---
    if (showColorDialog && selectedKeys.isNotEmpty()) {
        val firstSelected = userCollections.firstOrNull { it.key in selectedKeys }
        var chosenHex by remember(selectedKeys) { mutableStateOf(firstSelected?.colorHex) }

        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Folder color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose an icon color for the selected folder(s):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FolderColorSwatchRow(
                        selectedHex = chosenHex,
                        onSelectHex = { chosenHex = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateFolderColor(selectedKeys, chosenHex)
                        showColorDialog = false
                        isSelectionMode = false
                        selectedKeys = emptySet()
                    },
                    modifier = Modifier.testTag("manage_confirm_color_folder")
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog 4: Move Folder(s) Destination Picker ---
    if (showMoveDialog && selectedKeys.isNotEmpty()) {
        // Compute forbidden target folders (selected folders + all their descendants)
        val forbiddenKeys = remember(selectedKeys, userCollections) {
            val result = selectedKeys.toMutableSet()
            var changed = true
            while (changed) {
                changed = false
                for (c in userCollections) {
                    if (c.parentKey != null && c.parentKey in result && c.key !in result) {
                        result.add(c.key)
                        changed = true
                    }
                }
            }
            result
        }
        val availableParents = remember(userCollections, forbiddenKeys) {
            userCollections.filter { it.key !in forbiddenKeys }
        }
        var targetParentKey by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to folder") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { targetParentKey = null }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = targetParentKey == null,
                            onClick = { targetParentKey = null }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Folders (Top-Level)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    availableParents.forEach { destCol ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { targetParentKey = destCol.key }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = targetParentKey == destCol.key,
                                onClick = { targetParentKey = destCol.key }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = parseHexColor(destCol.colorHex) ?: extColors.folderAccentTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = destCol.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMoveFolders(selectedKeys, targetParentKey)
                        targetParentKey?.let { expandedMap[it] = true }
                        showMoveDialog = false
                        isSelectionMode = false
                        selectedKeys = emptySet()
                    },
                    modifier = Modifier.testTag("manage_confirm_move_folder")
                ) {
                    Text("Move here")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Dialog 5: Delete Folder(s) Confirmation (Moves papers to Trash) ---
    if (showDeleteConfirmDialog && selectedKeys.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    if (selectedKeys.size == 1) "Delete folder?" else "Delete ${selectedKeys.size} folders?"
                )
            },
            text = {
                Text(
                    text = "Deleting the selected folder(s) will also delete any subfolders and move all papers inside them to Trash.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFolders(selectedKeys)
                        showDeleteConfirmDialog = false
                        isSelectionMode = false
                        selectedKeys = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("manage_confirm_delete_folder")
                ) {
                    Text("Move to Trash & Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SamsungCircularCheckbox(
    checked: Boolean,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    val activeBlue = Color(0xFF2563EB)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) activeBlue else Color.Transparent)
            .border(
                width = if (checked) 0.dp else 1.8.dp,
                color = if (checked) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FolderBottomActionItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderColorSwatchRow(
    selectedHex: String?,
    onSelectHex: (String?) -> Unit
) {
    val extColors = ZoteroThemeTokens.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FolderColorPalette.forEach { (hex, label) ->
            val swatchColor = parseHexColor(hex) ?: extColors.folderAccentTint
            val isSelected = selectedHex == hex
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelectHex(hex) }
                    .testTag("folder_color_swatch_${label.lowercase().replace(" ", "_")}"),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DropInsertionLineIndicator(
    indentDp: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentDp, end = 18.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.5.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

