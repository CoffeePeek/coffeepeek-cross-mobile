package com.coffeepeek.admin.ui.screen.deleteaccount

import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.component.PlatformAlert
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.delete_account_pending_resend
import coffeepeek.composeapp.generated.resources.delete_account_pending_resend_wait
import coffeepeek.composeapp.generated.resources.delete_account_pending_subtitle
import coffeepeek.composeapp.generated.resources.delete_account_pending_title
import coffeepeek.composeapp.generated.resources.delete_account_pending_wait_hint
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.screen.auth.AuthPrimaryButton
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountPendingScreen(
    vm: DeleteAccountPendingViewModel = platformViewModel(),
) {
    val state by vm.uiState.collectAsState()

    state.error?.let { err ->
        PlatformAlert(title = "Ошибка", message = err, onConfirm = vm::clearError, onDismiss = vm::clearError)
    }

    Scaffold(
        topBar = { CpTopBar("Удаление аккаунта") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CoffeePeekLoader()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = CpDimens.spacing4),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.delete_account_pending_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.52).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(
                    Res.string.delete_account_pending_subtitle,
                    state.email.ifBlank { "вашу почту" },
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.delete_account_pending_wait_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
            AuthPrimaryButton(
                text = if (state.resendAvailable) {
                    stringResource(Res.string.delete_account_pending_resend)
                } else {
                    stringResource(Res.string.delete_account_pending_resend_wait)
                },
                onClick = vm::resendEmail,
                enabled = state.resendAvailable && !state.isResending,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = vm::dismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CpDimens.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                shape = RoundedCornerShape(CpDimens.buttonRadius),
            ) {
                Text(
                    text = "Закрыть",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
