package com.coffeepeek.feature.auth.data

import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.auth.api.LocalUserData
import com.coffeepeek.feature.auth.domain.AuthRepository
import io.ktor.client.HttpClient

/** Composition-only entry. [plain] carries no credentials; native logout uses its JSON refresh token. */
fun createAuthRepository(
    plain: HttpClient,
    sessions: SessionStore,
    localUserData: LocalUserData,
): AuthRepository = AuthRepositoryImpl(plain, sessions, localUserData)
