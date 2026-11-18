package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.di.checkinhistory.AppCheckInHistoryCalendar
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.di.shelf.iosShelfFactory
import com.coffeepeek.admin.di.shelf.nativeShelfController
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.session.OldReviewDrafts
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.impl.createIosOwnProfileHistorySession
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileSnapshotStore
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.domain.PublicProfileData
import com.coffeepeek.feature.profile.impl.createIosOwnProfileSession
import com.coffeepeek.feature.settings.api.LanguagePort
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosOwnProfileController = staticCompositionLocalOf<(IosOwnProfile) -> UIViewController> {
    error("The iOS app must provide an own profile controller")
}

@Composable
internal fun iosOwnProfileContent(): () -> UIViewController {
    val factory = LocalIosOwnProfileController.current
    val shelf = iosShelfFactory()
    val accounts = koinInject<ProfileAccountPort>()
    val notifications = koinInject<NotificationFeature>()
    val auth = koinInject<AuthRepository>()
    val drafts = koinInject<OldReviewDrafts>()
    val repository = koinInject<PublicProfileData>()
    val checkIns = koinInject<PublicCheckInsPort>()
    val visibility = koinInject<AuthorVisibility>()
    val snapshots = koinInject<ProfileSnapshotStore>()
    val sessions = koinInject<SessionReader>()
    val shops = koinInject<CheckInHistoryRepository>()
    val homes = koinInject<HomeHistoryReader>()
    val calendar = koinInject<AppCheckInHistoryCalendar>()
    val language = koinInject<LanguagePort>()
    val imageFetcher = koinInject<ImageUrlFetcher>()
    val media = remember(imageFetcher) { IosCatalogueMedia(imageFetcher, Constants.BASE_URL) }
    return {
        val lease = CheckInHistorySession(sessions)
        val profile = createIosOwnProfileSession(accounts, AppProfileSignOut(notifications, auth, drafts),
            repository, checkIns, AppProfileAuthorVisibility(visibility), snapshots, ::profileLifecycleDiagnostic)
        factory(IosOwnProfile(profile, lease,
            createIosOwnProfileHistorySession(lease.bind(shops), lease.bind(homes), calendar::snapshot), language, media,
            { allowed -> nativeShelfController(shelf("shelf", null, true, allowed)) }))
    }
}

private fun profileLifecycleDiagnostic(message: String) {
    if (AppConfig.isDebug) println("OwnProfileLifecycle $message")
}
