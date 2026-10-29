package com.coffeepeek.feature.community.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.api.CommunityPostEntry
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.impl.post.PostViewModel
import com.coffeepeek.feature.community.impl.ui.PostCallbacks
import com.coffeepeek.feature.community.impl.ui.PostScreen

/** A shared post. The ViewModel lives in the destination's store, one per post. */
fun createCommunityPostEntry(
    repository: CommunityRepository,
    account: CommunityAccount,
    visibility: AuthorVisibility,
    blockActions: AuthorBlockActions,
    host: CommunityHost,
): CommunityPostEntry = object : CommunityPostEntry {
    @Composable
    override fun Content(
        postId: String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenAuthor: (String, String, String?) -> Unit,
        onOpenShop: (String) -> Unit,
        onOpenCoffee: (String) -> Unit,
        modifier: Modifier,
    ) {
        PostScreen(
            viewModel(key = "community-post:$postId") { PostViewModel(postId, repository, account, visibility) },
            host, blockActions, PostCallbacks(onBack, onSignIn, onOpenAuthor, onOpenShop, onOpenCoffee), modifier,
        )
    }
}
