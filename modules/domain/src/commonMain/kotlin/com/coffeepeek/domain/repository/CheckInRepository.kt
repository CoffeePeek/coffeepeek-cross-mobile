package com.coffeepeek.domain.repository

import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CreateCheckInInput
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.UpdateCheckInInput
import com.coffeepeek.domain.model.CheckInVisibility

interface CheckInRepository {
    suspend fun createCheckIn(input: CreateCheckInInput): Result<Unit>
    suspend fun getMyCheckIns(page: Int, pageSize: Int): Result<PagedResult<CheckIn>>
    suspend fun getMyCheckIns(from: String, to: String, pageSize: Int): Result<List<CheckIn>>
    suspend fun updateCheckIn(id: String, input: UpdateCheckInInput): Result<CheckIn>
    suspend fun setVisibility(id: String, visibility: CheckInVisibility): Result<CheckIn>
    suspend fun setHelpful(id: String, helpful: Boolean): Result<CheckInHelpfulVote>
    suspend fun report(id: String, text: String): Result<Unit>
}

data class CheckInHelpfulVote(val isHelpful: Boolean, val helpfulCount: Int)
