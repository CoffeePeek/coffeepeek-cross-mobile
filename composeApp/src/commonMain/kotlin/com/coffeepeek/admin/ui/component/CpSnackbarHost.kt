package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
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
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons

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
        val colors = MaterialTheme.colorScheme
        val accent = if (colors.background.luminance() < .5f) CpColor.AccentTextDark else CpColor.AccentTextLight
        Surface(
            modifier = Modifier
                .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing2)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            shape = RoundedCornerShape(CpDimens.toastRadius),
            color = colors.surface,
            contentColor = colors.onSurface,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Column(Modifier.padding(CpDimens.toastPadding), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                    Icon(CpIcons.Info, contentDescription = null, tint = accent,
                        modifier = Modifier.padding(top = CpDimens.spacing3).size(24.dp))
                    Text(message.visuals.message, style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface, modifier = Modifier.weight(1f).padding(top = CpDimens.spacing3))
                    IconButton(onClick = message::dismiss) {
                        Icon(CpIcons.Close, contentDescription = "Скрыть сообщение",
                            tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
                message.visuals.actionLabel?.let { label ->
                    Button(
                        onClick = onAction ?: message::performAction,
                        enabled = !actionInProgress,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(CpDimens.toastRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary, contentColor = colors.onPrimary,
                            disabledContainerColor = colors.primary.copy(alpha = .7f),
                            disabledContentColor = colors.onPrimary,
                        ),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                            if (actionInProgress) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                            } else if (actionIcon != null) {
                                Icon(actionIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                            Text(if (actionInProgress) "Повторяем…" else label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
