package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Full rows for the most recently updated profiles: bounded ownership, refresh replacement and atomic concurrent pages on both platforms. */
internal class RememberedCheckIns(private val maxProfiles: Int = 4) {
    private data class ProfileRows(val slug: String, val rows: Map<String, PublicProfileCheckIn>)
    // List equality includes recency. Map equality would discard a reorder when the rows did not change.
    private val byProfile = MutableStateFlow<List<ProfileRows>>(emptyList())

    fun remember(slug: String, restart: Boolean, items: List<PublicProfileCheckIn>) {
        byProfile.update { current ->
            val rows = if (restart) emptyMap() else current.firstOrNull { it.slug == slug }?.rows.orEmpty()
            val profile = ProfileRows(slug, rows + items.associateBy { it.id })
            (current.filterNot { it.slug == slug } + profile).takeLast(maxProfiles.coerceAtLeast(0))
        }
    }

    fun find(id: String): PublicProfileCheckIn? = byProfile.value.firstNotNullOfOrNull { it.rows[id] }
}
