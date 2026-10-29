package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.DesignFilterChip
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.model.COFFEE_FOCUS_OPTIONS
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun FeedQuickFilterBar(
    nearbyOnly: Boolean,
    showNearby: Boolean,
    openOnly: Boolean,
    newOnly: Boolean,
    visitedOnly: Boolean,
    favoritesOnly: Boolean,
    onToggleOpen: () -> Unit,
    onToggleNew: () -> Unit,
    onToggleVisited: () -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleNearby: () -> Unit,
    coffeeFocusId: String?,
    onCoffeeFocusChange: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        .padding(horizontal = CpDimens.spacing4, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
        if (showNearby) DesignFilterChip(label = "Рядом", selected = nearbyOnly,
            onClick = onToggleNearby, leadingIcon = CpIcons.Distance)
        DesignFilterChip(label = "Открыто", selected = openOnly, onClick = onToggleOpen, leadingIcon = CpIcons.Time)
        DesignFilterChip(label = "Новые", selected = newOnly, onClick = onToggleNew, leadingIcon = CpIcons.Sparkle)
        DesignFilterChip(label = "Уже был", selected = visitedOnly, onClick = onToggleVisited, leadingIcon = CpIcons.CheckCircle)
        DesignFilterChip(label = "Избранное", selected = favoritesOnly, onClick = onToggleFavorites, leadingIcon = CpIcons.Favorite)
        COFFEE_FOCUS_OPTIONS.forEach { option ->
            DesignFilterChip(label = option.label, selected = coffeeFocusId == option.id, onClick = { onCoffeeFocusChange(option.id) })
        }
    }
}

@Composable
private fun FeedQuickFilterBarPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    FeedQuickFilterBar(false, true, true, false, false, true, {}, {}, {}, {}, {}, "specialty", {})
}

@Preview @Composable private fun FeedQuickFilterBarLightPreview() = FeedQuickFilterBarPreview(false)
@Preview @Composable private fun FeedQuickFilterBarDarkPreview() = FeedQuickFilterBarPreview(true)
