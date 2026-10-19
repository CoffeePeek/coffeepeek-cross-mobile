package com.coffeepeek.feature.authorblocking.api

import kotlinx.coroutines.flow.StateFlow

/** Presentation-only entry point for the temporary application-owned Cp UI renderer. */
interface AuthorBlockingPresenter : AuthorBlockActions, AutoCloseable {
    val blocks: StateFlow<AuthorBlockingListState>
    val prompt: StateFlow<AuthorBlockPrompt?>
    val needsLogin: StateFlow<Boolean>
    fun dismiss()
    fun loginHandled()
    fun refresh()
    fun confirm()
}

data class BlockedAuthorItem(val id: String, val name: String, val slug: String?, val avatarUrl: String?)

data class AuthorBlockingListState(
    val items: List<BlockedAuthorItem> = emptyList(),
    val signedIn: Boolean = false,
    val ready: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

data class AuthorBlockPrompt(val author: AuthorReference, val blocked: Boolean,
    val working: Boolean = false, val error: String? = null)
