package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal enum class PlatformMenuIcon { Edit, Hide, Report }

/** Presentation state only. A nullable selection distinguishes commands from single-choice lists. */
internal data class PlatformMenuItem(
    val id: String,
    val title: String,
    val enabled: Boolean = true,
    val selected: Boolean? = null,
    val icon: PlatformMenuIcon? = null,
    val onClick: () -> Unit,
)

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
