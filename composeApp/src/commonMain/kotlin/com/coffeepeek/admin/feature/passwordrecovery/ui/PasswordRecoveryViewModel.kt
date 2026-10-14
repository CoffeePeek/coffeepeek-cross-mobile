package com.coffeepeek.admin.feature.passwordrecovery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeepeek.admin.feature.passwordrecovery.domain.PasswordRecoveryRepository
import com.coffeepeek.admin.utils.MIN_REGISTRATION_PASSWORD_LENGTH
import com.coffeepeek.admin.utils.validateEmailRequired
import com.coffeepeek.admin.utils.validatePasswordRequired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class RecoveryStep { Email, EmailSent, Password, Complete }

internal data class PasswordRecoveryState(
    val step: RecoveryStep = RecoveryStep.Email,
    val email: String = "",
    val token: String = "",
    val hasLinkToken: Boolean = false,
    val password: String = "",
    val confirmation: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

internal class PasswordRecoveryViewModel(
    initialEmail: String,
    initialToken: String?,
    private val repository: PasswordRecoveryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PasswordRecoveryState(
        email = initialEmail,
        token = initialToken.orEmpty(),
        hasLinkToken = !initialToken.isNullOrBlank(),
        step = if (initialToken == null) RecoveryStep.Email else RecoveryStep.Password,
    ))
    val state = _state.asStateFlow()

    fun onEmailChange(value: String) = edit { copy(email = value) }
    fun onTokenChange(value: String) = edit { copy(token = value) }
    fun onPasswordChange(value: String) = edit { copy(password = value) }
    fun onConfirmationChange(value: String) = edit { copy(confirmation = value) }
    fun enterToken() = edit { copy(step = RecoveryStep.Password) }
    fun requestAnotherEmail() = edit { copy(step = RecoveryStep.Email, token = "", hasLinkToken = false, password = "", confirmation = "") }

    private fun edit(update: PasswordRecoveryState.() -> PasswordRecoveryState) {
        if (!_state.value.busy) _state.update { it.update().copy(error = null) }
    }

    fun submit() {
        val current = state.value
        if (current.busy || current.step !in setOf(RecoveryStep.Email, RecoveryStep.Password)) return
        val emailStep = current.step == RecoveryStep.Email
        val error = if (emailStep) validateEmailRequired(current.email) else when {
            current.token.isBlank() -> "Вставьте ссылку из письма"
            else -> validatePasswordRequired(current.password, MIN_REGISTRATION_PASSWORD_LENGTH)
                ?: if (current.password != current.confirmation) "Пароли не совпадают" else null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = if (emailStep) repository.requestReset(current.email)
                else repository.resetPassword(current.token, current.password)
            result.onSuccess {
                _state.update { it.copy(
                    busy = false,
                    step = if (emailStep) RecoveryStep.EmailSent else RecoveryStep.Complete,
                    token = "", password = "", confirmation = "",
                ) }
            }.onFailure {
                _state.update { it.copy(
                    busy = false,
                    error = if (emailStep) "Не удалось отправить письмо. Проверьте соединение и попробуйте снова."
                        else "Не удалось изменить пароль. Проверьте соединение и ссылку. Если она устарела, запросите новое письмо.",
                ) }
            }
        }
    }
}
