package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekActionList
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistorySource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun HistoryCreateSheet(onDismiss: () -> Unit, onCreate: (HistorySource) -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, stringResource(Res.string.history_close)) {
        HistoryCreateContent(onCreate, modifier)
    }
}

@Composable
private fun HistoryCreateContent(onCreate: (HistorySource) -> Unit, modifier: Modifier = Modifier) {
    val actions = listOf(
        CoffeePeekMenuAction(stringResource(Res.string.history_create_shop), CoffeePeekIcons.Location) {
            onCreate(HistorySource.Shop)
        },
        CoffeePeekMenuAction(stringResource(Res.string.history_create_home), CoffeePeekIcons.Coffee) {
            onCreate(HistorySource.Home)
        },
    )
    Column(modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 16.dp)
        .verticalScroll(rememberScrollState())) {
        Text(stringResource(Res.string.history_create_title), style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() })
        CoffeePeekActionList(actions.map { listOf(it) })
    }
}

@Composable private fun HistoryCreatePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) { HistoryCreateContent({}) }
@Preview @Composable private fun HistoryCreateLightPreview() = HistoryCreatePreview(false)
@Preview @Composable private fun HistoryCreateDarkPreview() = HistoryCreatePreview(true)
