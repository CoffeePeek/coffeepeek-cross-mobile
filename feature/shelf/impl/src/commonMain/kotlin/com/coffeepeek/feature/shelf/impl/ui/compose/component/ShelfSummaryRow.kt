package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_archive
import com.coffeepeek.feature.shelf.impl.resources.shelf_drag_hint
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfSummary
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** "7 пачек · ≈ 1,9 кг · 5 обжарщиков" and the archive button; while a bag is lifted, a hint instead. */
@Composable
internal fun ShelfSummaryRow(summary: ShelfSummary?, dragging: Boolean, onArchive: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        val text = if (dragging) stringResource(Res.string.shelf_drag_hint) else summary?.text().orEmpty()
        Text(text, Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onArchive != null) IconButton(onArchive) {
            Icon(CoffeePeekIcons.Archive, stringResource(Res.string.shelf_archive), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ShelfSummaryRowPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4)) {
        ShelfSummaryRow(ShelfSummary(bags = 7, grams = 1900, roasters = 5), dragging = false, onArchive = {})
        ShelfSummaryRow(ShelfSummary(bags = 7, grams = 1900, roasters = 5), dragging = true, onArchive = {})
    }
}

@Preview @Composable private fun ShelfSummaryRowLightPreview() = ShelfSummaryRowPreview(false)
@Preview @Composable private fun ShelfSummaryRowDarkPreview() = ShelfSummaryRowPreview(true)
