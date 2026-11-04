package com.coffeepeek.feature.profile.impl

import com.coffeepeek.feature.profile.api.ProfileSnapshot
import com.coffeepeek.feature.profile.api.ProfileSnapshotStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Bounded process-memory snapshots; switching account scope or visibility revision discards the previous set. */
fun createProfileSnapshotStore(maxEntries: Int = 4): ProfileSnapshotStore = MemoryProfileSnapshotStore(maxEntries.coerceAtLeast(1))

private data class SnapshotKey(val slug: String, val revision: Long)
private data class SnapshotEntry(val key: SnapshotKey, val snapshot: ProfileSnapshot)
private data class SnapshotCache(val scope: String? = null, val entries: List<SnapshotEntry> = emptyList())

private class MemoryProfileSnapshotStore(private val limit: Int) : ProfileSnapshotStore {
    private val cache = MutableStateFlow(SnapshotCache())

    override fun read(scope: String, slug: String, visibilityRevision: Long): ProfileSnapshot? {
        val current = cache.value
        if (current.scope != scope) return null
        return current.entries.firstOrNull { it.key == SnapshotKey(slug, visibilityRevision) }?.snapshot
    }

    override fun write(scope: String, slug: String, visibilityRevision: Long, snapshot: ProfileSnapshot) {
        val key = SnapshotKey(slug, visibilityRevision)
        val copy = snapshot.copy(
            profile = snapshot.profile.copy(
                favorites = snapshot.profile.favorites.toList(),
                activity = snapshot.profile.activity?.let { it.copy(activeDays = it.activeDays.toSet(), checkInCounts = it.checkInCounts.toMap()) },
            ),
            checkIns = snapshot.checkIns.map { it.copy(photoUrls = it.photoUrls.toList(), photoPreviewUrls = it.photoPreviewUrls.toList()) },
        )
        cache.update { old ->
            val remaining = if (old.scope == scope) old.entries.filterNot { it.key == key } else emptyList()
            SnapshotCache(scope, (listOf(SnapshotEntry(key, copy)) + remaining).take(limit))
        }
    }

    override fun clear() { cache.value = SnapshotCache() }
}
