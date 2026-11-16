package org.zotero.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.zotero.android.ui.theme.ZoteroThemeTokens

@Composable
fun SpeedDialFabMenu(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDismiss: () -> Unit,
    onAddByIdentifier: () -> Unit,
    onManualPaper: () -> Unit,
    onQuickNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extColors = ZoteroThemeTokens.colors
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        label = "fab_rotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim overlay when FAB menu is expanded
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (extColors.isDark) 0.5f else 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .testTag("fab_menu_scrim")
            )
        }

        // Stacked Speed-Dial Pills + Main Rounded-Square FAB
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpeedDialMenuItem(
                        label = "Quick Research Note",
                        icon = Icons.AutoMirrored.Filled.StickyNote2,
                        testTag = "fab_action_quick_note",
                        onClick = {
                            onDismiss()
                            onQuickNote()
                        }
                    )
                    SpeedDialMenuItem(
                        label = "Manual Paper",
                        icon = Icons.Default.EditNote,
                        testTag = "fab_action_manual_paper",
                        onClick = {
                            onDismiss()
                            onManualPaper()
                        }
                    )
                    SpeedDialMenuItem(
                        label = "Add by DOI or ISBN",
                        icon = Icons.Default.AutoFixHigh,
                        testTag = "fab_action_identifier",
                        onClick = {
                            onDismiss()
                            onAddByIdentifier()
                        }
                    )
                }
            }

            FloatingActionButton(
                onClick = onToggleExpanded,
                shape = RoundedCornerShape(18.dp),
                containerColor = extColors.accentPill,
                contentColor = extColors.onAccentPill,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .size(58.dp)
                    .testTag("speed_dial_main_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (expanded) "Close Menu" else "Expand Add Menu",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun SpeedDialMenuItem(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val extColors = ZoteroThemeTokens.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            shadowElevation = 3.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        SmallFloatingActionButton(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            containerColor = extColors.accentPill,
            contentColor = extColors.onAccentPill,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
