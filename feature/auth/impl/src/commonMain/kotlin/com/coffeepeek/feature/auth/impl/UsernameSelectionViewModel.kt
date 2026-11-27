package com.coffeepeek.feature.auth.impl

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.auth.api.*
import com.coffeepeek.feature.auth.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/** One application-owned instance; results are scoped to the opaque login lifetime. */
internal class UsernameSelectionViewModel(
    private val account: UsernameSelectionAccount,
    private val repository: UsernameSelectionRepository,
    private val signOut: suspend () -> Result<Unit>,
) : MviViewModel<UsernameSelectionState, UsernameSelectionAction, Nothing>(UsernameSelectionState()), UsernameSelectionGate {
    private var observation: Job? = null
    init { observeAccount() }

    private fun observeAccount() {
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                account.observe().distinctUntilChanged().collectLatest { owner ->
                    updateState { UsernameSelectionState(if (owner == null) UsernameSelectionPhase.SignedOut else UsernameSelectionPhase.Checking,
                        owner?.id, owner?.generation ?: 0, username = username.takeIf { accountId == owner?.id && accountGeneration == owner?.generation }.orEmpty()) }
                    if (owner != null) check(owner)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                updateState { copy(phase = UsernameSelectionPhase.Failed, error = UsernameSelectionError.Unavailable) }
            }
        }
    }

    override suspend fun handleActionInternal(action: UsernameSelectionAction) {
        when (action) {
            is UsernameSelectionAction.Edit -> if (currentState.phase == UsernameSelectionPhase.Required)
                updateState { copy(username = action.value, error = null) }
            UsernameSelectionAction.Submit -> submit()
            UsernameSelectionAction.Retry -> if (currentState.phase == UsernameSelectionPhase.Failed)
                observeAccount()
            UsernameSelectionAction.SignOut -> leave()
        }
    }

    private suspend fun check(owner: UsernameSelectionOwner) {
        updateState { copy(phase = UsernameSelectionPhase.Checking, error = null) }
        apply(owner, repository.load(owner))
    }

    private suspend fun submit() {
        if (currentState.phase != UsernameSelectionPhase.Required) return
        val owner = account.current()?.takeIf { matches(it) } ?: return
        val name = currentState.username.trim()
        if (!isValidSelectedUsername(name)) {
            updateState { copy(error = UsernameSelectionError.Invalid) }
            return
        }
        updateState { copy(phase = UsernameSelectionPhase.Submitting, error = null) }
        applyCompletion(owner, repository.complete(owner, name))
    }

    private suspend fun applyCompletion(owner: UsernameSelectionOwner, result: Result<UsernameSelectionProfile>) {
        if ((result.exceptionOrNull() as? UsernameSelectionException)?.reason == UsernameSelectionFailure.Conflict) {
            val refreshed = repository.load(owner)
            if (refreshed.getOrNull()?.required == false) { apply(owner, refreshed); return }
        }
        apply(owner, result, editing = true)
    }

    private fun apply(owner: UsernameSelectionOwner, result: Result<UsernameSelectionProfile>, editing: Boolean = false) {
        if (!matches(owner)) return
        result.fold(onSuccess = { profile ->
            updateState { copy(phase = if (profile.required) UsernameSelectionPhase.Required else UsernameSelectionPhase.Ready,
                username = if (profile.required && username.isNotBlank()) username else profile.username, error = null) }
        }, onFailure = { failure ->
            if (failure is CancellationException) throw failure
            val reason = (failure as? UsernameSelectionException)?.reason
            val error = reason?.let { UsernameSelectionError.valueOf(it.name) } ?: UsernameSelectionError.Unavailable
            val fieldError = editing && error in listOf(UsernameSelectionError.Invalid, UsernameSelectionError.Taken)
            updateState { copy(phase = if (fieldError) UsernameSelectionPhase.Required else UsernameSelectionPhase.Failed, error = error) }
        })
    }

    private fun matches(owner: UsernameSelectionOwner) = account.current() == owner &&
        currentState.accountId == owner.id && currentState.accountGeneration == owner.generation

    private suspend fun leave() {
        val owner = account.current() ?: return
        val result = signOut()
        if (matches(owner) && result.isFailure) updateState { copy(error = UsernameSelectionError.Unavailable) }
    }
}

/** Composition owns the application lifetime; consumers see only the feature API. */
fun createUsernameSelectionGate(account: UsernameSelectionAccount, repository: UsernameSelectionRepository,
    signOut: suspend () -> Result<Unit>,
): UsernameSelectionGate = UsernameSelectionViewModel(account, repository, signOut)
