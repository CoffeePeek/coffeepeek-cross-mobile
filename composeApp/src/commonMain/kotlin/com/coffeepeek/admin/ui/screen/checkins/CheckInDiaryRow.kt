package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.formatOneDecimal
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.savedDrinkName

@Composable
internal fun CheckInDiaryRow(checkIn: CheckIn, onClick: () -> Unit) {
    val drink = savedDrinkName(checkIn.drinkNameRu, checkIn.drinkNameEn, checkIn.customDrinkName, Locale.current.language)
    val time = utcIsoToLocalDateTime(checkIn.visitedAt.ifBlank { checkIn.createdAt }).substringAfterLast(' ', "")
    BoxWithConstraints(Modifier.fillMaxWidth().clickable(role = Role.Button,
        onClickLabel = "Открыть чекин", onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp)) {
        val ratingBelow = maxWidth < 310.dp || LocalDensity.current.fontScale > 1.2f
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CheckInDiaryThumbnail(checkIn, Modifier.size(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(checkIn.shopName.ifBlank { "Кофейня" }, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                drink?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                if (time.isNotBlank()) Text(time, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ratingBelow) DiaryRating(checkIn)
            }
            if (!ratingBelow) DiaryRating(checkIn)
            Icon(CpIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DiaryRating(checkIn: CheckIn) {
    checkIn.rating?.let { rating ->
        val label = formatOneDecimal(rating.average).replace('.', ',')
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Оценка $label из 5" }) {
            Icon(CpIcons.StarFilled, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal fun diaryPhoto(checkIn: CheckIn): String? =
    (checkIn.photoCalendarUrls ?: checkIn.photoThumbnailUrls.ifEmpty { checkIn.photoUrls }).firstOrNull(String::isNotBlank)

@Composable
internal fun CheckInDiaryThumbnail(checkIn: CheckIn, modifier: Modifier = Modifier) {
    val photo = diaryPhoto(checkIn)
    if (photo != null) {
        CpImage(data = photo, contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(10.dp)))
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) {
            Icon(CpIcons.Coffee, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxSize(0.5f))
        }
    }
}
