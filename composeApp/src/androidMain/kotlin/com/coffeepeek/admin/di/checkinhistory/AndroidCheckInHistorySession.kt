package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

/** Private reads are permanently revoked on replacement; same-account token renewal is not replacement. */
internal class AndroidCheckInHistorySession(
    private val sessions: SessionRepository,
    session: Session? = sessions.peekSession(),
) {
    val isLoggedIn = sessions.isActiveSession(session) && !session?.userId.isNullOrBlank()
    private val accountId = session?.userId.takeIf { isLoggedIn }
    @Volatile private var revoked = false

    fun isCurrent(session: Session? = sessions.peekSession()): Boolean {
        val active = sessions.isActiveSession(session) && !session?.userId.isNullOrBlank()
        return !revoked && active == isLoggedIn && (!active || session.userId == accountId)
    }

    fun revoke() { revoked = true }

    fun bind(repository: CheckInHistoryRepository): CheckInHistoryRepository = object : CheckInHistoryRepository {
        override suspend fun page(page: Int, pageSize: Int) = read { repository.page(page, pageSize) }
        override suspend fun period(period: com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod) =
            read { repository.period(period) }
    }

    private suspend fun <T> read(block: suspend () -> Result<T>): Result<T> = coroutineScope {
        ensureCurrent()
        if (!isLoggedIn) return@coroutineScope Result.failure(IllegalStateException("Sign in before reading history"))
        val operation = async(start = CoroutineStart.LAZY) {
            ensureCurrent()
            block().also {
                ensureCurrent()
                it.exceptionOrNull()?.let { error -> if (error is CancellationException) throw error }
            }
        }
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            sessions.observeSession().catch { error ->
                if (error is CancellationException) throw error
                revoke()
                operation.cancel(CancellationException("Could not observe history session", error))
            }.collect { value ->
                if (!isCurrent(value)) {
                    revoke()
                    operation.cancel(CancellationException("History session was replaced"))
                }
            }
        }
        try { operation.await() } finally { observer.cancel() }
    }

    private fun ensureCurrent() {
        if (!isCurrent()) {
            revoke()
            throw CancellationException("History session was replaced")
        }
    }
}
