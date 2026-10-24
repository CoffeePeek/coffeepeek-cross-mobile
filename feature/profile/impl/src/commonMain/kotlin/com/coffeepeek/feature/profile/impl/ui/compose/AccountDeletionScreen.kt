package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_back
import com.coffeepeek.feature.profile.impl.resources.profile_close
import com.coffeepeek.feature.profile.impl.resources.profile_deletion_confirm_no_email
import com.coffeepeek.feature.profile.impl.resources.profile_deletion_confirm_subtitle
import com.coffeepeek.feature.profile.impl.resources.profile_deletion_confirm_title
import com.coffeepeek.feature.profile.impl.resources.profile_deletion_resend
import com.coffeepeek.feature.profile.impl.resources.profile_deletion_resend_wait
import com.coffeepeek.feature.profile.impl.resources.profile_deletion_send
import com.coffeepeek.feature.profile.impl.resources.profile_deletion_sent_subtitle
import com.coffeepeek.feature.profile.impl.resources.profile_deletion_sent_title
import com.coffeepeek.feature.profile.impl.resources.profile_deletion_title
import com.coffeepeek.feature.profile.impl.resources.profile_deletion_wait_hint
import com.coffeepeek.feature.profile.impl.resources.profile_error_title
import com.coffeepeek.feature.profile.impl.resources.profile_loading
import com.coffeepeek.feature.profile.impl.resources.profile_ok
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.profile.impl.ui.AccountDeletionViewModel
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionAction
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionEvent

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.component.CoffeePeekScrollableColumn
import com.coffeepeek.core.designsystem.component.CoffeePeekErrorDialog
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AccountDeletionScreen(
    vm: AccountDeletionViewModel,
    onClose: () -> Unit,
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val sessionEnded by rememberUpdatedState(onSessionEnded)
    LaunchedEffect(vm) { vm.events.collect { event -> when (event) { AccountDeletionEvent.SessionEnded -> sessionEnded() } } }
    AccountDeletionScreenContent(state, vm::onAction, onClose, modifier)
}

@Composable
internal fun AccountDeletionScreenContent(state: AccountDeletionState, onAction: (AccountDeletionAction) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekErrorDialog(
        show = state.error != null, message = state.error.orEmpty(),
        title = stringResource(Res.string.profile_error_title), dismissLabel = stringResource(Res.string.profile_ok),
        onDismiss = { onAction(AccountDeletionAction.DismissError) },
    )
    CoffeePeekScaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(stringResource(Res.string.profile_deletion_title), stringResource(Res.string.profile_back), onBack = onClose) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CoffeePeekLoader(stringResource(Res.string.profile_loading)) }
        } else {
            AccountDeletionBody(state, { onAction(AccountDeletionAction.Send) }, onClose, Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable
private fun AccountDeletionBody(state: AccountDeletionState, onSend: () -> Unit, onClose: () -> Unit, modifier: Modifier) {
    CoffeePeekScrollableColumn(
        modifier.padding(horizontal = CoffeePeekDimens.spacing4), reserveBottomClearance = true,
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AccountDeletionMessage(state)
        Spacer(Modifier.height(28.dp))
        AccountDeletionButtons(state, onSend, onClose)
    }
}

@Composable
private fun AccountDeletionMessage(state: AccountDeletionState) {
    val email = state.email.ifBlank { stringResource(Res.string.profile_deletion_confirm_no_email) }
    Text(
        stringResource(if (state.requested) Res.string.profile_deletion_sent_title else Res.string.profile_deletion_confirm_title),
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.52).sp),
        color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(if (state.requested) Res.string.profile_deletion_sent_subtitle else Res.string.profile_deletion_confirm_subtitle, email),
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
    )
    if (state.requested) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(Res.string.profile_deletion_wait_hint), Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AccountDeletionButtons(state: AccountDeletionState, onSend: () -> Unit, onClose: () -> Unit) {
    CoffeePeekButton(
        text = stringResource(when {
            !state.requested -> Res.string.profile_deletion_send
            state.resendAvailable -> Res.string.profile_deletion_resend
            else -> Res.string.profile_deletion_resend_wait
        }),
        onClick = onSend, enabled = state.canSend, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.buttonHeight),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
    ) { Text(stringResource(Res.string.profile_close), style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun AccountDeletionPreview(dark: Boolean, requested: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    AccountDeletionScreenContent(AccountDeletionState(email = "fixture@example.invalid", isLoading = false, requested = requested), {}, {})
}

@Preview @Composable private fun AccountDeletionLightPreview() = AccountDeletionPreview(false, false)
@Preview @Composable private fun AccountDeletionDarkPreview() = AccountDeletionPreview(true, false)
@Preview @Composable private fun AccountDeletionSentLightPreview() = AccountDeletionPreview(false, true)
@Preview @Composable private fun AccountDeletionSentDarkPreview() = AccountDeletionPreview(true, true)
