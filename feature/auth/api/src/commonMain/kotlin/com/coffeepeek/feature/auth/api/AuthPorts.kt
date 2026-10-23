package com.coffeepeek.feature.auth.api

/**
 * Forgets everything the app stored for the account that was signed in: the stored session, the account's favorites and
 * the caches. The application owns that data, so it supplies this.
 */
interface LocalUserData {
    suspend fun clear()
}

enum class SignInMethod { Password, Google }

/** The screen the sign-in started from, as the analytics event reports it. */
enum class AuthEntry { Login, Register }

interface AuthAnalytics {
    /** A sign-in finished: identity sync, completion of a pending password sign-up and the `sign_in_completed` event. */
    fun signedIn(userId: String?, method: SignInMethod, entry: AuthEntry, email: String? = null)

    /** The optional analytics choice of the registration form; stored before the account is created and never blocks it. */
    suspend fun saveAnalyticsConsent(accepted: Boolean)

    /** The account was created with a password: remembered so the first sign-in with this e-mail completes the sign-up. */
    fun passwordSignUpCompleted(email: String)
}
