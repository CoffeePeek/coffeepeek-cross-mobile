package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_pagination_error
import com.coffeepeek.core.designsystem.resources.coffeepeek_pagination_retry
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** A list footer that preserves loaded content and offers an explicit retry after a page fails. */
@Composable
fun CoffeePeekPaginationStatus(
    isLoading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    fallbackMessage: String = stringResource(Res.string.coffeepeek_pagination_error),
    retryLabel: String = stringResource(Res.string.coffeepeek_pagination_retry),
) {
    if (!isLoading && !hasError) return
    Column(
        modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
    ) {
        if (isLoading) CoffeePeekLoader() else {
            Text(errorMessage?.takeIf { it.isNotBlank() } ?: fallbackMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            CoffeePeekTextButton(onClick = onRetry) { Text(retryLabel) }
        }
    }
}

@Composable
private fun CoffeePeekPaginationStatusPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        Column {
            CoffeePeekPaginationStatus(isLoading = false, hasError = true, onRetry = {})
            CoffeePeekPaginationStatus(isLoading = true, hasError = false, onRetry = {})
        }
    }
}

@Preview @Composable private fun CoffeePeekPaginationStatusLightPreview() = CoffeePeekPaginationStatusPreview(false)
@Preview @Composable private fun CoffeePeekPaginationStatusDarkPreview() = CoffeePeekPaginationStatusPreview(true)
