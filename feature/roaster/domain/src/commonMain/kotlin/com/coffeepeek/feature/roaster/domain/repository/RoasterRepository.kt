package com.coffeepeek.feature.roaster.domain.repository

import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.domain.model.RoasterSummary

/** Read side only; roaster submissions belong to Contributions. */
interface RoasterRepository {
    suspend fun getRoasters(): Result<List<RoasterSummary>>
    suspend fun getRoaster(slug: String): Result<RoasterDetails>
}
