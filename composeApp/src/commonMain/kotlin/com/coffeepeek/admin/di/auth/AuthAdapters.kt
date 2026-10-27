package com.coffeepeek.admin.di.auth

import com.coffeepeek.data.session.UserSessionCleaner
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.analytics.signInAnalytics
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.AuthEntry
import com.coffeepeek.feature.auth.api.LocalUserData
import com.coffeepeek.feature.auth.api.SignInMethod

/** The application's cleanup of an account's local data: the stored session, its favorites and the caches. */
class UserSessionLocalUserData(private val cleaner: UserSessionCleaner) : LocalUserData {
    override suspend fun clear() = cleaner.clearLocalUserData()
}

/** The analytics side of sign-in: the same events, properties and consent handling the legacy view models used. */
class AppAuthAnalytics : AuthAnalytics {
    override fun signedIn(userId: String?, method: SignInMethod, entry: AuthEntry, email: String?) {
        signInAnalytics(
            userId,
            method = when (method) {
                SignInMethod.Password -> "password"
                SignInMethod.Google -> "google"
            },
            entry = when (entry) {
                AuthEntry.Login -> "login"
                AuthEntry.Register -> "register"
            },
            email = email,
        )
    }

    override suspend fun saveAnalyticsConsent(accepted: Boolean) {
        Analytics.service?.setConsent(if (accepted) AnalyticsConsent.Accepted else AnalyticsConsent.Declined)
    }

    override fun passwordSignUpCompleted(email: String) {
        Analytics.service?.rememberPasswordSignUp(email)
    }
}
