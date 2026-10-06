package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.formatOneDecimal
import com.coffeepeek.domain.model.Review
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.checkin_rating_coffee
import coffeepeek.composeapp.generated.resources.checkin_rating_service
import coffeepeek.composeapp.generated.resources.checkin_rating_atmosphere
import org.jetbrains.compose.resources.painterResource

internal data class ReviewAverages(val coffee: Double, val service: Double, val place: Double) {
    val overall: Double get() = (coffee + service + place) / 3
}

internal fun averageReviewRatings(reviews: List<Review>): ReviewAverages? =
    if (reviews.isEmpty()) null else ReviewAverages(
        coffee = reviews.map { it.rating.coffee }.average(),
        service = reviews.map { it.rating.service }.average(),
        place = reviews.map { it.rating.place }.average(),
    )

@Composable
internal fun ReviewRatingsOverview(reviews: List<Review>, overallRating: Double?, reviewCount: Int) {
    val averages = averageReviewRatings(reviews) ?: return
    Column(
        Modifier.fillMaxWidth().padding(vertical = CpDimens.spacing3),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(CpIcons.StarFilled, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                "${formatOneDecimal(overallRating?.takeIf { it > 0 } ?: averages.overall)} · Отзывы: ${maxOf(reviewCount, reviews.size)}",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            )
        }
        if (reviewCount > reviews.size) Text(
            "Оценки по загруженным отзывам", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf(
            Triple("Кофе", averages.coffee, Res.drawable.checkin_rating_coffee),
            Triple("Сервис", averages.service, Res.drawable.checkin_rating_service),
            Triple("Аура", averages.place, Res.drawable.checkin_rating_atmosphere),
        ).forEach { (label, value, mascot) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.width(112.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painterResource(mascot), contentDescription = null, modifier = Modifier.size(28.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
                Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth((value / 5).toFloat().coerceIn(0f, 1f)).height(6.dp).background(MaterialTheme.colorScheme.primary))
                }
                Text(formatOneDecimal(value), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
