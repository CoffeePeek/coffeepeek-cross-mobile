package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal enum class PlatformMenuIcon { Edit, Hide, Report, NoteEdit, Add, CoffeeBean, Logout, Block }

/** Presentation state only. A nullable selection distinguishes commands from single-choice lists. */
internal data class PlatformMenuItem(
    val id: String,
    val title: String,
    val enabled: Boolean = true,
    val selected: Boolean? = null,
    val icon: PlatformMenuIcon? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/** Native menus may deliver a callback after recomposition or release: use the current gate and current items, not captured actions. */
internal fun performPlatformMenuAction(items: List<PlatformMenuItem>, id: String, enabled: Boolean) {
    if (enabled) items.firstOrNull { it.id == id && it.enabled }?.onClick?.invoke()
}

/** A menu belongs to its trigger, including when the trigger moves inside a scrolling list. */
@Composable
internal expect fun PlatformMenuButton(
    items: List<PlatformMenuItem>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueDescription: String? = null,
    content: @Composable (onClick: () -> Unit) -> Unit,
)
