package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_refresh_error
import com.coffeepeek.core.designsystem.resources.coffeepeek_pagination_retry
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** A refresh failure above retained content, using the shared list retry presentation. */
@Composable
fun CoffeePeekRefreshError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = stringResource(Res.string.coffeepeek_refresh_error),
    retryLabel: String = stringResource(Res.string.coffeepeek_pagination_retry),
) {
    CoffeePeekPaginationStatus(
        isLoading = false,
        hasError = true,
        onRetry = onRetry,
        modifier = modifier,
        fallbackMessage = message,
        retryLabel = retryLabel,
    )
}

@Composable
private fun CoffeePeekRefreshErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeePeekRefreshError(onRetry = {}) }
}

@Preview @Composable private fun CoffeePeekRefreshErrorLightPreview() = CoffeePeekRefreshErrorPreview(false)
@Preview @Composable private fun CoffeePeekRefreshErrorDarkPreview() = CoffeePeekRefreshErrorPreview(true)
