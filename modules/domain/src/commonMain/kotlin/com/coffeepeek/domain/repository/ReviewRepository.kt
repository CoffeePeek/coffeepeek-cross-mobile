package com.coffeepeek.domain.repository

import com.coffeepeek.domain.model.CreateReviewInput
import com.coffeepeek.domain.model.HelpfulVote
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.Review
import com.coffeepeek.domain.model.ReviewSubmission
import com.coffeepeek.domain.model.UpdateReviewInput

interface ReviewRepository {
    suspend fun submitReviewReport(reviewId: String, text: String): Result<String>
    suspend fun createReview(input: CreateReviewInput): Result<Unit>
    suspend fun updateReview(reviewId: String, input: UpdateReviewInput): Result<Unit>
    suspend fun getUserReviews(userId: String, page: Int, pageSize: Int): Result<PagedResult<Review>>
    suspend fun getMyReviewSubmissions(
        status: ModerationStatus,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<ReviewSubmission>>
    suspend fun setReviewHelpful(reviewId: String, helpful: Boolean): Result<HelpfulVote>
}
