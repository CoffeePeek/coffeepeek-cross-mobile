package com.coffeepeek.admin.feature.notifications.ui

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
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun NotificationEmptyState(signedOut: Boolean = false, onSignIn: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = CoffeePeekDimens.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        CoffeePeekIconBadge(if (signedOut) CoffeePeekIcons.Notifications else CoffeePeekIcons.Coffee, CoffeePeekIconBadgePalette.Gold)
        Text(stringResource(if (signedOut) Res.string.notifications_sign_in_title else Res.string.notifications_empty_title),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(if (signedOut) Res.string.notifications_sign_in_description else Res.string.notifications_empty_description),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        if (signedOut) CoffeePeekButton(stringResource(Res.string.login), onSignIn)
    }
}

@Composable
private fun NotificationEmptyStatePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { NotificationEmptyState() }
}

@Preview(widthDp = 360) @Composable private fun NotificationEmptyStateLightPreview() = NotificationEmptyStatePreview(false)
@Preview(widthDp = 360) @Composable private fun NotificationEmptyStateDarkPreview() = NotificationEmptyStatePreview(true)
