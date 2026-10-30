package com.coffeepeek.admin.di.community

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import com.coffeepeek.admin.analytics.TrackFeedViewed
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.admin.feature.community.ui.CommunityScreenRenderer
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CpSnackbarHost
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewer
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.admin.ui.component.rememberReviewDisplayDate
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateHost
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
import com.coffeepeek.core.media.PrefetchListPhotos
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.feature.community.api.CommunityEntry
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.api.ExposureKind
import com.coffeepeek.feature.community.impl.createCommunityEntry
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityAnalytics
import com.coffeepeek.feature.community.api.ConsumedDrinks
import com.coffeepeek.feature.community.domain.repository.CommunityRepository

/** Both platforms use one migrated feed; navigation, protected media, analytics and the global creation flow remain app-owned. */
internal class FeatureCommunityScreenRenderer(private val entry: CommunityEntry) : CommunityScreenRenderer {
    @Composable
    override fun Content(onCreateCheckIn: () -> Unit, notificationsAction: @Composable () -> Unit) {
        entry.Content(
            onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
            onOpenAuthor = { slug, name, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(slug, name, avatar)) },
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            onOpenCoffee = { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) },
            onEditHome = { Navigator.navigate(Navigator.Screen.EditHomeCheckIn(it)) },
            onOpenPost = { Navigator.navigate(Navigator.Screen.CheckInDetail(it)) },
            modifier = Modifier,
            headerAction = notificationsAction,
            bottomClearance = LocalFloatingNavClearance.current,
            floatingAction = { floating ->
                CheckInCreateHost(
                    canCreate = { floating.isLoggedIn == true }, resetKey = floating.sessionGeneration,
                    onShop = onCreateCheckIn, onHomeSaved = floating.refresh,
                )
            },
        )
    }
}

internal fun communityHost() = CommunityHost(
    image = { url, highQuality, modifier, scale -> CoffeePeekImage(data = url, highQualityData = highQuality, contentScale = scale, modifier = modifier) },
    displayDate = { rememberReviewDisplayDate(it) },
    brewMethodLabel = { it.methodLabel() },
    prefetchPhotos = { state, posts -> PrefetchListPhotos(state, posts) },
    photoViewer = { thumbnails, originals, index, onDismiss -> CoffeePeekPhotoViewer(CoffeePeekPhotoViewerImages(thumbnails, originals), index, onDismiss) },
    exposure = { modifier, kind ->
        when (kind) {
            ExposureKind.Feed -> modifier.analyticsExposure("feed", "feed", "feed")
            ExposureKind.ShopPost -> modifier.analyticsExposure("feed_shop_check_in", "feed", "feed")
            ExposureKind.HomePost -> modifier.analyticsExposure("feed_home_check_in", "feed", "feed")
        }
    },
    feedViewed = { count, isPublic -> TrackFeedViewed(count, isPublic) },
    snackbar = { state, clearance -> CpSnackbarHost(state, Modifier.padding(bottom = clearance)) },
    share = ShareHelper::shareText,
)

internal fun featureCommunityScreenRenderer(
    repository: CommunityRepository, account: CommunityAccount, analytics: CommunityAnalytics, drinks: ConsumedDrinks,
    visibility: AuthorVisibility, blockActions: AuthorBlockActions,
): CommunityScreenRenderer = FeatureCommunityScreenRenderer(
    createCommunityEntry(repository, account, analytics, drinks, visibility, blockActions, communityHost()),
)
