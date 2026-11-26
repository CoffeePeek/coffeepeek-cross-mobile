package com.coffeepeek.admin.di.community

/** A destination releases only its own registration after an account/root replacement. */
internal class CommunityFeedRefresh {
    private var current: (() -> Unit)? = null
    fun register(refresh: () -> Unit): () -> Unit {
        current = refresh
        return { if (current === refresh) current = null }
    }
    fun refresh() { current?.invoke() }
}
