package com.coffeepeek.feature.shelf.data.repository

import com.coffeepeek.feature.shelf.data.backend.*
import com.coffeepeek.feature.shelf.data.mapper.toDomain
import com.coffeepeek.feature.shelf.data.mapper.toWrite
import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.domain.usecase.ShelfRatingRange
import com.coffeepeek.feature.shelf.domain.usecase.canCreate
import com.coffeepeek.feature.shelf.domain.usecase.isValid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Writes run one at a time so the server sees moves in the order the user made them. */
internal class RemoteShelfRepository(
    private val backend: ShelfBackend,
    private val uploads: ShelfCardUploadBackend,
    private val apiBaseUrl: String,
    private val account: ShelfAccount,
) : ShelfRepository {
    private val state = MutableStateFlow<ShelfSnapshot?>(null)
    private val writes = Mutex()

    override val shelf: Flow<List<ShelfBag>?>
        get() =
            combine(state, account.owners) { snapshot, owner -> snapshot?.takeIf { owner != null && it.owner == owner }?.shown }
                .distinctUntilChanged()

    override suspend fun refresh(): Result<List<ShelfBag>> = owned { owner -> load(owner) }

    override suspend fun archive(page: Int, pageSize: Int, query: String?): Result<ShelfArchivePage> = owned {
        if (page !in 1..10000 || pageSize !in 1..100 || (query?.length ?: 0) > 100) throw ShelfFailure(ShelfIssue.InvalidInput)
        val response = backend.archive(page, pageSize, query?.trim()?.ifEmpty { null })
        ShelfArchivePage(response.items.map { it.toDomain(apiBaseUrl) }, response.totalCount, page, pageSize)
    }

    override suspend fun add(details: ShelfBagDetails, card: ShelfBagCard?): Result<ShelfBag> = owned { owner ->
        if (!details.canCreate(card != null)) throw ShelfFailure(ShelfIssue.InvalidInput)
        val photo = card?.let { uploads.upload(it) }
        val bag = writes.withLock {
            backend.create(details.toWrite(photo)).toDomain(apiBaseUrl).also { created -> confirm(owner) { listOf(created) + it } }
        }
        bag
    }.contextual(notFound = ShelfIssue.BeanUnavailable, conflict = ShelfIssue.ShelfFull)

    override suspend fun update(id: String, details: ShelfBagDetails, card: ShelfBagCard?): Result<ShelfBag> = owned { owner ->
        if (!details.isValid()) throw ShelfFailure(ShelfIssue.InvalidInput)
        val photo = card?.let { uploads.upload(it) }
        writes.withLock {
            backend.update(id, details.toWrite(photo)).toDomain(apiBaseUrl).also { updated ->
                confirm(owner) { bags -> bags.map { if (it.id == updated.id) updated else it } }
            }
        }
    }

    override suspend fun move(id: String, toIndex: Int): Result<Unit> = optimistic({ it.moved(id, toIndex) }) { owner, snapshot ->
        val order = backend.order(snapshot().orderToSend()).map { it.toDomain(apiBaseUrl) }.sortedBy { it.position }
        settle(owner) { order }
    }

    override suspend fun finish(id: String, rating: Int?, buyAgain: Boolean?): Result<ShelfBag> {
        if (rating != null && rating !in ShelfRatingRange) return Result.failure(ShelfFailure(ShelfIssue.InvalidInput))
        val removed = { bags: List<ShelfBag> -> bags.filterNot { it.id == id } }
        return optimistic(removed) { owner, _ ->
            backend.finish(id, ShelfFinishDto(rating, buyAgain)).toDomain(apiBaseUrl).also { settle(owner, removed) }
        }
    }

    override suspend fun restore(id: String, position: Int?): Result<ShelfBag> = owned { owner ->
        writes.withLock {
            backend.restore(id, ShelfRestoreDto(position)).toDomain(apiBaseUrl).also { restored ->
                confirm(owner) { bags -> bags.filterNot { it.id == id }.toMutableList().apply { add((restored.position ?: 0).coerceIn(0, size), restored) } }
            }
        }
    }

    override suspend fun delete(id: String): Result<Unit> = owned { owner ->
        writes.withLock {
            backend.delete(id)
            confirm(owner) { bags -> bags.filterNot { it.id == id } }
        }
    }

    private suspend fun load(owner: String): List<ShelfBag> {
        val bags = backend.shelf().map { it.toDomain(apiBaseUrl) }.sortedBy { it.position }
        if (bags.any { it.status != ShelfBagStatus.OnShelf }) throw ShelfFailure(ShelfIssue.InvalidResponse)
        state.update { old -> ShelfSnapshot(owner, bags.renumbered(), bags.renumbered(), pending = old?.takeIf { it.owner == owner }?.pending ?: 0) }
        return bags.renumbered()
    }

    /**
     * Shows [change] at once, then runs [remote] after earlier writes. On failure every unconfirmed change rolls back;
     * a stale shelf (409) is reloaded from the server.
     */
    private suspend fun <T> optimistic(
        change: (List<ShelfBag>) -> List<ShelfBag>,
        remote: suspend (owner: String, snapshot: () -> ShelfSnapshot) -> T,
    ): Result<T> = owned { owner ->
        if (state.value?.owner != owner) load(owner)
        state.update { it?.takeIf { s -> s.owner == owner }?.optimistic(change) ?: it }
        try {
            writes.withLock { remote(owner) { state.value?.takeIf { it.owner == owner } ?: throw ShelfFailure(ShelfIssue.AccountChanged) } }
        } catch (failure: Throwable) {
            settle(owner, null)
            if (failure !is CancellationException && failure.toShelfFailure().issue == ShelfIssue.Stale) reloadQuietly(owner)
            throw failure
        }
    }

    /** After a refused move the server's order wins; if even that fails, the rolled-back snapshot stays. */
    private suspend fun reloadQuietly(owner: String) {
        try { load(owner) } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) {}
    }

    private fun confirm(owner: String, change: (List<ShelfBag>) -> List<ShelfBag>) =
        state.update { if (it?.owner == owner) it.confirm(change) else it }

    private fun settle(owner: String, change: ((List<ShelfBag>) -> List<ShelfBag>)?) =
        state.update { if (it?.owner == owner) it.settle(change) else it }

    /** Runs [block] for the current owner and cancels it when the account changes meanwhile. */
    private suspend fun <T> owned(block: suspend (owner: String) -> T): Result<T> = shelfRequestResult {
        val owner = account.owner ?: throw ShelfFailure(ShelfIssue.SignedOut)
        state.update { it?.takeIf { s -> s.owner == owner } }
        coroutineScope {
            val watcher = launch { account.owners.first { it != owner }; throw ShelfFailure(ShelfIssue.AccountChanged) }
            try { block(owner) } finally { watcher.cancel() }
        }
    }
}

/** Status codes whose meaning depends on the request: an unpublished product on create, a full shelf, and so on. */
private fun <T> Result<T>.contextual(notFound: ShelfIssue? = null, conflict: ShelfIssue? = null): Result<T> {
    val failure = exceptionOrNull() as? ShelfFailure ?: return this
    val issue = when (failure.issue) {
        ShelfIssue.NotFound -> notFound
        ShelfIssue.Stale -> conflict
        else -> null
    } ?: return this
    return Result.failure(ShelfFailure(issue, failure.statusCode))
}
