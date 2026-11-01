package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun HistoryShopTimelineCard(entry: PersonalCheckIn, timeLabel: String, onClick: () -> Unit) {
    LocalHistoryShopTimelineContent.current?.let { content ->
        content(entry, onClick)
        return
    }
    Card(onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(CoffeePeekIcons.Location, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.history_source_time, stringResource(Res.string.history_at_shop), timeLabel),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(entry.shopName.ifBlank { stringResource(Res.string.history_shop) },
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            // Match the current timeline's saved-name order, rather than changing it with device locale.
            val drink = entry.drink?.let { it.nameRu ?: it.customName ?: it.nameEn }
            if (!drink.isNullOrBlank()) Text(drink, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
            if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodyMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun HistoryShopTimelineCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryShopTimelineCard(previewHistoryCheckIn(), "13:00", {})
}
@Preview @Composable private fun HistoryShopTimelineCardLightPreview() = HistoryShopTimelineCardPreview(false)
@Preview @Composable private fun HistoryShopTimelineCardDarkPreview() = HistoryShopTimelineCardPreview(true)
