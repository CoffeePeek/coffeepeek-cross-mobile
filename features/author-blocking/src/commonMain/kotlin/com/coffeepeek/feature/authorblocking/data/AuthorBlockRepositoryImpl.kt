package com.coffeepeek.feature.authorblocking.data

import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.authorblocking.domain.*
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

internal class AuthorBlockRepositoryImpl(
    private val client: HttpClient,
    private val sessions: SessionRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : AuthorBlockRepository, AuthorVisibility, AutoCloseable {
    private val lock = Mutex()
    private val epoch = MutableStateFlow(0L)
    private var account = key(sessions.peekSession())
    private val _state = MutableStateFlow(AuthorBlocksState(signedIn = account != null, ready = account == null))
    override val state = _state.asStateFlow()
    private val _visibility = MutableStateFlow(AuthorVisibilitySnapshot(ready = account == null))
    override val visibility = _visibility.asStateFlow()

    init {
        scope.launch {
            sessions.observeSession().map(::key).distinctUntilChanged().collectLatest { current ->
                val generation = epoch.updateAndGet { it + 1 }
                account = current
                _state.value = AuthorBlocksState(signedIn = current != null, ready = current == null, sessionVersion = generation)
                publish(emptyList(), current == null, generation, force = true)
                if (current != null) refresh()
            }
        }
    }

    private fun key(session: Session?): String? = session?.takeIf(sessions::isActiveSession)?.let { it.userId ?: it.accessToken }
    private fun requireCurrent(generation: Long) {
        if (epoch.value != generation || account != key(sessions.peekSession())) throw CancellationException("Account changed")
    }

    override suspend fun refresh(): Result<Unit> = attempt {
        val generation = epoch.value
        lock.withLock {
            requireCurrent(generation)
            if (account == null) return@withLock
            _state.update { if (it.sessionVersion == generation) it.copy(loading = true, error = null) else it }
            try {
                val items = readAll()
                requireCurrent(generation)
                replace(items, generation, force = true)
            } catch (cancelled: CancellationException) {
                _state.update { if (it.sessionVersion == generation) it.copy(loading = false) else it }
                throw cancelled
            } catch (e: Exception) {
                requireCurrent(generation)
                _state.update { if (it.sessionVersion == generation) it.copy(loading = false, error = "Не удалось загрузить блокировки. Повторите попытку.") else it }
                throw e
            }
        }
    }

    override suspend fun setBlocked(id: String?, slug: String?, name: String, avatarUrl: String?, blocked: Boolean): Result<Unit> = attempt {
        val generation = epoch.value
        lock.withLock {
            requireCurrent(generation)
            check(account != null) { "Войдите в аккаунт" }
            if (!state.value.ready) {
                val existing = readAll()
                requireCurrent(generation)
                replace(existing, generation)
            }
            val authorId = id?.takeIf(String::isNotBlank) ?: resolveId(slug)
            requireCurrent(generation)
            var confirmed = false
            try {
                val response = if (blocked) client.put("/api/v1/me/blocks") {
                    expectSuccess = false
                    url { appendPathSegments(authorId, encodeSlash = true) }
                    header(HttpHeaders.CacheControl, "no-store")
                } else client.delete("/api/v1/me/blocks") {
                    expectSuccess = false
                    url { appendPathSegments(authorId, encodeSlash = true) }
                    header(HttpHeaders.CacheControl, "no-store")
                }
                response.requireSuccess()
                val result = response.body<Envelope<BlockStateDto>>().value()
                check(result.authorId == authorId && result.isBlocked == blocked)
                confirmed = true
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) {
                // A timeout can happen after the server committed. Reconcile before offering retry.
                val actual = try { readAll() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { null }
                requireCurrent(generation)
                if (actual != null) {
                    replace(actual, generation)
                    confirmed = actual.any { it.id == authorId } == blocked
                }
                if (!confirmed) throw failure
            }
            requireCurrent(generation)
            if (confirmed) {
                val updated = state.value.items.filterNot { it.id == authorId }.let {
                    if (blocked) listOf(BlockedAuthor(authorId, name, slug, avatarUrl)) + it else it
                }
                // Publish immediately only after a server acknowledgement or successful reconciliation.
                replace(updated, generation)
            }
        }
    }

    private suspend fun resolveId(slug: String?): String {
        require(!slug.isNullOrBlank()) { "Автор недоступен" }
        val response = client.get("/api/Users") {
            expectSuccess = false
            url { appendPathSegments(slug, encodeSlash = true) }
            header(HttpHeaders.CacheControl, "no-store")
        }
        response.requireSuccess()
        return response.body<Envelope<IdentityDto>>().value().authorId?.takeIf(String::isNotBlank)
            ?: error("Обновите приложение после обновления сервера")
    }

    private suspend fun readAll(): List<BlockedAuthor> {
        val items = linkedMapOf<String, BlockedAuthor>()
        val cursors = mutableSetOf<String>()
        var cursor: String? = null
        var revision: String? = null
        do {
            val response = client.get("/api/v1/me/blocks") {
                expectSuccess = false
                header(HttpHeaders.CacheControl, "no-store")
                parameter("pageSize", 100)
                cursor?.let { parameter("cursor", it) }
            }
            response.requireSuccess()
            val page = response.body<Envelope<BlockPageDto>>().value()
            check(revision == null || revision == page.revision) { "Список изменился. Повторите загрузку." }
            revision = page.revision
            page.items.forEach { dto ->
                check(dto.authorId.isNotBlank())
                items[dto.authorId] = BlockedAuthor(dto.authorId, dto.userName, dto.address?.slug,
                    dto.avatarUrl?.let { avatar -> URLBuilder(response.call.request.url).apply {
                        parameters.clear()
                        fragment = ""
                        takeFrom(avatar)
                    }.buildString() })
            }
            cursor = page.nextCursor
            check(cursor == null || cursors.add(cursor)) { "Некорректный ответ сервера" }
        } while (cursor != null)
        return items.values.toList()
    }

    private fun replace(items: List<BlockedAuthor>, generation: Long, force: Boolean = false) {
        _state.update { if (it.sessionVersion == generation && account == key(sessions.peekSession())) AuthorBlocksState(items, signedIn = true, ready = true, sessionVersion = generation) else it }
        publish(items, ready = true, generation = generation, force = force)
    }

    private fun publish(items: List<BlockedAuthor>, ready: Boolean, generation: Long, force: Boolean = false) {
        val ids = items.mapTo(mutableSetOf()) { it.id }
        val slugs = items.mapNotNullTo(mutableSetOf()) { it.slug }
        _visibility.update { previous ->
            if (_state.value.sessionVersion != generation || account != key(sessions.peekSession())) previous
            else if (!force && previous.ready == ready && previous.blockedIds == ids && previous.blockedSlugs == slugs) previous
            else AuthorVisibilitySnapshot(previous.revision + 1, ready, ids, slugs)
        }
    }
    override fun close() { scope.cancel() }
}

private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try { Result.success(block()) }
    catch (cancelled: CancellationException) { throw cancelled } catch (e: Exception) { Result.failure(e) }
private fun HttpResponse.requireSuccess() { check(status.isSuccess()) { "Не удалось изменить блокировку. Повторите попытку." } }
@Serializable private data class Envelope<T>(val data: T? = null, val isSuccess: Boolean = true) {
    fun value(): T = checkNotNull(data.takeIf { isSuccess }) { "Некорректный ответ сервера" }
}
@Serializable private data class AddressDto(val slug: String? = null)
@Serializable private data class BlockDto(val authorId: String, val userName: String, val avatarUrl: String? = null, val address: AddressDto? = null)
@Serializable private data class BlockPageDto(val items: List<BlockDto>, val nextCursor: String? = null, val revision: String)
@Serializable private data class BlockStateDto(val authorId: String, val isBlocked: Boolean)
@Serializable private data class IdentityDto(val authorId: String? = null)
