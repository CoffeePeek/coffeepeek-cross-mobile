package com.coffeepeek.admin.feature.authorblocking

import com.coffeepeek.admin.feature.authorblocking.data.AuthorBlockRepositoryImpl
import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.test.*

class AuthorBlockRepositoryTest {
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private fun page(items: String = "", cursor: String? = null, revision: String = "r") =
        """{"isSuccess":true,"data":{"items":[$items],"nextCursor":${cursor?.let { "\"$it\"" } ?: "null"},"revision":"$revision"}}"""
    private fun author(id: String, slug: String = id) = """{"authorId":"$id","userName":"Author $id","address":{"slug":"$slug"}}"""
    private fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Test fun loadsEveryPageAndResolvesLegacySlugBeforeBlocking(): Unit = runBlocking {
        val methods = mutableListOf<String>()
        val client = HttpClientFactory(MockEngine { request ->
            methods += "${request.method.value} ${request.url.encodedPath}"
            assertEquals("no-store", request.headers[HttpHeaders.CacheControl])
            when {
                request.method == HttpMethod.Put -> {
                    assertEquals("/api/v1/me/blocks/opaque-new", request.url.encodedPath)
                    respond("""{"data":{"authorId":"opaque-new","isBlocked":true}}""", headers = headers)
                }
                request.url.encodedPath.startsWith("/api/Users/") -> {
                    assertEquals("/api/Users/old%20%2F%20slug", request.url.encodedPath)
                    respond("""{"data":{"authorId":"opaque-new"}}""", headers = headers)
                }
                request.url.parameters["cursor"] == "next" -> respond(page(author("b")), headers = headers)
                else -> respond(page(author("a"), "next"), headers = headers)
            }
        }).api("https://api.example")
        val repo = AuthorBlockRepositoryImpl(client, BlockSessions(), scope())
        try {
            withTimeout(5_000) { repo.state.first { it.ready } }
            assertEquals(setOf("a", "b"), repo.visibility.value.blockedIds)
            repo.setBlocked(null, "old / slug", "New author", null, true).getOrThrow()
            assertEquals(setOf("a", "b", "opaque-new"), repo.visibility.value.blockedIds)
            assertTrue(repo.visibility.value.hides("opaque-new", "renamed-slug"))
            assertEquals(1, methods.count { it.startsWith("PUT") })
        } finally { repo.close(); client.close() }
    }

    @Test fun failedMutationAndFailedReconciliationNeverHideAnUnconfirmedAuthor(): Unit = runBlocking {
        var fail = false
        val client = HttpClientFactory(MockEngine {
            if (fail) respond("{}", HttpStatusCode.ServiceUnavailable, headers)
            else respond(page(author("existing")), headers = headers)
        }).api("https://api.example")
        val repo = AuthorBlockRepositoryImpl(client, BlockSessions(), scope())
        try {
            withTimeout(5_000) { repo.state.first { it.ready } }; fail = true
            assertTrue(repo.setBlocked("new", null, "New", null, true).isFailure)
            assertEquals(setOf("existing"), repo.visibility.value.blockedIds)
            assertFalse(repo.visibility.value.hides("new", null))
            assertTrue(repo.refresh().isFailure)
            assertEquals(setOf("existing"), repo.visibility.value.blockedIds)
        } finally { repo.close(); client.close() }
    }

    @Test fun reconcilesTimeoutAfterServerCommittedWithoutDuplicateWrite(): Unit = runBlocking {
        var committed = false; var writes = 0
        val client = HttpClientFactory(MockEngine { request ->
            if (request.method == HttpMethod.Put) { committed = true; writes++; error("Response lost") }
            respond(page(if (committed) author("target") else ""), headers = headers)
        }).api("https://api.example")
        val repo = AuthorBlockRepositoryImpl(client, BlockSessions(), scope())
        try {
            withTimeout(5_000) { repo.state.first { it.ready } }
            repo.setBlocked("target", "target", "Target", null, true).getOrThrow()
            assertEquals(setOf("target"), repo.visibility.value.blockedIds)
            assertEquals(1, writes)
        } finally { repo.close(); client.close() }
    }

    @Test fun accountSwitchDiscardsLateMutationAndNeverInheritsBlocks(): Unit = runBlocking {
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val sessions = BlockSessions()
        val client = HttpClientFactory(MockEngine { request ->
            if (request.method == HttpMethod.Put) {
                started.complete(Unit); release.await()
                respond("""{"data":{"authorId":"target","isBlocked":true}}""", headers = headers)
            } else respond(page(), headers = headers)
        }).api("https://api.example")
        val repo = AuthorBlockRepositoryImpl(client, sessions, scope())
        try {
            withTimeout(5_000) { repo.state.first { it.ready } }
            val write = async { repo.setBlocked("target", "target", "Target", null, true) }
            started.await(); sessions.applySession(Session("token-b", userId = "b"))
            assertFalse(repo.visibility.value.ready)
            release.complete(Unit)
            assertFailsWith<CancellationException> { write.await() }
            withTimeout(5_000) { repo.state.first { it.ready } }
            assertTrue(repo.visibility.value.blockedIds.isEmpty())
        } finally { release.complete(Unit); repo.close(); client.close() }
    }

    @Test fun incompleteOrInconsistentPagesNeverPublishPartialBlockList(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { request ->
            respond(if (request.url.parameters["cursor"] == null) page(author("a"), "next", "one")
                else page(author("b"), revision = "two"), headers = headers)
        }).api("https://api.example")
        val repo = AuthorBlockRepositoryImpl(client, BlockSessions(), scope())
        try {
            withTimeout(5_000) { repo.state.first { it.error != null } }
            assertFalse(repo.visibility.value.ready)
            assertTrue(repo.state.value.items.isEmpty())
            assertTrue(repo.visibility.value.hides("any", "any"))
        } finally { repo.close(); client.close() }
    }

    @Test fun guestsCannotWriteAndDoNotLoadPersonalBlocks(): Unit = runBlocking {
        val client = HttpClientFactory(MockEngine { error("Guest request") }).api("https://api.example")
        val repo = AuthorBlockRepositoryImpl(client, BlockSessions(null), scope())
        try {
            assertTrue(repo.setBlocked("target", null, "Target", null, true).isFailure)
            assertTrue(repo.visibility.value.ready)
            assertTrue(repo.visibility.value.blockedIds.isEmpty())
        } finally { repo.close(); client.close() }
    }
}

private class BlockSessions(initial: Session? = Session("token-a", userId = "a")) : SessionRepository {
    private val session = MutableStateFlow(initial)
    override fun peekSession() = session.value
    override fun applySession(session: Session?) { this.session.value = session }
    override fun isActiveSession(session: Session?) = session != null && session == this.session.value
    override suspend fun getSession() = peekSession()
    override suspend fun persistSession(session: Session?) = applySession(session)
    override suspend fun saveSession(session: Session?) = applySession(session)
    override suspend fun warmCache() = Unit
    override fun observeSession() = session
    override suspend fun isLoggedIn() = session.value != null
}
