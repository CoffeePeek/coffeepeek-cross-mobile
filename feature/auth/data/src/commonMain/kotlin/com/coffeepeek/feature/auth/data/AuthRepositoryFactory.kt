package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.auth.api.LocalUserData
import com.coffeepeek.feature.auth.domain.AuthRepository
import io.ktor.client.HttpClient

/** Composition-only entry. [plain] carries no credentials, [authenticated] attaches the access token. */
fun createAuthRepository(
    plain: HttpClient,
    authenticated: HttpClient,
    sessions: SessionStore,
    localUserData: LocalUserData,
): AuthRepository = AuthRepositoryImpl(plain, authenticated, sessions, localUserData)
