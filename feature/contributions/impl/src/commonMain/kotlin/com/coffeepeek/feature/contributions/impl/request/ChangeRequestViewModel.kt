package com.coffeepeek.feature.contributions.impl.request

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.domain.model.ContributionFailure
import com.coffeepeek.feature.contributions.domain.model.FailureKind
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** One sent change request. A change of account drops the request of the previous one and its late answer. */
internal class ChangeRequestViewModel(
    private val requestId: String,
    private val repository: ContributionsRepository,
    private val account: ContributionsAccount,
) : MviViewModel<ChangeRequestState, ChangeRequestAction, ChangeRequestEvent>(ChangeRequestState()) {
    private var key: String? = null
    private var generation = 0L
    private var job: Job? = null

    init {
        viewModelScope.launch {
            account.accountKey.distinctUntilChanged().collect { accountKey ->
                key = accountKey
                if (accountKey == null) {
                    generation++; job?.cancel()
                    updateState { ChangeRequestState(isLoading = false, error = RequestError.SignInRequired) }
                } else load() // load() starts from an empty state, so another account's request never stays visible
            }
        }
    }

    override suspend fun handleActionInternal(action: ChangeRequestAction) {
        when (action) {
            ChangeRequestAction.Refresh -> if (key != null) load()
        }
    }

    private fun load() {
        generation++; job?.cancel()
        val mine = generation
        val owner = key
        updateState { ChangeRequestState(isLoading = true) }
        job = viewModelScope.launch {
            val result = repository.change(requestId)
            if (mine != generation || owner != account.currentAccountKey()) return@launch
            result.onSuccess { request -> updateState { ChangeRequestState(request = request, isLoading = false) } }
                .onFailure { failure ->
                    val known = failure as? ContributionFailure
                    updateState {
                        ChangeRequestState(
                            isLoading = false,
                            error = when (known?.kind) {
                                FailureKind.SignInRequired -> RequestError.SignInRequired
                                FailureKind.NotFound -> RequestError.NotFound
                                else -> RequestError.LoadFailed
                            },
                            serverMessage = known?.serverMessage,
                        )
                    }
                }
        }
    }
}
