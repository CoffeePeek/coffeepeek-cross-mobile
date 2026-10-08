package com.coffeepeek.data.session

import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlin.test.*

class SessionTokenProviderTest {
    @Test
    fun restoresSavedSessionAndUsesImmediateMemoryUpdatesWithoutWaitingForPersistenceFlow() = runBlocking {
        val restored = CompletableDeferred<Unit>()
        val saved = Session("saved-token", "refresh", "one")
        val sessions = object : SessionRepository {
            var current: Session? = null
            override fun peekSession() = current
            override fun applySession(session: Session?) { current = session }
            override fun isActiveSession(session: Session?) = !session?.accessToken.isNullOrBlank()
            override suspend fun getSession() = saved.also(::applySession)
            override suspend fun warmCache() { getSession(); restored.complete(Unit) }
            override fun observeSession() = flowOf(saved)
            override suspend fun persistSession(session: Session?) = Unit
            override suspend fun saveSession(session: Session?) = applySession(session)
            override suspend fun isLoggedIn() = isActiveSession(current)
        }
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        try {
            val provider = SessionTokenProvider(sessions, scope)
            withTimeout(5_000) { restored.await() }
            assertEquals("saved-token", provider.current()?.accessToken)
            sessions.applySession(Session("new-user", "new-refresh", "two"))
            assertEquals("new-user", provider.current()?.accessToken)
            assertEquals("new-refresh", provider.current()?.refreshToken)
            sessions.applySession(null)
            assertNull(provider.current())
        } finally { scope.cancel() }
    }
}
