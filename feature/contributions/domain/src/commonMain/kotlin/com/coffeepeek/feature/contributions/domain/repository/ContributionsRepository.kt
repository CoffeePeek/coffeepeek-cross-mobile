package com.coffeepeek.feature.contributions.domain.repository

import com.coffeepeek.feature.contributions.domain.model.ChangeDraft
import com.coffeepeek.feature.contributions.domain.model.ChangeRequest
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.domain.model.NewRoaster
import com.coffeepeek.feature.contributions.domain.model.NewShop
import com.coffeepeek.feature.contributions.domain.model.Paged
import com.coffeepeek.feature.contributions.domain.model.RoasterSubmission
import com.coffeepeek.feature.contributions.domain.model.RoasterSubmitted
import com.coffeepeek.feature.contributions.domain.model.ShopSubmission

/** Everything the signed-in user sends for moderation, and what became of it. Failures are [com.coffeepeek.feature.contributions.domain.model.ContributionFailure]. */
interface ContributionsRepository {
    suspend fun shops(status: ModerationStatus, page: Int, pageSize: Int): Result<Paged<ShopSubmission>>
    suspend fun roasters(status: ModerationStatus, page: Int, pageSize: Int): Result<Paged<RoasterSubmission>>
    suspend fun changes(
        status: ModerationStatus?,
        page: Int,
        pageSize: Int,
        shopSlug: String? = null,
        section: ChangeSection? = null,
    ): Result<Paged<ChangeRequest>>

    suspend fun change(id: String): Result<ChangeRequest>

    /** Uploads the photos, then sends the shop once. Nothing is sent when an upload fails. */
    suspend fun submitShop(shop: NewShop): Result<Unit>
    suspend fun submitRoaster(roaster: NewRoaster): Result<RoasterSubmitted>

    /** Sends a new request; new photos are uploaded first. */
    suspend fun submitChange(draft: ChangeDraft): Result<ChangeRequest>

    /** Replaces the section and payload of an own request that is not approved yet. */
    suspend fun editChange(id: String, draft: ChangeDraft): Result<ChangeRequest>
}
