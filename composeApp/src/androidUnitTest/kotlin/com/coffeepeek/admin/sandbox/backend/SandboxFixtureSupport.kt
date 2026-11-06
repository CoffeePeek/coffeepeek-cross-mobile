package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.favorites.data.repository.createAccountFavorites
import com.coffeepeek.feature.favorites.domain.model.FavoriteAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

/** Same shape as the Sandbox app's no-credential session, without its Android-only source set. */
internal class LocalSandboxSession : SessionStore {
    private val current = MutableStateFlow<Session?>(Session("local", "local", "11111111-1111-4111-8111-111111111111"))
    override fun peekSession() = current.value
    override fun applySession(session: Session?) { current.value = session }
    override fun isActiveSession(session: Session?) = session != null
    override suspend fun getSession() = peekSession()
    override suspend fun persistSession(session: Session?) = applySession(session)
    override suspend fun replaceSession(expected: Session?, session: Session?) = current.compareAndSet(expected, session)
    override suspend fun saveSession(session: Session?) = applySession(session)
    override suspend fun warmCache() = Unit
    override fun observeSession() = current.asStateFlow()
    override suspend fun isLoggedIn() = current.value != null
}

internal fun sandboxAccountFavorites(client: ApiClients) = createAccountFavorites(client.client, LocalSandboxSession())

internal fun sandboxShopFavorite(slug: String) =
    FavoriteEntry(FavoriteKind.CoffeeShop, FavoriteAddress(slug, "/coffee-shops/$slug", 1, false))

/** A fresh in-memory Sandbox server and the four mock-only transports in front of it. */
internal suspend fun withSandboxFixture(temporary: TemporaryFolder, block: suspend (SandboxBackend, ApiClients) -> Unit) {
    val backend = SandboxBackend(byteArrayOf(1, 2, 3), today = LocalDate.parse("2026-10-18"))
    val client = ApiClients(SandboxBackend.BASE_URL, temporary.newFolder().path, false,
        credentials = object : SessionCredentials {
            override suspend fun load(): SessionTokens? { return null }
            override suspend fun save(tokens: SessionTokens?) { error("No credential writes") }
            override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean = false
        },
        refreshWith = { { Result.failure(IllegalStateException("unused")) } },
        apiClientFactory = backend::apiClient, uploadClientFactory = backend::uploadClient)
    try { block(backend, client) } finally {
        client.client.close(); client.plainClient.close(); client.uploadClient.close(); client.imageClient.close(); backend.close()
    }
}
