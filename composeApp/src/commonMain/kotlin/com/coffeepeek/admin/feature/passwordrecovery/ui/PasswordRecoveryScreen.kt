package com.coffeepeek.admin.feature.passwordrecovery.ui

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.ui.screen.auth.AuthPrimaryButton
import com.coffeepeek.admin.ui.screen.auth.AuthScreenScaffold
import com.coffeepeek.admin.ui.screen.auth.AuthTextField
import org.koin.core.parameter.parametersOf

@Composable
internal fun PasswordRecoveryScreen(
    initialEmail: String = "",
    initialToken: String? = null,
    onClose: () -> Unit,
    onLogin: () -> Unit,
) {
    val vm: PasswordRecoveryViewModel = platformViewModel(parameters = { parametersOf(initialEmail, initialToken.orEmpty()) })
    val state by vm.state.collectAsState()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    AuthScreenScaffold(showMascot = false, onClose = onClose) {
        Text(
            if (state.step == RecoveryStep.Complete) "Пароль изменён" else "Восстановление пароля",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        when (state.step) {
            RecoveryStep.Email -> {
                Text("Введите email аккаунта — мы отправим ссылку для восстановления пароля.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                AuthTextField(state.email, vm::onEmailChange, "name@example.com", label = "Email")
            }
            RecoveryStep.EmailSent -> {
                Text("Если аккаунт с этим email существует, на него отправлено письмо. Откройте ссылку из письма или вставьте её здесь. Проверьте также папку «Спам».", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                AuthPrimaryButton("У меня есть ссылка", vm::enterToken)
                TextButton(onClick = vm::requestAnotherEmail) { Text("Указать другой email") }
            }
            RecoveryStep.Password -> {
                if (!state.hasLinkToken) {
                    AuthTextField(state.token, vm::onTokenChange, "Вставьте ссылку из письма", label = "Ссылка из письма", keyboardType = KeyboardType.Uri, next = true)
                    Spacer(Modifier.height(14.dp))
                }
                AuthTextField(state.password, vm::onPasswordChange, "Не менее 8 символов", label = "Новый пароль", next = true, isPassword = true, newPassword = true)
                Spacer(Modifier.height(14.dp))
                AuthTextField(state.confirmation, vm::onConfirmationChange, "Повторите пароль", label = "Подтверждение пароля", isPassword = true, newPassword = true)
            }
            RecoveryStep.Complete -> {
                Text("Теперь можно войти с новым паролем.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                AuthPrimaryButton("Войти", onLogin)
            }
        }
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.step == RecoveryStep.Email || state.step == RecoveryStep.Password) {
            Spacer(Modifier.height(16.dp))
            AuthPrimaryButton(
                text = if (state.busy) "Подождите…" else if (state.step == RecoveryStep.Email) "Отправить письмо" else "Сохранить пароль",
                enabled = !state.busy,
                onClick = { focus.clearFocus(force = true); keyboard?.hide(); vm.submit() },
            )
            if (state.step == RecoveryStep.Password) {
                TextButton(onClick = vm::requestAnotherEmail, enabled = !state.busy) { Text("Запросить новую ссылку") }
            }
        }
    }
}
