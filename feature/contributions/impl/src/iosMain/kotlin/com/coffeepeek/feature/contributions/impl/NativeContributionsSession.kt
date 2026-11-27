package com.coffeepeek.feature.contributions.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.impl.list.ContributionsListAction
import com.coffeepeek.feature.contributions.impl.list.ContributionsListViewModel
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestAction
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NativeContributionsListSession(
    kind: ContributionKind, repository: ContributionsRepository, account: ContributionsAccount,
) {
    private val session = AccountContributionDestination(account, { loading, error -> NativeContributionsListState(loading, error) }) {
        ContributionsListViewModel(kind, repository, it)
    }

    fun observe(receive: (NativeContributionsListState) -> Unit): Job =
        session.observe({ it.state.map { state -> state.toNative() } }, receive)

    fun refresh() { session.activeModel()?.onAction(ContributionsListAction.Refresh) }

    fun select(status: String) {
        val model = session.activeModel() ?: return
        val selected = model.state.value.visibleTabs.firstOrNull { it.key() == status } ?: return
        model.onAction(ContributionsListAction.Select(selected))
    }

    fun loadMore(status: String) {
        val model = session.activeModel() ?: return
        val selected = model.state.value.selectedTab?.takeIf { it.key() == status } ?: return
        model.onAction(ContributionsListAction.LoadMore(selected))
    }

    fun destination(id: String): Pair<Boolean, String>? {
        val state = session.activeModel()?.state?.value ?: return null
        if (state.isLoading || state.error != null) return null
        return state.tabs[state.selectedTab]?.items?.firstOrNull { it.id == id }?.target?.destination()
    }

    fun close() = session.close()
}

class NativeChangeRequestSession(requestId: String, repository: ContributionsRepository, account: ContributionsAccount) {
    private val session = AccountContributionDestination(account, { loading, error -> NativeChangeRequestState(loading, error) }) {
        ChangeRequestViewModel(requestId, repository, it)
    }

    fun observe(receive: (NativeChangeRequestState) -> Unit): Job =
        session.observe({ it.state.map { state -> state.toNative() } }, receive)

    fun refresh() { session.activeModel()?.onAction(ChangeRequestAction.Refresh) }
    fun shop(): String? = session.activeModel()?.state?.value?.request?.shopSlug?.takeIf(String::isNotBlank)
    fun close() = session.close()
}

/** The shared models own request policy. The native controller owns their store and lifetime. */
private class AccountContributionDestination<Model : ViewModel, State>(
    private val account: ContributionsAccount,
    private val empty: (Boolean, String?) -> State,
    create: (ContributionsAccount) -> Model,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val active = MutableStateFlow<Owner<Model>?>(null)

    init {
        scope.launch {
            account.accountKey.distinctUntilChanged().collectLatest { key ->
                val model = create(account)
                val store = ViewModelStore().apply { put("contributions", model) }
                try {
                    active.value = Owner(key, model)
                    awaitCancellation()
                } finally {
                    active.value = null
                    store.clear()
                }
            }
        }
    }

    fun activeModel(): Model? = active.value?.takeIf { it.key == account.currentAccountKey() }?.model

    fun observe(states: (Model) -> Flow<State>, receive: (State) -> Unit): Job = scope.launch {
        active.collectLatest { owner ->
            if (owner == null) {
                receive(empty(true, null))
            } else {
                states(owner.model).combine(account.accountKey) { state, key ->
                    if (key == owner.key && key == account.currentAccountKey()) state else empty(key != null, key.signInError())
                }.collect(receive)
            }
        }
    }

    fun close() = scope.cancel()

    private data class Owner<Model>(val key: String?, val model: Model)
}

private fun String?.signInError() = if (this == null) "sign_in_prompt" else null
