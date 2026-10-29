package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekSelectionCard
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistorySource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCreateSheet(onDismiss: () -> Unit, onCreate: (HistorySource) -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, stringResource(Res.string.history_close)) {
        HistoryCreateContent(onCreate, modifier)
    }
}

@Composable private fun HistoryCreateContent(onCreate: (HistorySource) -> Unit, modifier: Modifier = Modifier) {
    var source by rememberSaveable { mutableStateOf(HistorySource.Shop) }
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.history_new), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(Res.string.history_where), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HistorySource.entries.forEach { option ->
                CoffeePeekSelectionCard(
                    stringResource(if (option == HistorySource.Shop) Res.string.history_at_shop else Res.string.history_at_home),
                    stringResource(if (option == HistorySource.Shop) Res.string.history_create_shop_hint else Res.string.history_create_home_hint),
                    if (option == HistorySource.Shop) CoffeePeekIcons.Location else CoffeePeekIcons.Coffee,
                    source == option, { source = option },
                )
            }
        }
        CoffeePeekButton(stringResource(Res.string.history_continue), { onCreate(source) })
    }
}

@Preview @Composable private fun HistoryCreateLightPreview() = CoffeePeekTheme(darkTheme = false) { HistoryCreateContent({}) }
@Preview @Composable private fun HistoryCreateDarkPreview() = CoffeePeekTheme(darkTheme = true) { HistoryCreateContent({}) }
