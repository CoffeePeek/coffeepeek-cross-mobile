package com.coffeepeek.feature.collections.data.repository

import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary
import com.coffeepeek.feature.collections.domain.model.SavedCollectionTarget
import com.coffeepeek.feature.collections.domain.model.SavedCollectionFeaturedSelection
import com.coffeepeek.feature.collections.domain.model.SavedCollectionVisibility
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.days
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** In-memory owner library snapshots. Every read rechecks the active session before exposing data. */
internal class OwnerCollectionsCache(private val account: CollectionsAccount,
    private val timeSource: TimeSource = TimeSource.Monotonic) {
    private data class Snapshot(val owner: String? = null,
        val lists: Map<SavedCollectionTarget?, List<SavedCollectionSummary>> = emptyMap(),
        val featured: SavedCollectionFeaturedSelection? = null,
        val confirmedAt: Map<SavedCollectionTarget?, TimeMark> = emptyMap())

    private val state = MutableStateFlow(Snapshot())

    fun peek(target: SavedCollectionTarget? = null): List<SavedCollectionSummary>? {
        val owner = account.currentId()
        state.update { if (it.owner == owner) it else Snapshot(owner) }
        return state.value.takeIf { owner != null && owner == account.currentId() && it.owner == owner }?.lists?.get(target)
    }

    fun fresh(target: SavedCollectionTarget?): List<SavedCollectionSummary>? {
        val lists = peek(target) ?: return null
        return lists.takeIf { state.value.confirmedAt[target]?.elapsedNow()?.let { it < 1.days } == true }
    }

    fun peekFeatured(): SavedCollectionFeaturedSelection? {
        val owner = account.currentId()
        state.update { if (it.owner == owner) it else Snapshot(owner) }
        return state.value.takeIf { owner != null && owner == account.currentId() && it.owner == owner }?.featured
    }

    fun recordMemberships(owner: String, target: SavedCollectionTarget, lists: List<SavedCollectionSummary>) =
        record(owner, target, lists, verified = state.value.let { it.owner == owner && it.confirmedAt.containsKey(target) })

    fun record(owner: String, target: SavedCollectionTarget?, lists: List<SavedCollectionSummary>, verified: Boolean = true) {
        state.update { current ->
            if (account.currentId() != owner) return@update current
            val previous = current.lists.takeIf { current.owner == owner }.orEmpty()
            val merged = previous.mapValues { (key, old) -> lists.map { fresh ->
                fresh.copy(containsItem = when (key) {
                    target -> fresh.containsItem
                    null -> false
                    else -> old.firstOrNull { it.id == fresh.id }?.containsItem == true
                })
            } }
            val confirmed = current.confirmedAt.takeIf { current.owner == owner }.orEmpty()
            Snapshot(owner, merged + (target to lists.toList()), current.featured.takeIf { current.owner == owner },
                if (verified) confirmed + (target to timeSource.markNow()) else confirmed - target)
        }
    }

    fun upsert(owner: String, summary: SavedCollectionSummary) = change(owner,
        transform = { lists ->
            val updated = lists.map { if (it.id == summary.id) summary.copy(containsItem = it.containsItem) else it }
            if (updated.any { it.id == summary.id }) updated else listOf(summary.copy(containsItem = false)) + updated
        }, featured = { selection ->
            if (summary.visibility == SavedCollectionVisibility.Private && selection?.collectionId == summary.id)
                SavedCollectionFeaturedSelection(null) else selection
        })

    fun remove(owner: String, id: String) = change(owner,
        transform = { lists -> lists.filterNot { it.id == id } },
        featured = { selection -> if (selection?.collectionId == id) SavedCollectionFeaturedSelection(null) else selection })

    fun feature(owner: String, id: String?) {
        state.update { current ->
            if (account.currentId() != owner) current else {
                val lists = current.lists.takeIf { current.owner == owner }.orEmpty()
                Snapshot(owner, lists.mapValues { (_, values) -> values.map { it.copy(isFeatured = it.id == id) } },
                    SavedCollectionFeaturedSelection(id), current.confirmedAt.takeIf { current.owner == owner }.orEmpty())
            }
        }
    }

    fun recordFeatured(owner: String, id: String?) {
        state.update { current ->
            if (account.currentId() != owner) current else Snapshot(owner,
                current.lists.takeIf { current.owner == owner }.orEmpty(), SavedCollectionFeaturedSelection(id),
                current.confirmedAt.takeIf { current.owner == owner }.orEmpty())
        }
    }

    fun dropTargets(owner: String) {
        state.update { current ->
            if (current.owner != owner || account.currentId() != owner) current
            else current.copy(lists = current.lists.filterKeys { it == null }, confirmedAt = current.confirmedAt.filterKeys { it == null })
        }
    }

    private fun change(owner: String, transform: (List<SavedCollectionSummary>) -> List<SavedCollectionSummary>,
        featured: (SavedCollectionFeaturedSelection?) -> SavedCollectionFeaturedSelection? = { it }) {
        state.update { current ->
            if (current.owner != owner || account.currentId() != owner) current
            else current.copy(lists = current.lists.mapValues { transform(it.value) }, featured = featured(current.featured))
        }
    }
}
