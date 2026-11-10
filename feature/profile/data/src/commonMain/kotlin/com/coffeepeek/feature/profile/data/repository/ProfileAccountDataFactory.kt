package com.coffeepeek.feature.profile.data.repository

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.profile.api.AccountDeletionPort
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.data.backend.OwnProfileBackend
import com.coffeepeek.feature.profile.data.backend.ProfileAvatarUploadBackend
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope

/** Three existing contracts share one owner-scoped cache; composition supplies their common lifetime. */
class ProfileAccountData(val accounts: ProfileAccountPort, val edit: ProfileEditPort, val deletion: AccountDeletionPort)

fun createProfileAccountData(client: HttpClient, uploadClient: HttpClient, sessions: SessionReader, scope: CoroutineScope): ProfileAccountData {
    require(client !== uploadClient) { "Avatar uploads require a separate credential-free client" }
    val repository = ProfileAccountRepository(OwnProfileBackend(client), ProfileAvatarUploadBackend(client, uploadClient), sessions, scope)
    return ProfileAccountData(repository, repository, repository)
}
