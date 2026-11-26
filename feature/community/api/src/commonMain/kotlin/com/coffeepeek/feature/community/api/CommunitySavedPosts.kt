package com.coffeepeek.feature.community.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/** Confirmed public saves only. The feed reconciles these IDs with its next successful server read. */
data class SavedCommunityPost(val accountKey: String, val id: String)

interface CommunitySavedPosts {
    val posts: StateFlow<List<SavedCommunityPost>>
    /** Signals that a successful edit requires a fresh feed read for this account. */
    val updates: Flow<String>
    fun saved(accountKey: String, id: String)
    fun updated(accountKey: String)
    fun acknowledge(posts: List<SavedCommunityPost>)
    fun retainAccount(accountKey: String?)

    object None : CommunitySavedPosts {
        override val posts: StateFlow<List<SavedCommunityPost>> = MutableStateFlow(emptyList())
        override val updates: Flow<String> = emptyFlow()
        override fun saved(accountKey: String, id: String) = Unit
        override fun updated(accountKey: String) = Unit
        override fun acknowledge(posts: List<SavedCommunityPost>) = Unit
        override fun retainAccount(accountKey: String?) = Unit
    }
}
