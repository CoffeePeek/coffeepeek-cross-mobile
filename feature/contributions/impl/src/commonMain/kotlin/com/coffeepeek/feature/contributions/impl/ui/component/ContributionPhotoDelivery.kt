package com.coffeepeek.feature.contributions.impl.ui.component

import com.coffeepeek.feature.contributions.domain.model.PendingPhoto

/** A single composed attachment group; late/duplicate platform callbacks cannot refill a closed form. */
internal class ContributionPhotoDelivery(
    private val remaining: () -> Int,
    private val deliver: (List<PendingPhoto>) -> Unit,
) {
    private var active = true
    private var revision = 0L

    fun request(limit: Int): ((List<PendingPhoto>) -> Unit)? {
        if (!active || limit <= 0) return null
        val expected = ++revision
        return callback@{ photos ->
            if (!active || revision != expected) return@callback
            revision++
            val accepted = photos.take(minOf(limit, remaining()).coerceAtLeast(0))
                .map { PendingPhoto(it.fileName, it.contentType, it.bytes.copyOf()) }
            if (accepted.isNotEmpty()) deliver(accepted)
        }
    }

    fun dispose() { active = false; revision++ }
}
