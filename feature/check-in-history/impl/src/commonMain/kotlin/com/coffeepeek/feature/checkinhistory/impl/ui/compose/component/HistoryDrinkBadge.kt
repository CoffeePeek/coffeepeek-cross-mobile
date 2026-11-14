package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekDrinkLabel
import com.coffeepeek.core.designsystem.component.CoffeePeekDrinkLabelSize
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun HistoryDrinkBadge(name: String, modifier: Modifier = Modifier) =
    CoffeePeekDrinkLabel(name, modifier, size = CoffeePeekDrinkLabelSize.Compact)

@Composable private fun HistoryDrinkBadgePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryDrinkBadge("Капучино")
}
@Preview @Composable private fun HistoryDrinkBadgeLightPreview() = HistoryDrinkBadgePreview(false)
@Preview @Composable private fun HistoryDrinkBadgeDarkPreview() = HistoryDrinkBadgePreview(true)
