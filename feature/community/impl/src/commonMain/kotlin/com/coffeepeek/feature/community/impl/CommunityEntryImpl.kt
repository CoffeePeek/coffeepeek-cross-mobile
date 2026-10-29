package com.coffeepeek.feature.community.impl

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityAnalytics
import com.coffeepeek.feature.community.api.CommunityEntry
import com.coffeepeek.feature.community.api.CommunityFloating
import com.coffeepeek.feature.community.api.ConsumedDrinks
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.impl.timeline.CommunityViewModel
import com.coffeepeek.feature.community.impl.ui.CommunityCallbacks
import com.coffeepeek.feature.community.impl.ui.CommunityScreen

/** «Лента». The ViewModel lives in the destination's store. */
fun createCommunityEntry(
    repository: CommunityRepository,
    account: CommunityAccount,
    analytics: CommunityAnalytics,
    drinks: ConsumedDrinks,
    visibility: AuthorVisibility,
    blockActions: AuthorBlockActions,
    host: CommunityHost,
): CommunityEntry = object : CommunityEntry {
    @Composable
    override fun Content(
        onSignIn: () -> Unit,
        onOpenAuthor: (String, String, String?) -> Unit,
        onOpenShop: (String) -> Unit,
        onOpenCoffee: (String) -> Unit,
        onEditHome: (String) -> Unit,
        onOpenPost: (String) -> Unit,
        modifier: Modifier,
        headerAction: @Composable () -> Unit,
        bottomClearance: Dp,
        floatingAction: @Composable BoxScope.(CommunityFloating) -> Unit,
    ) {
        CommunityScreen(
            viewModel(key = "community-feed") { CommunityViewModel(repository, account, analytics, drinks, visibility) },
            host, blockActions, CommunityCallbacks(onSignIn, onOpenAuthor, onOpenShop, onEditHome, onOpenPost, onOpenCoffee), modifier, bottomClearance, headerAction, floatingAction,
        )
    }
}
