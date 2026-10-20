package com.coffeepeek.admin.feature.profile.domain

import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.feature.profile.domain.PublicProfileData

internal data class PublicProfileCheckIn(
    val id: String,
    val coffeeShopSlug: String?,
    val venue: String,
    val title: String,
    val details: String?,
    val rating: Double?,
    val note: String,
    val occurredAt: String,
    val photoUrls: List<String>,
    val source: CheckIn? = null,
)

internal data class PublicProfileCheckInPage(
    val items: List<PublicProfileCheckIn>,
    val nextCursor: String?,
)

/** Profile data from the profile data module plus the author's check-ins, which still use the legacy timeline model. */
internal interface PublicProfileRepository : PublicProfileData {
    suspend fun getCheckIns(slug: String, cursor: String? = null): Result<PublicProfileCheckInPage>
}
