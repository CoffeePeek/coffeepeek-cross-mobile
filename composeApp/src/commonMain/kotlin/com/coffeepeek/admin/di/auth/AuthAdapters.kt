package com.coffeepeek.admin.di.auth

import com.coffeepeek.data.session.UserSessionCleaner
import com.coffeepeek.feature.auth.api.LocalUserData

/** The application's cleanup of an account's local data: the stored session, its favorites and the caches. */
class UserSessionLocalUserData(private val cleaner: UserSessionCleaner) : LocalUserData {
    override suspend fun clear() = cleaner.clearLocalUserData()
}
