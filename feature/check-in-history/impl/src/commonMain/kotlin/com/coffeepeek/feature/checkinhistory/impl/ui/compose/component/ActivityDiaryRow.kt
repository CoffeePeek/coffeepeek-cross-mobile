package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryTimelineItem
import com.coffeepeek.feature.checkinhistory.impl.ui.data.displayName
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
internal fun ActivityDiaryRow(row: HistoryTimelineItem, image: HistoryImageContent, timeLabel: (String) -> String, onClick: () -> Unit) {
    val title = when (row) {
        is HistoryTimelineItem.Shop -> row.entry.shopName.ifBlank { stringResource(Res.string.history_shop) }
        is HistoryTimelineItem.Home -> stringResource(Res.string.history_activity_at_home)
    }
    val drink = when (row) {
        is HistoryTimelineItem.Shop -> row.entry.drink.displayName(Locale.current.language)
        is HistoryTimelineItem.Home -> row.entry.customDrinkName?.takeIf(String::isNotBlank) ?: row.entry.drinkName
    }
    BoxWithConstraints(Modifier.fillMaxWidth().clickable(role = Role.Button,
        onClickLabel = stringResource(Res.string.history_activity_open), onClick = onClick).padding(12.dp)) {
        val ratingBelow = maxWidth < 310.dp || LocalDensity.current.fontScale > 1.2f
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActivityThumbnail(row, image, Modifier.size(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!drink.isNullOrBlank()) Text(drink, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val time = timeLabel(row.timestamp)
                if (time.isNotBlank()) Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ratingBelow) ActivityRating(row)
            }
            if (!ratingBelow) ActivityRating(row)
            Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityThumbnail(row: HistoryTimelineItem, image: HistoryImageContent, modifier: Modifier) {
    val photo = when (row) {
        is HistoryTimelineItem.Shop -> (row.entry.photoCalendarUrls ?: row.entry.photoThumbnailUrls ?: row.entry.photoUrls)
            .firstOrNull(String::isNotBlank)
        is HistoryTimelineItem.Home -> row.entry.photos.firstNotNullOfOrNull { it.calendarUrl?.takeIf(String::isNotBlank) }
    }
    if (photo != null) image(photo, modifier.clip(RoundedCornerShape(10.dp)), ContentScale.Crop)
    else Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center) {
        Icon(CoffeePeekIcons.Coffee, null, Modifier.fillMaxSize(0.5f), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActivityRating(row: HistoryTimelineItem) {
    val rating = (row as? HistoryTimelineItem.Shop)?.entry?.rating ?: return
    val scores = listOfNotNull(rating.coffee, rating.service, rating.place)
    if (scores.isEmpty()) return
    val tenths = (scores.average() * 10).roundToInt()
    CoffeePeekRating(scores.average(), display = CoffeePeekRatingDisplay.Compact,
        size = CoffeePeekRatingSize.Small, valueText = stringResource(Res.string.history_rating_decimal, tenths / 10, tenths % 10),
        valueSpacing = 4.dp)
}

@Composable private fun ActivityDiaryPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ActivityDiaryRow(HistoryTimelineItem.Shop(previewHistoryCheckIn(), "2026-10-25"), { _, _, _ -> }, { "13:00" }, {})
}
@Preview @Composable private fun ActivityDiaryLightPreview() = ActivityDiaryPreview(false)
@Preview @Composable private fun ActivityDiaryDarkPreview() = ActivityDiaryPreview(true)
