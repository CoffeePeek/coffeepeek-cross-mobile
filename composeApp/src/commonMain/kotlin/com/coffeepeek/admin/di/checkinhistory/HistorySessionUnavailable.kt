package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.history_session_unavailable
import coffeepeek.composeapp.generated.resources.history_session_retry
import coffeepeek.composeapp.generated.resources.history_session_back
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun HistorySessionUnavailable(onRetry: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(CoffeePeekDimens.spacing6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            Text(stringResource(Res.string.history_session_unavailable), color = MaterialTheme.colorScheme.error)
            CoffeePeekButton(stringResource(Res.string.history_session_retry), onRetry)
            CoffeePeekButton(stringResource(Res.string.history_session_back), onBack)
        }
    }
}

@Composable
private fun HistorySessionUnavailablePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) { HistorySessionUnavailable({}, {}) }

@Preview @Composable private fun HistorySessionUnavailableLightPreview() = HistorySessionUnavailablePreview(false)
@Preview @Composable private fun HistorySessionUnavailableDarkPreview() = HistorySessionUnavailablePreview(true)
