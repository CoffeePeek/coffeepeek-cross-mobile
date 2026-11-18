package com.coffeepeek.feature.profile.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.profile.api.AccountDeletionPort
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.impl.ui.AccountDeletionViewModel
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionAction
import com.coffeepeek.feature.profile.impl.ui.model.AccountDeletionError
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Native rendering of the same explicit-confirmation and resend state machine. */
class NativeAccountDeletionSession internal constructor(
    private val model: AccountDeletionViewModel,
    private val accounts: ProfileAccountPort,
    private val closeModel: () -> Unit,
) {
    private val owner = accounts.activeAccount()
    private val generation = accounts.cacheScope()
    private var revoked = false
    val state = combine(model.state, accounts.observeActiveAccount(), accounts.observeCacheScope()) { _, _, _ -> current() }
    val ended = model.events.map { Unit }

    fun isCurrent(): Boolean {
        if (owner == null || accounts.activeAccount() != owner || accounts.cacheScope() != generation) revoked = true
        return !revoked
    }

    fun current(): NativeAccountDeletionState {
        if (!isCurrent()) return NativeAccountDeletionState(revoked = true)
        val state = model.state.value
        return NativeAccountDeletionState(state.email, state.isLoading, state.isSending, state.requested,
            state.canSend, state.resendAvailable, false, state.error.errorKey(), (state.error as? AccountDeletionError.Server)?.message)
    }

    fun send() { if (isCurrent()) model.onAction(AccountDeletionAction.Send) }
    fun dismissError() { if (isCurrent()) model.onAction(AccountDeletionAction.DismissError) }
    fun close() {
        revoked = true
        closeModel()
    }
}

data class NativeAccountDeletionState(
    val email: String = "", val loading: Boolean = false, val sending: Boolean = false,
    val requested: Boolean = false, val canSend: Boolean = false, val resendAvailable: Boolean = false,
    val revoked: Boolean = false, val errorKey: String? = null, val serverError: String? = null,
)

fun createNativeAccountDeletionSession(accounts: ProfileAccountPort, deletion: AccountDeletionPort): NativeAccountDeletionSession {
    val model = AccountDeletionViewModel(accounts, deletion)
    val store = ViewModelStore().apply { put("account-deletion", model) }
    return NativeAccountDeletionSession(model, accounts, store::clear)
}

private fun AccountDeletionError?.errorKey() = when (this) {
    AccountDeletionError.Status -> "profile_deletion_status_failed"
    AccountDeletionError.Send -> "profile_deletion_send_failed"
    else -> null
}
