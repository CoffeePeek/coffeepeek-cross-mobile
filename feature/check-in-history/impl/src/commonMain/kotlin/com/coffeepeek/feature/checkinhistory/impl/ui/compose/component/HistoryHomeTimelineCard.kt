package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyBrewMethodLabel
import com.coffeepeek.feature.checkinhistory.impl.ui.data.visibilityLabel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryHomeTimelineCard(entry: HomeHistoryEntry, timeLabel: String, onClick: () -> Unit) {
    LocalHistoryHomeTimelineContent.current?.let { content ->
        content(entry, onClick)
        return
    }
    Card(onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(CoffeePeekIcons.Coffee, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(Res.string.history_source_time, stringResource(Res.string.history_at_home), timeLabel),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                entry.visibilityLabel()?.let { Text(it, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(entry.recipe.beanName?.takeIf(String::isNotBlank) ?: stringResource(Res.string.history_home_title),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val method = entry.recipe.brewMethodCode?.let { historyBrewMethodLabel(it) }
            val dose = entry.recipe.doseGrams?.let {
                stringResource(Res.string.history_dose, (if ('.' in it) it.trimEnd('0').trimEnd('.') else it).replace('.', ','))
            }
            val details = if (method != null && dose != null) stringResource(Res.string.history_details_separator, method, dose)
                else method ?: dose
            if (!details.isNullOrBlank()) Text(details, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            entry.recipe.tasteScore?.let { score ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(CoffeePeekIcons.StarFilled, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(Res.string.history_taste, score), style = MaterialTheme.typography.labelMedium)
                }
            }
            entry.note?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal fun previewHomeHistoryEntry() = HomeHistoryEntry("home", "2026-10-14", "2026-10-14T08:00:00Z",
    "2026-10-14T08:00:00Z", "Сладкий и сбалансированный кофе.",
    HomeHistoryRecipe("hario-v60", null, "Эфиопия", "18.00", 180, 4), null, null, null, emptyList(),
    HistoryVisibility.Private, HistoryModeration.NotSubmitted)

@Preview @Composable private fun HistoryHomeTimelineCardPreview() = CoffeePeekTheme {
    HistoryHomeTimelineCard(previewHomeHistoryEntry(), "11:00", {})
}
