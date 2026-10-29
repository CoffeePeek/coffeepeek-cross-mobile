package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.resources.*

@Composable
internal fun UsernameChangeConfirmation(username: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.widthIn(max = 440.dp).padding(CoffeePeekDimens.spacing6)) {
            UsernameChangeConfirmationContent(username, onConfirm, onDismiss)
        }
    }
}

@Composable
private fun UsernameChangeConfirmationContent(username: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CoffeePeekContentCard(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            stringResource(Res.string.profile_edit_confirm_username_title),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(CoffeePeekDimens.spacing3))
        Text(
            stringResource(Res.string.profile_edit_confirm_username_message, username),
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(CoffeePeekDimens.spacing6))
        CoffeePeekButton(stringResource(Res.string.profile_edit_confirm_username_action), onConfirm)
        TextButton(onDismiss, Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.profile_cancel),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun UsernameChangeConfirmationPreview(dark: Boolean) {
    CoffeePeekTheme(darkTheme = dark) { UsernameChangeConfirmationContent("coffee_lover", {}, {}) }
}

@Preview @Composable private fun UsernameChangeConfirmationLightPreview() = UsernameChangeConfirmationPreview(false)
@Preview @Composable private fun UsernameChangeConfirmationDarkPreview() = UsernameChangeConfirmationPreview(true)
