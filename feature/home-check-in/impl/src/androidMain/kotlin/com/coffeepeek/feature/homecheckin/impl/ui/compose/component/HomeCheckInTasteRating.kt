package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomeCheckInTasteRating(score: Int) {
    val description = stringResource(R.string.home_check_in_rating_description, score)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.home_check_in_rating), style = MaterialTheme.typography.titleSmall)
        CoffeePeekRating(score.toDouble(), contentDescription = description, valueText = "$score/5",
            starSpacing = 4.dp, valueSpacing = 10.dp, valueStyle = MaterialTheme.typography.labelLarge,
            inactiveColor = MaterialTheme.colorScheme.outlineVariant)
    }
}

@PreviewLightDark @Composable private fun HomeCheckInTasteRatingPreview() = CoffeePeekTheme {
    Surface { HomeCheckInTasteRating(4) }
}
