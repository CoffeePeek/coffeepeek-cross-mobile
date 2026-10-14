package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryRating
import com.coffeepeek.feature.checkinhistory.impl.resources.Res
import com.coffeepeek.feature.checkinhistory.impl.resources.history_rating_description
import com.coffeepeek.feature.checkinhistory.impl.resources.history_rating_missing
import com.coffeepeek.feature.checkinhistory.impl.resources.history_rating_decimal
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCheckInRating(rating: HistoryRating, modifier: Modifier = Modifier) {
    val scores = listOfNotNull(rating.coffee, rating.service, rating.place).filter { it in 1..5 }
    if (scores.isEmpty()) return
    val average = scores.average()
    val tenths = (average * 10).roundToInt()
    val label = stringResource(Res.string.history_rating_decimal, tenths / 10, tenths % 10)
    val missing = stringResource(Res.string.history_rating_missing)
    val description = stringResource(Res.string.history_rating_description,
        rating.coffee?.toString() ?: missing, rating.service?.toString() ?: missing, rating.place?.toString() ?: missing, label)
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            (1..5).forEach { star ->
                Icon(if (star <= average.toInt()) CoffeePeekIcons.StarFilled else CoffeePeekIcons.Star,
                    null, Modifier.size(20.dp), tint = if (star <= average.toInt()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline)
            }
        }
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@PreviewLightDark @Composable private fun HistoryCheckInRatingPreview() = CoffeePeekTheme {
    HistoryCheckInRating(HistoryRating(4, 4, 5))
}
