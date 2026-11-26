package com.coffeepeek.feature.community.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityAnalytics
import com.coffeepeek.feature.community.api.CommunitySavedPosts
import com.coffeepeek.feature.community.api.ConsumedDrinks
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.impl.timeline.CommunityAction
import com.coffeepeek.feature.community.impl.timeline.CommunityViewModel

/** A controller-owned business lifetime; temporary native sheets do not clear its model. */
internal class NativeCommunityFeedSession(repository: CommunityRepository, account: CommunityAccount,
    analytics: CommunityAnalytics, drinks: ConsumedDrinks, authors: AuthorVisibility,
    saved: CommunitySavedPosts = CommunitySavedPosts.None,
) {
    val model = CommunityViewModel(repository, account, analytics, drinks, authors, saved)
    private val store = ViewModelStore().apply { put("native-community-feed", model) }
    private var closed = false
    private var homeEditorOpened = false

    fun perform(action: CommunityAction) {
        if (!closed) model.onAction(action)
    }

    fun homeEditorOpened() {
        if (!closed) homeEditorOpened = true
    }

    fun resumed() {
        if (closed || !homeEditorOpened) return
        homeEditorOpened = false
        perform(CommunityAction.Refresh())
    }

    fun close() {
        if (closed) return
        closed = true
        homeEditorOpened = false
        store.clear()
    }
}
