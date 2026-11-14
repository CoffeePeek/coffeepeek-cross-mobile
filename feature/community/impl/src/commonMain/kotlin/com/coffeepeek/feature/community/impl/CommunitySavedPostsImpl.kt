package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.api.CommunitySavedPosts
import com.coffeepeek.feature.community.api.SavedCommunityPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

fun createCommunitySavedPosts(): CommunitySavedPosts = InMemorySavedPosts()

private class InMemorySavedPosts : CommunitySavedPosts {
    private val pending = MutableStateFlow<List<SavedCommunityPost>>(emptyList())
    override val posts = pending.asStateFlow()
    override fun saved(accountKey: String, id: String) {
        if (accountKey.isBlank() || id.isBlank()) return
        val saved = SavedCommunityPost(accountKey, id)
        pending.update { (it.filter { post -> post.accountKey == accountKey && post.id != id } + saved).takeLast(20) }
    }
    override fun acknowledge(posts: List<SavedCommunityPost>) = pending.update { it - posts.toSet() }
    override fun retainAccount(accountKey: String?) = pending.update { it.filter { post -> post.accountKey == accountKey } }
}
