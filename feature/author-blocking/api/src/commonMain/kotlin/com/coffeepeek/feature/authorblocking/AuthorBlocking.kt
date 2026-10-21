package com.coffeepeek.feature.authorblocking.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Opaque public identity. Slug is only a fallback for resolving older content. */
data class AuthorReference(val id: String?, val slug: String?, val name: String, val avatarUrl: String? = null)

data class AuthorVisibilitySnapshot(
    val revision: Long = 0,
    val ready: Boolean = true,
    val blockedIds: Set<String> = emptySet(),
    val blockedSlugs: Set<String> = emptySet(),
) {
    fun hides(id: String?, slug: String?): Boolean = !ready ||
        (id != null && id in blockedIds) || (slug != null && slug in blockedSlugs)
}

/** Cross-feature read contract. A revision invalidates personalized pages and open photos. */
interface AuthorVisibility {
    val visibility: StateFlow<AuthorVisibilitySnapshot>
    suspend fun refresh(): Result<Unit>

    object None : AuthorVisibility {
        override val visibility = MutableStateFlow(AuthorVisibilitySnapshot())
        override suspend fun refresh() = Result.success(Unit)
    }
}

interface AuthorBlockActions {
    fun requestBlock(author: AuthorReference)
    fun requestUnblock(author: AuthorReference)
}
