package com.coffeepeek.feature.community.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityAnalytics
import com.coffeepeek.feature.community.api.ConsumedDrinks
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.impl.post.PostAction
import com.coffeepeek.feature.community.impl.post.PostViewModel

/** Uses the existing post model and its mutation/draft guards for every native resume. */
internal class NativeCommunityPostSession(id: String, repository: CommunityRepository,
    account: CommunityAccount, authors: AuthorVisibility, analytics: CommunityAnalytics, drinks: ConsumedDrinks,
) {
    val model = PostViewModel(id, repository, account, authors, analytics, drinks)
    private val store = ViewModelStore().apply { put("native-community-post", model) }
    private var closed = false

    fun perform(action: PostAction) {
        if (!closed) model.onAction(action)
    }

    fun resumed() = perform(PostAction.Reload)

    fun close() {
        if (closed) return
        closed = true
        store.clear()
    }
}
