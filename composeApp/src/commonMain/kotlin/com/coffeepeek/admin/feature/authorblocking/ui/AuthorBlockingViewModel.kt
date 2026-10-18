package com.coffeepeek.admin.feature.authorblocking.ui

import androidx.lifecycle.ViewModel
import com.coffeepeek.admin.feature.authorblocking.api.*
import com.coffeepeek.admin.feature.authorblocking.domain.AuthorBlockRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal data class AuthorBlockPrompt(val author: AuthorReference, val blocked: Boolean,
    val working: Boolean = false, val error: String? = null)

internal class AuthorBlockingViewModel(
    private val repository: AuthorBlockRepository,
    private val sessions: SessionRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : ViewModel(), AuthorBlockActions, AutoCloseable {
    val blocks = repository.state
    private val _prompt = MutableStateFlow<AuthorBlockPrompt?>(null)
    val prompt = _prompt.asStateFlow()
    private val _needsLogin = MutableStateFlow(false)
    val needsLogin = _needsLogin.asStateFlow()
    private var mutation: Job? = null

    init {
        addCloseable(this)
        scope.launch {
            sessions.observeSession().map { session -> session?.takeIf(sessions::isActiveSession)?.let { it.userId ?: it.accessToken } }
                .distinctUntilChanged().drop(1).collect { mutation?.cancel(); _prompt.value = null; _needsLogin.value = false }
        }
    }
    override fun requestBlock(author: AuthorReference) = request(author, true)
    override fun requestUnblock(author: AuthorReference) = request(author, false)
    private fun request(author: AuthorReference, blocked: Boolean) {
        if (_prompt.value?.working == true) return
        if (!sessions.isActiveSession(sessions.peekSession())) { _needsLogin.value = true; return }
        _prompt.value = AuthorBlockPrompt(author, blocked)
    }
    fun dismiss() { if (_prompt.value?.working != true) _prompt.value = null }
    fun loginHandled() { _needsLogin.value = false }
    fun refresh() { scope.launch { repository.refresh() } }
    fun confirm() {
        val request = _prompt.value?.takeUnless { it.working } ?: return
        _prompt.value = request.copy(working = true, error = null)
        mutation = scope.launch {
            try {
                val result = repository.setBlocked(request.author.id, request.author.slug, request.author.name, request.author.avatarUrl, request.blocked)
                ensureActive()
                result.onSuccess { _prompt.value = null }
                    .onFailure { _prompt.value = request.copy(error = "Не удалось сохранить блокировку. Проверьте подключение и повторите попытку.") }
            } finally {
                if (!isActive) _prompt.value = null
            }
        }
    }
    override fun close() { scope.cancel() }
}
