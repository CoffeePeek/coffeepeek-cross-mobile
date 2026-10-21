package com.coffeepeek.feature.auth.api

/**
 * Forgets everything the app stored for the account that was signed in: the stored session, the account's favorites and
 * the caches. The application owns that data, so it supplies this.
 */
interface LocalUserData {
    suspend fun clear()
}
