package com.coffeepeek.feature.profile.api

/** Server-confirmed reaction to a public check-in. */
data class ProfileCheckInLike(val isHelpful: Boolean, val helpfulCount: Int)

/** Composition adapts the shared check-in writer without exposing another feature's implementation. */
fun interface ProfileCheckInLikes {
    suspend fun setHelpful(id: String, helpful: Boolean): Result<ProfileCheckInLike>
}
