package com.coffeepeek.admin.di.shop

import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlin.concurrent.Volatile

/** A modal lease, not a token snapshot: same-account token renewal stays valid. */
internal class AndroidShopCheckInSession(
    private val sessions: SessionRepository,
    private val repository: ShopCheckInRepository,
) : ShopCheckInRepository {
    val userId = sessions.peekSession()?.userId
    @Volatile private var revoked = false

    fun isCurrent(session: Session? = sessions.peekSession()): Boolean =
        !revoked && !userId.isNullOrBlank() && session?.userId == userId && sessions.isActiveSession(session)

    fun revoke() { revoked = true }

    override suspend fun getDrinkOptions() = withSession { repository.getDrinkOptions() }
    override suspend fun create(input: ShopCheckInCreateInput) = withSession { repository.create(input) }

    private suspend fun <T> withSession(block: suspend () -> T): T = coroutineScope {
        ensureCurrent()
        val operation = async(start = CoroutineStart.LAZY) {
            ensureCurrent()
            block().also { ensureCurrent() }
        }
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            sessions.observeSession().catch { error ->
                if (error is CancellationException) throw error
                revoke()
                operation.cancel(CancellationException("Could not observe check-in session", error))
            }.collect { session ->
                if (!isCurrent(session)) {
                    revoke()
                    operation.cancel(CancellationException("Check-in session ended"))
                }
            }
        }
        try { operation.await() } finally { observer.cancel() }
    }

    private fun ensureCurrent() {
        if (!isCurrent()) {
            revoke()
            throw CancellationException("Check-in session ended")
        }
    }
}
