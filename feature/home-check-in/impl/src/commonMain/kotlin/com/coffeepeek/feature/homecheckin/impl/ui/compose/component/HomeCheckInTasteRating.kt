package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_rating
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_rating_description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun HomeCheckInTasteRating(score: Int) {
    val description = stringResource(Res.string.home_check_in_rating_description, score)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.home_check_in_rating), style = MaterialTheme.typography.titleSmall)
        CoffeePeekRating(score.toDouble(), contentDescription = description, valueText = "$score/5",
            starSpacing = 4.dp, valueSpacing = 10.dp, valueStyle = MaterialTheme.typography.labelLarge,
            inactiveColor = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable private fun HomeCheckInTasteRatingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInTasteRating(4) }
}
@Preview @Composable private fun HomeCheckInTasteRatingLightPreview() = HomeCheckInTasteRatingPreview(false)
@Preview @Composable private fun HomeCheckInTasteRatingDarkPreview() = HomeCheckInTasteRatingPreview(true)
