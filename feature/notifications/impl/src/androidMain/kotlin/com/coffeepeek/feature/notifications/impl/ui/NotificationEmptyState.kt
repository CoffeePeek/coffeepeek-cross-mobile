package com.coffeepeek.feature.notifications.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.coffeepeek.feature.notifications.impl.R
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun NotificationEmptyState(signedOut: Boolean = false, onSignIn: () -> Unit = {}, description: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = CoffeePeekDimens.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        CoffeePeekIconBadge(if (signedOut) CoffeePeekIcons.Notifications else CoffeePeekIcons.Coffee, CoffeePeekIconBadgePalette.Gold)
        Text(stringResource(if (signedOut) R.string.notifications_sign_in_title else R.string.notifications_empty_title),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() })
        Text(description ?: stringResource(if (signedOut) R.string.notifications_sign_in_description else R.string.notifications_empty_description),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        if (signedOut) CoffeePeekButton(stringResource(R.string.notifications_sign_in), onSignIn)
    }
}

@Composable
private fun NotificationEmptyStatePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { NotificationEmptyState() }
}

@Preview(widthDp = 360) @Composable private fun NotificationEmptyStateLightPreview() = NotificationEmptyStatePreview(false)
@Preview(widthDp = 360) @Composable private fun NotificationEmptyStateDarkPreview() = NotificationEmptyStatePreview(true)
