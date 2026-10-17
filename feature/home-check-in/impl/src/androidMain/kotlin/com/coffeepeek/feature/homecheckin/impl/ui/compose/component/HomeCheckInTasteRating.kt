package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomeCheckInTasteRating(score: Int) {
    val description = stringResource(R.string.home_check_in_rating_description, score)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.home_check_in_rating), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.semantics(mergeDescendants = true) { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { index ->
                Icon(if (index < score) CoffeePeekIcons.StarFilled else CoffeePeekIcons.StarOutline, null,
                    Modifier.size(20.dp), tint = if (index < score) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            }
            Text("$score/5", Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@PreviewLightDark @Composable private fun HomeCheckInTasteRatingPreview() = CoffeePeekTheme {
    Surface { HomeCheckInTasteRating(4) }
}
