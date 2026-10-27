package com.coffeepeek.admin.ui.screen.deleteaccount

import com.coffeepeek.core.datetime.parseServerInstant
import com.coffeepeek.admin.auth.GoogleAuth
import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.domain.model.AccountDeletionRequest
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class DeleteAccountPendingUiState(
    val email: String = "",
    val isLoading: Boolean = true,
    val isResending: Boolean = false,
    val resendAvailable: Boolean = true,
    val resendAvailableAtUtc: String? = null,
    val error: String? = null,
)

class DeleteAccountPendingViewModel(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountPendingUiState())
    val uiState: StateFlow<DeleteAccountPendingUiState> = _uiState.asStateFlow()

    init {
        observeSessionEnd()
        load()
        tickResendAvailability()
    }

    fun resendEmail() {
        if (_uiState.value.isResending || !_uiState.value.resendAvailable) return
        workScope.launch {
            _uiState.update { it.copy(isResending = true, error = null) }
            userRepository.requestAccountDeletion()
                .onSuccess { request ->
                    applyRequest(request)
                    _uiState.update { it.copy(isResending = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            error = err.message ?: "Не удалось отправить письмо",
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismiss() {
        Navigator.popBack()
    }

    private fun load() {
        workScope.launch {
            val email = userRepository.observeProfile().value?.email
                ?: userRepository.getMe().getOrNull()?.email
                ?: ""
            _uiState.update { it.copy(email = email, isLoading = true, error = null) }

            val existing = userRepository.getAccountDeletionRequest().getOrElse { err ->
                _uiState.update {
                    it.copy(isLoading = false, error = err.message ?: "Ошибка загрузки статуса")
                }
                return@launch
            }

            if (existing != null) {
                applyRequest(existing)
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            userRepository.requestAccountDeletion()
                .onSuccess { request ->
                    applyRequest(request)
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Не удалось отправить письмо",
                        )
                    }
                }
        }
    }

    private fun observeSessionEnd() {
        workScope.launch {
            var sawActiveSession = false
            sessionRepository.observeSession()
                .map { session -> sessionRepository.isActiveSession(session) }
                .distinctUntilChanged()
                .collect { isActive ->
                    if (isActive) {
                        sawActiveSession = true
                    } else if (sawActiveSession) {
                        GoogleAuth.signOut()
                        Navigator.openLoginAfterSessionEnd()
                    }
                }
        }
    }

    private fun tickResendAvailability() {
        workScope.launch {
            while (isActive) {
                refreshResendAvailability()
                delay(1_000)
            }
        }
    }

    private fun applyRequest(request: AccountDeletionRequest) {
        _uiState.update {
            it.copy(
                resendAvailableAtUtc = request.resendAvailableAtUtc,
                resendAvailable = isResendAvailable(request.resendAvailableAtUtc),
                error = null,
            )
        }
    }

    private fun refreshResendAvailability() {
        val at = _uiState.value.resendAvailableAtUtc ?: return
        val available = isResendAvailable(at)
        if (available != _uiState.value.resendAvailable) {
            _uiState.update { it.copy(resendAvailable = available) }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun isResendAvailable(resendAvailableAtUtc: String): Boolean {
        if (resendAvailableAtUtc.isBlank()) return true
        val availableAt = parseServerInstant(resendAvailableAtUtc) ?: return true
        return Clock.System.now() >= availableAt
    }
}
