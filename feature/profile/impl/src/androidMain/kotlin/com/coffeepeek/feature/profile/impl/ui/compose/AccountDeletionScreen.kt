package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.profile.impl.R
import com.coffeepeek.feature.profile.impl.ui.AccountDeletionViewModel
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionAction
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionEvent

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

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.onAction(AccountDeletionAction.DismissError) },
            title = { Text(stringResource(R.string.profile_error_title)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { vm.onAction(AccountDeletionAction.DismissError) }) { Text(stringResource(R.string.profile_ok)) } },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(stringResource(R.string.profile_deletion_title), stringResource(R.string.profile_back), onBack = onClose) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CoffeePeekLoader(stringResource(R.string.profile_loading)) }
            return@Scaffold
        }
        val email = state.email.ifBlank { stringResource(R.string.profile_deletion_confirm_no_email) }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = CoffeePeekDimens.spacing4),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(if (state.requested) R.string.profile_deletion_sent_title else R.string.profile_deletion_confirm_title),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.52).sp),
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(if (state.requested) R.string.profile_deletion_sent_subtitle else R.string.profile_deletion_confirm_subtitle, email),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            if (state.requested) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.profile_deletion_wait_hint), Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(28.dp))
            CoffeePeekButton(
                text = stringResource(when {
                    !state.requested -> R.string.profile_deletion_send
                    state.resendAvailable -> R.string.profile_deletion_resend
                    else -> R.string.profile_deletion_resend_wait
                }),
                onClick = { vm.onAction(AccountDeletionAction.Send) },
                enabled = state.canSend,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.buttonHeight),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
            ) { Text(stringResource(R.string.profile_close), style = MaterialTheme.typography.labelLarge) }
        }
    }
}
