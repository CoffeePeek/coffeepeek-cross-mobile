package com.coffeepeek.feature.ownership.domain.repository

import com.coffeepeek.feature.ownership.domain.model.*

interface BusinessOwnershipRepository {
    suspend fun claims(page: Int): Result<BusinessPage<OwnershipClaim>>
    suspend fun claim(id: String): Result<OwnershipClaim>
    suspend fun submit(input: ClaimSubmission): Result<OwnershipClaim>
    suspend fun businesses(kind: BusinessKind, page: Int): Result<BusinessPage<OwnedBusiness>>
    suspend fun profile(kind: BusinessKind, id: String): Result<BusinessProfile>
    suspend fun save(original: BusinessProfile, draft: ProfileDraft): Result<BusinessProfile>
}
