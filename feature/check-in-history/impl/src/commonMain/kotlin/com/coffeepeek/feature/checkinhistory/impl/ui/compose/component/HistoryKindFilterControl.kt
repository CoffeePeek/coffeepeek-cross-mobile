package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryKindFilter
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryKindFilterControl(filter: HistoryKindFilter, onSelect: (HistoryKindFilter) -> Unit,
    modifier: Modifier = Modifier) {
    val labels = mapOf(HistoryKindFilter.All to stringResource(Res.string.history_all),
        HistoryKindFilter.Shop to stringResource(Res.string.history_at_shop),
        HistoryKindFilter.Home to stringResource(Res.string.history_at_home))
    CoffeePeekCapsuleSegmentedControl(HistoryKindFilter.entries, filter, { labels.getValue(it) }, onSelect, modifier)
}

@Preview @Composable private fun HistoryKindFilterPreview() = CoffeePeekTheme {
    HistoryKindFilterControl(HistoryKindFilter.All, {})
}
