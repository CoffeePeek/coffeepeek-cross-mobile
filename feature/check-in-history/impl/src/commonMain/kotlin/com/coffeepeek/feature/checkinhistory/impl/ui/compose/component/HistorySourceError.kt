package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistorySourceError(message: String, onRetry: () -> Unit) {
    val retryLabel = stringResource(Res.string.history_retry_for, message)
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        TextButton(onRetry, Modifier.semantics { contentDescription = retryLabel }) {
            Text(stringResource(Res.string.history_retry))
        }
    }
}

@Composable private fun HistorySourceErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistorySourceError(stringResource(Res.string.history_home_timeline_error), {})
}
@Preview @Composable private fun HistorySourceErrorLightPreview() = HistorySourceErrorPreview(false)
@Preview @Composable private fun HistorySourceErrorDarkPreview() = HistorySourceErrorPreview(true)
