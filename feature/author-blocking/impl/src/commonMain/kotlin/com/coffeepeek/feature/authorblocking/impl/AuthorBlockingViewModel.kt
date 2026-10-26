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
    private var observedAccount = account.current()
    private var promptAccount: String? = null

    init {
        scope.launch {
            account.changes.distinctUntilChanged().collect(::synchronizeAccount)
        }
    }
    private fun synchronizeAccount(current: String?) {
        if (current == observedAccount) return
        observedAccount = current
        mutation?.cancel()
        clearPrompt()
        _needsLogin.value = false
    }
    private fun clearPrompt() {
        _prompt.value = null
        promptAccount = null
    }
    override fun requestBlock(author: AuthorReference) = request(author, true)
    override fun requestUnblock(author: AuthorReference) = request(author, false)
    private fun request(author: AuthorReference, blocked: Boolean) {
        val owner = account.current()
        synchronizeAccount(owner)
        if (_prompt.value?.working == true) return
        if (owner == null) {
            _needsLogin.value = true
            return
        }
        promptAccount = owner
        _prompt.value = AuthorBlockPrompt(author, blocked)
    }
    override fun dismiss() { if (_prompt.value?.working != true) clearPrompt() }
    override fun loginHandled() { _needsLogin.value = false }
    override fun refresh() { scope.launch { repository.refresh() } }
    override fun confirm() {
        val request = _prompt.value?.takeUnless { it.working } ?: return
        val owner = promptAccount ?: return
        if (owner != account.current()) {
            synchronizeAccount(account.current())
            return
        }
        val active = request.copy(working = true, error = null)
        _prompt.value = active
        mutation = scope.launch { save(request, active, owner) }
    }
    private suspend fun save(request: AuthorBlockPrompt, active: AuthorBlockPrompt, owner: String) {
        try {
            synchronizeAccount(account.current())
            if (promptAccount != owner || _prompt.value !== active) return
            val result = repository.setBlocked(request.author.id, request.author.slug, request.author.name, request.author.avatarUrl, request.blocked)
            currentCoroutineContext().ensureActive()
            if (promptAccount == owner && account.current() == owner && _prompt.value === active) complete(request, result)
        } finally {
            if (!currentCoroutineContext().isActive && _prompt.value === active) clearPrompt()
        }
    }
    private fun complete(request: AuthorBlockPrompt, result: Result<Unit>) {
        result.onSuccess { clearPrompt() }.onFailure {
            _prompt.value = request.copy(error = "Не удалось сохранить блокировку. Проверьте подключение и повторите попытку.")
        }
    }
    override fun close() { scope.cancel() }
}

private fun AuthorBlocksState.presentation() = AuthorBlockingListState(
    items = items.map { BlockedAuthorItem(it.id, it.name, it.slug, it.avatarUrl) },
    signedIn = signedIn, ready = ready, loading = loading, error = error,
)
