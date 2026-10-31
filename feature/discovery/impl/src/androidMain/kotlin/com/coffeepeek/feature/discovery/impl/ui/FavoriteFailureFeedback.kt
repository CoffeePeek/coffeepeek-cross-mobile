package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.R

/** State survives hidden overlays; SnackbarHost owns its accessible timeout/live region. */
@Composable
internal fun FavoriteFailureFeedback(failureId: Long?, onDismiss: (Long) -> Unit, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    val message = stringResource(R.string.discovery_favorite_failed)
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(failureId, message) {
        if (failureId != null) {
            snackbar.showSnackbar(message, duration = SnackbarDuration.Long, withDismissAction = true)
            dismiss(failureId)
        }
    }
    SnackbarHost(snackbar, modifier) { data -> FavoriteFailureNotice(data.visuals.message, data::dismiss) }
}

@Composable
private fun FavoriteFailureNotice(message: String, onDismiss: () -> Unit) {
    CoffeePeekContentCard(Modifier.widthIn(max = 560.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
            CoffeePeekIconButton(onDismiss) {
                Icon(CoffeePeekIcons.Close, stringResource(R.string.discovery_message_dismiss), Modifier.size(20.dp))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FavoriteFailureFeedbackPreview() = CoffeePeekTheme {
    FavoriteFailureNotice(stringResource(R.string.discovery_favorite_failed), {})
}
