package com.coffeepeek.admin.ui.component

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coffeepeek.admin.theme.CpDimens

/** CoffeePeek surface and typography, with a separate cancel group for intentional actions. */
@Composable
internal fun CpActionSheet(
    title: String,
    message: String,
    actions: List<PlatformAction>,
    onDismiss: () -> Unit,
    cancelLabel: String = stringResource(Res.string.history_home_delete_cancel),
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CpDimens.cardRadius)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(CpDimens.spacing4), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                Column(Modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape)) {
                    Column(Modifier.padding(CpDimens.cardPadding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                        BasicText(title, style = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().semantics { heading() })
                        BasicText(message, style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceVariant, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
                    }
                    actions.forEach { action ->
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
                        CpTextAction(action.title, action.onClick, Modifier.fillMaxWidth().heightIn(min = 54.dp), action.enabled, action.destructive)
                    }
                }
                CpTextAction(cancelLabel, onDismiss, Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape), emphasized = true)
            }
        }
    }
}

/** Plain project action with a 44dp target, pressed opacity and no Material ripple or button chrome. */
@Composable
internal fun CpTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    emphasized: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    Box(modifier.heightIn(min = CpDimens.controlHeight)
        .graphicsLayer { alpha = if (!enabled) .4f else if (pressed) .55f else 1f }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing2), contentAlignment = Alignment.Center) {
        BasicText(text, style = MaterialTheme.typography.bodyLarge.copy(
            color = if (destructive) colors.error else colors.onSurface,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        ))
    }
}

/** Nonmodal operation feedback. It never presents a disabled alert as a progress indicator. */
@Composable
internal fun CpOperationStatus(message: String) {
    if (NativeOperationStatus(message)) return
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CpDimens.radius2xl)
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = CpDimens.spacing4, vertical = CpDimens.floatingNavContentClearance + CpDimens.spacing4), contentAlignment = Alignment.BottomCenter) {
        Row(Modifier.widthIn(max = 440.dp).clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape)
            .padding(horizontal = CpDimens.spacing5, vertical = CpDimens.spacing4)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite; progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
            CoffeePeekLoader(size = CpDimens.loaderButton, contentDescription = "")
            BasicText(message, style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface, fontWeight = FontWeight.Medium))
        }
    }
}
