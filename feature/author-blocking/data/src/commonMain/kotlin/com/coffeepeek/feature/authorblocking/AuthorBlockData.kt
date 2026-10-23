package com.coffeepeek.feature.authorblocking.data

import com.coffeepeek.feature.authorblocking.api.AuthorBlockingAccount
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.domain.AuthorBlockRepository
import io.ktor.client.HttpClient

/** One repository exposed through its domain contract and the cross-feature visibility contract. */
class AuthorBlockData internal constructor(
    val repository: AuthorBlockRepository,
    val visibility: AuthorVisibility,
    private val closeRepository: () -> Unit,
) : AutoCloseable {
    override fun close() = closeRepository()
}

fun createAuthorBlockData(client: HttpClient, account: AuthorBlockingAccount): AuthorBlockData {
    val repository = AuthorBlockRepositoryImpl(client, account)
    return AuthorBlockData(repository, repository, repository::close)
}
