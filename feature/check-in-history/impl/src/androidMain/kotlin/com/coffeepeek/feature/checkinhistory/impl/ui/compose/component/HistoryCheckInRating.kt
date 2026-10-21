package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
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
    CoffeePeekRating(average, modifier, valueText = label, contentDescription = description)
}

@PreviewLightDark @Composable private fun HistoryCheckInRatingPreview() = CoffeePeekTheme {
    HistoryCheckInRating(HistoryRating(4, 4, 5))
}
