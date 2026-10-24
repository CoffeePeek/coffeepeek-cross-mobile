package com.coffeepeek.feature.roaster.domain.repository

import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.domain.model.RoasterSummary

/** Read side only; roaster submissions belong to Contributions. */
interface RoasterRepository {
    suspend fun getRoasters(): Result<List<RoasterSummary>>

    /**
     * An explicit refresh (pull-to-refresh, retry): asks the server even when the list is already loaded or saved data is still fresh.
     * A failed refresh keeps the data that is already there. The default is the normal read, for repositories without a cache.
     */
    suspend fun refreshRoasters(): Result<List<RoasterSummary>> = getRoasters()
    suspend fun getRoaster(slug: String): Result<RoasterDetails>
}
