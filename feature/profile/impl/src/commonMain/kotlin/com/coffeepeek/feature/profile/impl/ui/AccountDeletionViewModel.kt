package com.coffeepeek.feature.profile.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.profile.api.AccountDeletionPort
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileDeletionRequest
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionAction
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionEvent
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Account deletion by e-mail confirmation. Opening only reads the status: an existing request is shown, otherwise the user confirms
 * explicitly before the e-mail is sent (production sent it on open). Resend is gated by the server's time, and the end of a session that
 * was active reports [AccountDeletionEvent.SessionEnded] once.
 */
@OptIn(ExperimentalTime::class)
internal class AccountDeletionViewModel(
    private val accounts: ProfileAccountPort,
    private val deletion: AccountDeletionPort,
    private val now: () -> Instant = { Clock.System.now() },
    private val tickMillis: Long = 1_000,
) : MviViewModel<AccountDeletionState, AccountDeletionAction, AccountDeletionEvent>(AccountDeletionState()) {
    private var ticker: Job? = null

    init {
        observeSessionEnd()
        load()
    }

    override suspend fun handleActionInternal(action: AccountDeletionAction) {
        when (action) {
            AccountDeletionAction.Send -> send()
            AccountDeletionAction.DismissError -> updateState { copy(error = null) }
        }
    }

    private suspend fun send() {
        if (!currentState.canSend) return
        updateState { copy(isSending = true, error = null) }
        deletion.request()
            .onSuccess { applyRequest(it); updateState { copy(isSending = false) } }
            .onFailure { updateState { copy(isSending = false, error = it.message?.takeIf(String::isNotBlank) ?: SEND_FAILED) } }
    }

    private fun load() {
        viewModelScope.launch {
            val email = accounts.cachedOwnProfile()?.email ?: accounts.loadOwnProfile(forceRefresh = false).getOrNull()?.email.orEmpty()
            updateState { copy(email = email, isLoading = true, error = null) }
            val existing = deletion.existingRequest().getOrElse { error ->
                updateState { copy(isLoading = false, error = error.message?.takeIf(String::isNotBlank) ?: STATUS_FAILED) }
                return@launch
            }
            if (existing != null) applyRequest(existing)
            updateState { copy(isLoading = false) }
        }
    }

    private fun observeSessionEnd() {
        viewModelScope.launch {
            var sawActiveSession = false
            accounts.observeActiveAccount().collect { account ->
                if (account != null) sawActiveSession = true
                else if (sawActiveSession) { sawActiveSession = false; sendEvent(AccountDeletionEvent.SessionEnded) }
            }
        }
    }

    private fun applyRequest(request: ProfileDeletionRequest) {
        val available = isResendAvailable(request.resendAvailableAtUtc)
        updateState { copy(requested = true, resendAvailableAtUtc = request.resendAvailableAtUtc, resendAvailable = available, error = null) }
        ticker?.cancel()
        ticker = if (available) null else viewModelScope.launch {
            while (!isResendAvailable(request.resendAvailableAtUtc)) delay(tickMillis)
            updateState { copy(resendAvailable = true) }
        }
    }

    private fun isResendAvailable(resendAvailableAtUtc: String): Boolean {
        val availableAt = parseInstant(resendAvailableAtUtc) ?: return true
        return now() >= availableAt
    }

    /** The server may omit the offset; such a value is UTC. */
    private fun parseInstant(value: String): Instant? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        runCatching { Instant.parse(trimmed) }.getOrNull()?.let { return it }
        val hasOffset = trimmed.endsWith("Z", ignoreCase = true) || trimmed.contains('+') || trimmed.indexOf('-', startIndex = 10) >= 0
        return if (hasOffset) null else runCatching { Instant.parse("${trimmed}Z") }.getOrNull()
    }

    private companion object {
        const val STATUS_FAILED = "Ошибка загрузки статуса"
        const val SEND_FAILED = "Не удалось отправить письмо"
    }
}
