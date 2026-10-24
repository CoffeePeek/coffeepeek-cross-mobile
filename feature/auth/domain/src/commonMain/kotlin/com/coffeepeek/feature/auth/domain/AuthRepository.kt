package com.coffeepeek.feature.auth.domain

/** Who is signed in after a successful sign-in; the token itself never leaves the data layer. */
data class SignedInAccount(val userId: String?)

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<SignedInAccount>
    suspend fun signInWithGoogle(idToken: String): Result<SignedInAccount>
    suspend fun beginAppleSignIn(): Result<AppleSignInChallenge>
    suspend fun signInWithApple(challengeId: String, idToken: String): Result<SignedInAccount>
    suspend fun register(userName: String, email: String, password: String): Result<Unit>
    suspend fun isEmailTaken(email: String): Result<Boolean>
    suspend fun requestPasswordReset(email: String): Result<Unit>

    /** Local cleanup first and authoritative; the server call is best effort. */
    suspend fun signOut(): Result<Unit>
}
