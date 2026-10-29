package com.coffeepeek.admin.di.contentreport

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.contentreport.domain.model.*
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlin.concurrent.Volatile

/** Permanently revoked on replacement; token renewal for a known user preserves the form. */
internal class ContentReportSession(private val sessions: SessionReader, expected: Session? = sessions.peekSession()) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    private val owner = key(expected)
    val isLoggedIn = owner != null
    @Volatile private var revoked = false

    private fun key(session: Session?): String? = session?.takeIf(sessions::isActiveSession)
        ?.let { session -> session.userId?.takeIf(String::isNotBlank)?.let { "user:$it" } ?: "token:${session.accessToken}" }

    fun isCurrent(session: Session? = sessions.peekSession()) = !revoked && key(session) == owner

    fun revoke() {
        revoked = true
        viewModelStore.clear()
    }

    fun bind(repository: ContentReportRepository) = ContentReportRepository { target, reason ->
        guarded { repository.submit(target, reason) }
    }

    private fun ensureCurrent() {
        if (!isCurrent()) {
            revoked = true
            throw CancellationException("Content report account was replaced")
        }
    }

    private suspend fun guarded(block: suspend () -> Result<Unit>): Result<Unit> = coroutineScope {
        currentCoroutineContext().ensureActive()
        ensureCurrent()
        if (!isLoggedIn) return@coroutineScope Result.failure(ContentReportFailure(ContentReportProblem.SignInRequired))
        val operation = async(start = CoroutineStart.LAZY) {
            ensureCurrent()
            block().also {
                currentCoroutineContext().ensureActive()
                ensureCurrent()
                it.exceptionOrNull()?.let { error -> if (error is CancellationException) throw error }
            }
        }
        val observer = launch(start = CoroutineStart.UNDISPATCHED) { observe(operation) }
        try { operation.await() } finally { observer.cancel() }
    }

    private suspend fun observe(operation: Job) {
        sessions.observeSession().catch { error ->
            if (error is CancellationException) throw error
            revoked = true
            operation.cancel(CancellationException("Could not observe report account", error))
        }.collect { session ->
            if (!isCurrent(session)) {
                revoked = true
                operation.cancel(CancellationException("Content report account was replaced"))
            }
        }
    }
}
