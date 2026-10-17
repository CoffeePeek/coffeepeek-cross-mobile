package com.coffeepeek.feature.homecheckin.impl.ui.data

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload

/** UI-thread callback gate for a single composed form, not a global picker/session. */
internal class HomeCheckInPhotoDelivery(
    private val canAccept: () -> Boolean,
    private val deliver: (List<HomePreparationUpload>) -> Unit,
) {
    private var active = true
    private var revision = 0L

    fun request(remaining: Int): ((List<HomePreparationUpload>) -> Unit)? {
        if (!active || !canAccept() || remaining <= 0) return null
        val expected = ++revision
        val limit = remaining.coerceAtMost(5)
        return callback@{ photos ->
            if (!active || revision != expected) return@callback
            revision++ // A launcher must not deliver its callback twice.
            if (!canAccept()) return@callback
            val owned = photos.take(limit).map { it.copy(bytes = it.bytes.copyOf()) }
            if (owned.isNotEmpty()) deliver(owned)
        }
    }

    fun dispose() { active = false; revision++ }
}
