package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

interface CoffeePeekNativeActionSheetPresenter {
    @Composable
    fun Present(title: String, message: String?, actions: List<CoffeePeekDialogAction>,
        dismissBeforeAction: Boolean, onDismiss: () -> Unit): Boolean
}

val LocalCoffeePeekNativeActionSheetPresenter = staticCompositionLocalOf<CoffeePeekNativeActionSheetPresenter?> { null }

/** Intentional actions keep their own cancel group and the established pressed-opacity treatment. */
@Composable
fun CoffeePeekActionSheet(title: String, message: String, actions: List<CoffeePeekDialogAction>,
    onDismiss: () -> Unit, cancelLabel: String, dismissBeforeAction: Boolean = false) {
    if (LocalCoffeePeekNativeActionSheetPresenter.current?.Present(title, message, actions, dismissBeforeAction, onDismiss) == true) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(CoffeePeekDimens.spacing4),
            contentAlignment = Alignment.BottomCenter) {
            ActionSheetContent(title, message, actions, onDismiss, cancelLabel)
        }
    }
}

@Composable
private fun ActionSheetContent(title: String, message: String, actions: List<CoffeePeekDialogAction>,
    onDismiss: () -> Unit, cancelLabel: String) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CoffeePeekDimens.cardRadius)
    Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        CoffeePeekCard(shape = shape, border = BorderStroke(1.dp, colors.outlineVariant), elevation = 0.dp,
            contentPadding = PaddingValues(0.dp)) {
            Column(Modifier.padding(CoffeePeekDimens.cardPadding), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                BasicText(title, style = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().semantics { heading() })
                BasicText(message, style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
            }
            actions.forEach { action ->
                androidx.compose.material3.HorizontalDivider(color = colors.outlineVariant)
                CoffeePeekTextAction(action.label, action.onClick, Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    action.enabled, action.destructive)
            }
        }
        CoffeePeekCard(shape = shape, border = BorderStroke(1.dp, colors.outlineVariant), elevation = 0.dp,
            contentPadding = PaddingValues(0.dp)) {
            CoffeePeekTextAction(cancelLabel, onDismiss, Modifier.fillMaxWidth().heightIn(min = 54.dp), emphasized = true)
        }
    }
}

@Composable private fun ActionSheetPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ActionSheetContent("Подтверждение", "Это действие изменит видимость публикаций.",
        listOf(CoffeePeekDialogAction("Подтвердить", {}, destructive = true)), {}, "Отмена")
}
@Preview @Composable private fun CoffeePeekActionSheetLightPreview() = ActionSheetPreview(false)
@Preview @Composable private fun CoffeePeekActionSheetDarkPreview() = ActionSheetPreview(true)
