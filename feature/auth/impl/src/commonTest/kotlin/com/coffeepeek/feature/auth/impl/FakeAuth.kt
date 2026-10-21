package com.coffeepeek.feature.auth.impl

import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.AuthEntry
import com.coffeepeek.feature.auth.api.SignInMethod
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.SignedInAccount
import kotlinx.coroutines.CompletableDeferred

internal class FakeAuthRepository : AuthRepository {
    val signIns = mutableListOf<Pair<String, String>>()
    val resets = mutableListOf<String>()
    var signIn: Result<SignedInAccount> = Result.success(SignedInAccount("user-1"))
    var reset: Result<Unit> = Result.success(Unit)
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun signIn(email: String, password: String): Result<SignedInAccount> {
        signIns += email to password
        gate?.await()
        return signIn
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        resets += email
        gate?.await()
        return reset
    }

    override suspend fun signInWithGoogle(idToken: String): Result<SignedInAccount> = error("Unused")
    override suspend fun register(userName: String, email: String, password: String): Result<Unit> = error("Unused")
    override suspend fun isEmailTaken(email: String): Result<Boolean> = error("Unused")
    override suspend fun signOut(): Result<Unit> = error("Unused")
}

internal class FakeAnalytics : AuthAnalytics {
    val signedIn = mutableListOf<List<Any?>>()
    override fun signedIn(userId: String?, method: SignInMethod, entry: AuthEntry, email: String?) {
        signedIn += listOf(userId, method, entry, email)
    }
}
