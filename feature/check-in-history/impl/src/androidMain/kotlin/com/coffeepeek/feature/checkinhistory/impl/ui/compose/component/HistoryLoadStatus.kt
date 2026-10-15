package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryLoadStatus(isLoading: Boolean, hasError: Boolean, hasContent: Boolean, onRetry: () -> Unit,
    modifier: Modifier = Modifier, paging: Boolean = false) {
    if (!isLoading && !hasError) return
    Column(modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing3), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        if (isLoading) CoffeePeekLoader(stringResource(Res.string.history_loading), size = CoffeePeekDimens.loaderButton)
        if (hasError) {
            Text(stringResource(when {
                paging -> Res.string.history_paging_error
                hasContent -> Res.string.history_refresh_error
                else -> Res.string.history_load_error
            }), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            CoffeePeekButton(stringResource(Res.string.history_retry), onRetry, enabled = !isLoading)
        }
    }
}

@PreviewLightDark @Composable private fun HistoryLoadStatusPreview() = CoffeePeekTheme {
    HistoryLoadStatus(false, true, true, {})
}
