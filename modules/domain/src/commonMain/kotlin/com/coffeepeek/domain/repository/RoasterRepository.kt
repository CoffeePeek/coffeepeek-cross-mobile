package com.coffeepeek.domain.repository

import com.coffeepeek.domain.model.CreateRoasterInput
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.model.RoasterSummary
import com.coffeepeek.domain.model.RoasterSubmission
import com.coffeepeek.domain.model.RoasterSubmissionResult

interface RoasterRepository {
    suspend fun getRoasters(): Result<List<RoasterSummary>>

    /** An explicit refresh asks the server even when the catalog is loaded or fresh; a failure keeps the loaded data. */
    suspend fun refreshRoasters(): Result<List<RoasterSummary>> = getRoasters()
    suspend fun getRoaster(id: String): Result<RoasterDetails>
    suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult>
    suspend fun getMyRoasterSubmissions(
        status: ModerationStatus,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<RoasterSubmission>>
}
