package com.coffeepeek.feature.authorblocking.domain

import kotlinx.coroutines.flow.StateFlow

internal data class BlockedAuthor(val id: String, val name: String, val slug: String?, val avatarUrl: String?)
internal data class AuthorBlocksState(
    val items: List<BlockedAuthor> = emptyList(),
    val signedIn: Boolean = false,
    val ready: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val sessionVersion: Long = 0,
)

internal interface AuthorBlockRepository {
    val state: StateFlow<AuthorBlocksState>
    suspend fun refresh(): Result<Unit>
    suspend fun setBlocked(id: String?, slug: String?, name: String, avatarUrl: String?, blocked: Boolean): Result<Unit>
}
