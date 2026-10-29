package com.coffeepeek.feature.roaster.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalog
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalogEntry
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository

fun createPublicRoasterCatalog(repository: RoasterRepository): PublicRoasterCatalog = PublicRoasterCatalog {
    requestResult {
        repository.getRoasters().getOrThrow().mapNotNull { row ->
            row.address?.slug?.takeIf(String::isNotBlank)?.let { PublicRoasterCatalogEntry(it, row.name, row.photoUrl) }
        }
    }
}
