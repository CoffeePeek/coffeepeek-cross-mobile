package com.coffeepeek.admin.di.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxScope
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.TrackFeedViewed
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.admin.feature.community.ui.CommunityScreenRenderer
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CpSnackbarHost
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.ui.component.rememberReviewDisplayDate
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateButton
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlow
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.PrefetchListPhotos
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.feature.community.api.CommunityEntry
import com.coffeepeek.feature.community.api.CommunityFloating
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.api.ExposureKind
import com.coffeepeek.feature.community.impl.createCommunityEntry
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityAnalytics
import com.coffeepeek.feature.community.api.ConsumedDrinks
import com.coffeepeek.feature.community.domain.repository.CommunityRepository

/** Android routes the feed tab to the migrated entry; navigation, images, the photo viewer, analytics trackers and the creation flow stay app-owned. */
internal class AndroidCommunityScreenRenderer(private val entry: CommunityEntry) : CommunityScreenRenderer {
    @Composable
    override fun Content(onCreateCheckIn: () -> Unit, notificationsAction: @Composable () -> Unit) {
        entry.Content(
            onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
            onOpenAuthor = { slug, name, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(slug, name, avatar)) },
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            onEditHome = { Navigator.navigate(Navigator.Screen.EditHomeCheckIn(it)) },
            onOpenPost = { Navigator.navigate(Navigator.Screen.CheckInDetail(it)) },
            headerAction = notificationsAction,
            bottomClearance = LocalFloatingNavClearance.current,
            floatingAction = { floating -> CreateAction(floating, onCreateCheckIn) },
        )
    }
}

/** The legacy create button: a guest is sent to sign in, a signed-in user gets the global creation flow, which closes with the account scope. */
@Composable
private fun BoxScope.CreateAction(floating: CommunityFloating, onCreateCheckIn: () -> Unit) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(floating.sessionGeneration) { showCreate = false }
    if (showCreate) CheckInCreateFlow(
        onDismiss = { showCreate = false },
        onShop = { showCreate = false; onCreateCheckIn() },
        onHomeSaved = floating.refresh,
    )
    CheckInCreateButton(Modifier.align(Alignment.BottomEnd).padding(end = CpDimens.spacing4, bottom = LocalFloatingNavClearance.current + 12.dp)) {
        Analytics.track(AnalyticsEvent.CHECK_IN_START_ATTEMPTED, "source" to "global_create")
        if (floating.isLoggedIn == true) showCreate = true
        else { Analytics.gate("check_in"); Navigator.navigate(Navigator.Screen.Auth) }
    }
}

internal fun communityHost() = CommunityHost(
    image = { url, highQuality, modifier, scale -> CpImage(data = url, highQualityData = highQuality, contentScale = scale, modifier = modifier) },
    displayDate = { rememberReviewDisplayDate(it) },
    brewMethodLabel = { it.methodLabel() },
    prefetchPhotos = { state, posts -> PrefetchListPhotos(state, posts) },
    photoViewer = { thumbnails, originals, index, onDismiss -> FullScreenImageDialog(PhotoViewerImages(thumbnails, originals), index, onDismiss) },
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

internal fun androidCommunityScreenRenderer(
    repository: CommunityRepository, account: CommunityAccount, analytics: CommunityAnalytics, drinks: ConsumedDrinks,
    visibility: AuthorVisibility, blockActions: AuthorBlockActions,
): CommunityScreenRenderer = AndroidCommunityScreenRenderer(
    createCommunityEntry(repository, account, analytics, drinks, visibility, blockActions, communityHost()),
)
