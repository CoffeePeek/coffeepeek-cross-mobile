package com.coffeepeek.feature.favorites.data.repository

import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

internal data class FavoriteCacheOwner(val account: String, val generation: Long)

/** One cache owns full rows and the metadata-independent view of confirmed membership. */
internal class ServerFavoritesCache(private val activeOwner: () -> FavoriteCacheOwner?) {
    private data class Confirmed(val favorite: Boolean, val revision: Long)
    private data class Snapshot(val owner: FavoriteCacheOwner? = null, val rows: List<FavoriteEntry>? = null,
        val confirmed: Map<FavoriteTarget, Confirmed> = emptyMap(), val revision: Long = 0, val error: Exception? = null)
    private val state = MutableStateFlow(Snapshot())

    fun revision() = state.value.revision
    fun rows(owner: FavoriteCacheOwner) = state.filter { it.owner == owner }.mapNotNull { it.fullResult() }
    fun memberships(owner: FavoriteCacheOwner) = state.filter { it.owner == owner }.map(::membershipResult)
    fun peek(owner: FavoriteCacheOwner): List<FavoriteEntry>? = snapshot(owner)?.completeRows()

    fun cachedMembership(target: FavoriteTarget): Boolean? {
        val owner = activeOwner() ?: return null
        val snapshot = snapshot(owner) ?: return null
        return snapshot.confirmed[target]?.favorite ?: snapshot.rows?.any { it.target() == target }
    }

    fun confirm(owner: FavoriteCacheOwner, target: FavoriteTarget, favorite: Boolean): Boolean {
        if (activeOwner() != owner || target.slug.isBlank()) return false
        state.update { current ->
            requireOwner(owner)
            val before = current.forOwner(owner)
            val revision = before.revision + 1
            val rows = before.rows?.filterNot { !favorite && it.target() == target }
            before.copy(rows = rows,
                confirmed = before.confirmed + (target to Confirmed(favorite, revision)), revision = revision)
        }
        return true
    }

    fun recordRows(owner: FavoriteCacheOwner, rows: List<FavoriteEntry>, startedRevision: Long): List<FavoriteEntry>? {
        val snapshot = state.updateAndGet { current ->
            requireOwner(owner)
            val before = current.forOwner(owner)
            val newer = before.confirmed.filterValues { it.revision > startedRevision }
            val reconciled = rows.filterNot { it.target()?.let(newer::get)?.favorite == false }
            before.copy(rows = reconciled, confirmed = newer, error = null)
        }
        return snapshot.completeRows()
    }

    fun recordWrite(owner: FavoriteCacheOwner, entry: FavoriteEntry, favorite: Boolean) {
        val target = requireNotNull(entry.target())
        state.update { current ->
            requireOwner(owner)
            val before = current.forOwner(owner)
            val rows = before.rows.orEmpty()
            val existing = rows.firstOrNull { it.target() == target }
            val remaining = rows.filterNot { it.target() == target }
            val revision = before.revision + 1
            before.copy(rows = if (favorite) listOf(existing ?: entry) + remaining else remaining,
                confirmed = before.confirmed + (target to Confirmed(favorite, revision)), revision = revision, error = null)
        }
    }

    fun fail(owner: FavoriteCacheOwner, error: Exception) {
        state.update { current ->
            requireOwner(owner)
            val before = current.forOwner(owner)
            if (before.completeRows() != null) before else before.copy(error = error)
        }
    }

    fun clear() = state.update { Snapshot(revision = it.revision + 1) }

    private fun snapshot(owner: FavoriteCacheOwner) = state.value.takeIf { it.owner == owner && activeOwner() == owner }
    private fun Snapshot.forOwner(owner: FavoriteCacheOwner) = if (this.owner == owner) this else Snapshot(owner, revision = revision)
    private fun requireOwner(owner: FavoriteCacheOwner) {
        if (activeOwner() != owner) throw CancellationException("Favorites account changed")
    }

    private fun membershipResult(snapshot: Snapshot): Result<Set<FavoriteTarget>> {
        if (snapshot.rows == null && snapshot.error != null && snapshot.confirmed.isEmpty()) return Result.failure(snapshot.error)
        val targets = snapshot.rows.orEmpty().mapNotNull { it.target() }.toMutableSet()
        snapshot.confirmed.forEach { (target, value) -> if (value.favorite) targets.add(target) else targets.remove(target) }
        return Result.success(targets)
    }

    private fun Snapshot.completeRows(): List<FavoriteEntry>? {
        val knownRows = rows ?: return null
        val targets = knownRows.mapNotNull { it.target() }.toSet()
        return knownRows.takeUnless { confirmed.any { (target, value) -> value.favorite && target !in targets } }
    }

    private fun Snapshot.fullResult(): Result<List<FavoriteEntry>>? =
        completeRows()?.let { Result.success(it) } ?: error?.let { Result.failure(it) }
}

private fun FavoriteEntry.target() = slug?.let { FavoriteTarget(kind, it) }
