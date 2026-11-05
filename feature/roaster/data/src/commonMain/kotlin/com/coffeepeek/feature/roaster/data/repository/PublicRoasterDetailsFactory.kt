package com.coffeepeek.feature.roaster.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.roaster.api.PublicRoasterDetails
import com.coffeepeek.feature.roaster.api.PublicRoasterSummary
import com.coffeepeek.feature.roaster.api.PublicRoasterDescription
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository

fun createPublicRoasterDetails(repository: RoasterRepository): PublicRoasterDetails = object : PublicRoasterDetails {
    override suspend fun summaries() = requestResult {
        repository.getRoasters().getOrThrow().mapNotNull { row ->
            row.address?.slug?.let { PublicRoasterSummary(it, row.photoUrl, row.tags.map { tag -> tag.name },
                row.coffeeShopsCount, row.coffeeProductsCount, row.availableCoffeeProducts) }
        }
    }
    override suspend fun load(slug: String) = requestResult {
        val details = repository.getRoaster(slug).getOrThrow()
        PublicRoasterDescription(details.photos.firstOrNull()?.url, details.about)
    }
}
