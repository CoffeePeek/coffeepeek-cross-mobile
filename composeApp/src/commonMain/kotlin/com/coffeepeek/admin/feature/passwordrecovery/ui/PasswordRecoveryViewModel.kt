package com.coffeepeek.admin.feature.passwordrecovery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeepeek.admin.feature.passwordrecovery.domain.PasswordRecoveryRepository
import com.coffeepeek.admin.utils.validateEmailRequired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class RecoveryStep { Email, EmailSent }

internal data class PasswordRecoveryState(
    val step: RecoveryStep = RecoveryStep.Email,
    val email: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

internal class PasswordRecoveryViewModel(
    initialEmail: String,
    private val repository: PasswordRecoveryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PasswordRecoveryState(
        email = initialEmail,
    ))
    val state = _state.asStateFlow()

    fun onEmailChange(value: String) = edit { copy(email = value) }
    fun requestAnotherEmail() = edit { copy(step = RecoveryStep.Email) }

    private fun edit(update: PasswordRecoveryState.() -> PasswordRecoveryState) {
        if (!_state.value.busy) _state.update { it.update().copy(error = null) }
    }

    fun submit() {
        val current = state.value
        if (current.busy || current.step != RecoveryStep.Email) return
        val error = validateEmailRequired(current.email)
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = repository.requestReset(current.email)
            result.onSuccess {
                _state.update { it.copy(
                    busy = false,
                    step = RecoveryStep.EmailSent,
                ) }
            }.onFailure {
                _state.update { it.copy(
                    busy = false,
                    error = "Не удалось отправить письмо. Проверьте соединение и попробуйте снова.",
                ) }
            }
        }
    }
}
