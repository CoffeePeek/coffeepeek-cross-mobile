package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.shop_phone_copied
import coffeepeek.composeapp.generated.resources.shop_session_retry
import coffeepeek.composeapp.generated.resources.shop_session_unavailable
import coffeepeek.composeapp.generated.resources.snackbar_dismiss
import coffeepeek.composeapp.generated.resources.snackbar_retrying
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekPlainIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Uses the existing message queue and accessible timeout, with CoffeePeek presentation. */
@Composable
internal fun CpSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    actionInProgress: Boolean = false,
    onAction: (() -> Unit)? = null,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { message ->
        SnackbarMessageContent(message.visuals.message, message::dismiss, message.visuals.actionLabel,
            actionIcon, actionInProgress, onAction ?: message::performAction)
    }
}

@Composable
private fun SnackbarMessageContent(
    message: String, onDismiss: () -> Unit, actionLabel: String? = null,
    actionIcon: ImageVector? = null, actionInProgress: Boolean = false, onAction: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2)
            .widthIn(max = 560.dp).fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(CoffeePeekDimens.toastRadius),
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(start = CoffeePeekDimens.spacing4, end = CoffeePeekDimens.spacing1,
            bottom = if (actionLabel == null) 0.dp else CoffeePeekDimens.spacing3)) {
            SnackbarMessageRow(message, onDismiss)
            if (actionLabel != null) SnackbarMessageAction(actionLabel, onAction, actionIcon, actionInProgress,
                Modifier.padding(end = CoffeePeekDimens.spacing3))
        }
    }
}

@Composable
private fun SnackbarMessageRow(message: String, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val accent = if (colors.background.luminance() < .5f) CoffeePeekColors.AccentTextDark else CoffeePeekColors.AccentTextLight
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        CoffeePeekIcon(CoffeePeekIcons.Info, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface, modifier = Modifier.weight(1f).padding(vertical = CoffeePeekDimens.spacing2))
        CoffeePeekPlainIconButton(onClick = onDismiss) {
            CoffeePeekIcon(CoffeePeekIcons.Close, contentDescription = stringResource(Res.string.snackbar_dismiss),
                tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SnackbarMessageAction(
    label: String, onClick: () -> Unit, icon: ImageVector?, inProgress: Boolean, modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    CoffeePeekFilledButton(
        onClick = onClick,
        enabled = !inProgress,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(CoffeePeekDimens.toastRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary, contentColor = colors.onPrimary,
            disabledContainerColor = colors.primary.copy(alpha = .7f), disabledContentColor = colors.onPrimary,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            if (inProgress) CoffeePeekLoader(modifier = Modifier.size(20.dp), size = 20.dp, color = colors.onPrimary, strokeWidth = 2.dp)
            else if (icon != null) CoffeePeekIcon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(if (inProgress) stringResource(Res.string.snackbar_retrying) else label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SnackbarMessagePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.background(MaterialTheme.colorScheme.background).padding(vertical = CoffeePeekDimens.spacing2)) {
        SnackbarMessageContent(stringResource(Res.string.shop_phone_copied), {})
        SnackbarMessageContent(stringResource(Res.string.shop_session_unavailable), {},
            actionLabel = stringResource(Res.string.shop_session_retry))
        SnackbarMessageContent(stringResource(Res.string.shop_session_unavailable), {},
            actionLabel = stringResource(Res.string.shop_session_retry), actionInProgress = true)
    }
}

@Preview @Composable private fun SnackbarMessageLightPreview() = SnackbarMessagePreview(false)
@Preview @Composable private fun SnackbarMessageDarkPreview() = SnackbarMessagePreview(true)
