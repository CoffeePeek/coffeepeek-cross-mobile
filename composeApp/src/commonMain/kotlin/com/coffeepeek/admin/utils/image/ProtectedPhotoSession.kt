package com.coffeepeek.admin.utils.image

import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** Credentials stay in the auth layer; keys use a rotating, opaque identity. */
@OptIn(ExperimentalAtomicApi::class)
internal object ProtectedPhotoSession {
    private class State(val session: Session?, val scope: Any = Any())
    private val state = AtomicReference(State(null))
    private val worker = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _changes = MutableStateFlow(Any())
    val changes = _changes.asStateFlow()
    private var sessions: SessionRepository? = null

    fun scope(session: Session?): Any {
        sync(if (sessions != null) sessions?.peekSession() else session)
        return state.load().scope
    }

    fun current(scope: Any): Boolean {
        val current = state.load()
        return current.scope === scope && (sessions == null || sessions?.peekSession() == current.session)
    }

    fun session(scope: Any): Session? = state.load().takeIf { it.scope === scope && current(scope) }?.session

    private fun sync(session: Session?, force: Boolean = false) {
        while (true) {
            val old = state.load()
            if (!force && old.session == session) return
            val next = State(session)
            if (state.compareAndSet(old, next)) {
                _changes.value = next.scope
                worker.launch {
                    protectedPhotoCache.discardInvalidScopes(::current)
                    protectedPhotoPainterCache.discardInvalidScopes(::current)
                }
                return
            }
        }
    }

    fun invalidate() = sync(if (sessions != null) sessions?.peekSession() else state.load().session, force = true)

    fun install(repository: SessionRepository, client: HttpClient, visibility: Flow<*> = emptyFlow<Unit>()) {
        sessions = repository
        sync(repository.peekSession(), force = true)
        installPhotoSessionInvalidation(repository, client, worker, { sync(it) }, ::invalidate, visibility)
    }
}
