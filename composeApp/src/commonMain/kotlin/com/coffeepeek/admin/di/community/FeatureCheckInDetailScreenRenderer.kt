package com.coffeepeek.admin.di.community

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.feature.community.ui.CheckInDetailScreenRenderer
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityPostEntry
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.impl.createCommunityPostEntry

/** Opens shared post links in the migrated entry, with the feed's images, dates and photo viewer. */
internal class FeatureCheckInDetailScreenRenderer(private val entry: CommunityPostEntry) : CheckInDetailScreenRenderer {
    @Composable
    override fun Content(id: String) {
        entry.Content(
            postId = id,
            onBack = Navigator::popBack,
            onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
            onOpenAuthor = { slug, name, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(slug, name, avatar)) },
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            modifier = androidx.compose.ui.Modifier,
        )
    }
}

internal fun featureCheckInDetailScreenRenderer(
    repository: CommunityRepository, account: CommunityAccount, visibility: AuthorVisibility, blockActions: AuthorBlockActions,
): CheckInDetailScreenRenderer = FeatureCheckInDetailScreenRenderer(
    createCommunityPostEntry(repository, account, visibility, blockActions, communityHost()),
)
