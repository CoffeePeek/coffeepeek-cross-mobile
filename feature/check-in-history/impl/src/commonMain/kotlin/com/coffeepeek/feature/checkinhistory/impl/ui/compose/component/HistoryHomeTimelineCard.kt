package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekTimelineContent
import com.coffeepeek.core.designsystem.component.CoffeePeekTimelineHeader
import com.coffeepeek.core.designsystem.component.CoffeePeekTimelineFooter
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyBrewMethodLabel
import com.coffeepeek.feature.checkinhistory.impl.ui.data.visibilityLabel
import com.coffeepeek.feature.checkinhistory.impl.ui.data.timelinePhotos
import com.coffeepeek.feature.checkinhistory.impl.ui.data.timelineDose
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryHomeTimelineCard(entry: HomeHistoryEntry, dateLabel: String, onClick: () -> Unit,
    onPhoto: (Int) -> Unit, image: HistoryImageContent?) {
    val photos = entry.timelinePhotos()
    val status = entry.visibilityLabel()
    val drink = entry.customDrinkName?.takeIf(String::isNotBlank) ?: entry.drinkName?.takeIf(String::isNotBlank)
    CoffeePeekTimelineContent(entry.note.orEmpty(), Modifier.clickable(role = Role.Button, onClick = onClick),
        header = { CoffeePeekTimelineHeader(stringResource(Res.string.history_you), dateLabel, avatar = {}) },
        heading = { HistoryTimelineHomeRecipe(entry.recipe) },
        photos = { HistoryTimelinePhotosContent(photos, onPhoto, image) },
        supporting = status?.let { { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        footer = { CoffeePeekTimelineFooter(null, false, "", drink) })
}

@Composable
private fun HistoryTimelineHomeRecipe(recipe: HomeHistoryRecipe) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recipe.beanName?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        val details = listOfNotNull(recipe.brewMethodCode?.let { historyBrewMethodLabel(it) },
            recipe.timelineDose()?.let { stringResource(Res.string.history_dose, it.toString()) },
            recipe.brewDurationSeconds?.let { stringResource(Res.string.history_seconds, it) },
            recipe.tasteScore?.let { stringResource(Res.string.history_taste, it) })
            .joinToString(stringResource(Res.string.history_recipe_separator))
        if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun previewHomeHistoryEntry() = HomeHistoryEntry("home", "2026-10-14", "2026-10-14T08:00:00Z",
    "2026-10-14T08:00:00Z", "Сладкий и сбалансированный кофе.",
    HomeHistoryRecipe("hario-v60", null, "Эфиопия", "18.00", 180, 4), null, null, null, emptyList(),
    HistoryVisibility.Private, HistoryModeration.NotSubmitted)

@Composable private fun HomeTimelinePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryHomeTimelineCard(previewHomeHistoryEntry(), "14 октября 2026", {}, {}, null)
}
@Preview @Composable private fun HomeTimelineLightPreview() = HomeTimelinePreview(false)
@Preview @Composable private fun HomeTimelineDarkPreview() = HomeTimelinePreview(true)
