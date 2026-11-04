package com.coffeepeek.feature.authorblocking.impl

import com.coffeepeek.feature.authorblocking.api.*
import com.coffeepeek.feature.authorblocking.domain.AuthorBlockRepository
import com.coffeepeek.feature.authorblocking.domain.AuthorBlocksState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class AuthorBlockingViewModel(
    private val repository: AuthorBlockRepository,
    private val account: AuthorBlockingAccount,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : AuthorBlockingPresenter {
    override val blocks = repository.state.map(AuthorBlocksState::presentation)
        .stateIn(scope, SharingStarted.Eagerly, repository.state.value.presentation())
    private val _prompt = MutableStateFlow<AuthorBlockPrompt?>(null)
    override val prompt = _prompt.asStateFlow()
    private val _needsLogin = MutableStateFlow(false)
    override val needsLogin = _needsLogin.asStateFlow()
    private var mutation: Job? = null

    init {
        scope.launch {
            account.changes
                .distinctUntilChanged().drop(1).collect { mutation?.cancel(); _prompt.value = null; _needsLogin.value = false }
        }
    }
    override fun requestBlock(author: AuthorReference) = request(author, true)
    override fun requestUnblock(author: AuthorReference) = request(author, false)
    private fun request(author: AuthorReference, blocked: Boolean) {
        if (_prompt.value?.working == true) return
        if (account.current() == null) { _needsLogin.value = true; return }
        _prompt.value = AuthorBlockPrompt(author, blocked)
    }
    override fun dismiss() { if (_prompt.value?.working != true) _prompt.value = null }
    override fun loginHandled() { _needsLogin.value = false }
    override fun refresh() { scope.launch { repository.refresh() } }
    override fun confirm() {
        val request = _prompt.value?.takeUnless { it.working } ?: return
        _prompt.value = request.copy(working = true, error = null)
        mutation = scope.launch {
            try {
                val result = repository.setBlocked(request.author.id, request.author.slug, request.author.name, request.author.avatarUrl, request.blocked)
                ensureActive()
                result.onSuccess { _prompt.value = null }
                    .onFailure { _prompt.value = request.copy(error = AuthorBlockError.SaveFailed) }
            } finally {
                if (!isActive) _prompt.value = null
            }
        }
    }
    override fun close() { scope.cancel() }
}

private fun AuthorBlocksState.presentation() = AuthorBlockingListState(
    items = items.map { BlockedAuthorItem(it.id, it.name, it.slug, it.avatarUrl) },
    signedIn = signedIn, ready = ready, loading = loading, error = error,
)
