package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.di.collections.iosCollectionsWorkspaceContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.shelf.iosShelfFactory
import com.coffeepeek.admin.di.shelf.IosShelfFactory
import com.coffeepeek.admin.di.shelf.nativeShelfController
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileSnapshotStore
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.domain.PublicProfileData
import com.coffeepeek.feature.profile.impl.createIosPublicProfileSession
import com.coffeepeek.feature.settings.api.LanguagePort
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosPublicProfileController = staticCompositionLocalOf<(IosPublicProfile) -> UIViewController> {
    error("The iOS app must provide a public profile controller")
}

@Composable
internal fun iosPublicProfileContent(): (String, String?, String?, Boolean) -> UIViewController {
    val factory = LocalIosPublicProfileController.current
    val shelf = iosShelfFactory()
    val accounts = koinInject<ProfileAccountPort>()
    val data = koinInject<PublicProfileData>()
    val checkIns = koinInject<PublicCheckInsPort>()
    val visibility = koinInject<AuthorVisibility>()
    val blocks = koinInject<AuthorBlockActions>()
    val snapshots = koinInject<ProfileSnapshotStore>()
    val language = koinInject<LanguagePort>()
    val imageFetcher = koinInject<ImageUrlFetcher>()
    val collections = iosCollectionsWorkspaceContent()
    val media = remember(imageFetcher) { IosCatalogueMedia(imageFetcher, Constants.BASE_URL) }
    return iosPublicProfileContent(factory, shelf, accounts, data, checkIns, visibility, blocks, snapshots, language, media, collections)
}

internal fun iosPublicProfileContent(factory: (IosPublicProfile) -> UIViewController, shelf: IosShelfFactory,
    accounts: ProfileAccountPort, data: PublicProfileData, checkIns: PublicCheckInsPort, visibility: AuthorVisibility,
    blocks: AuthorBlockActions, snapshots: ProfileSnapshotStore, language: LanguagePort, media: IosCatalogueMedia,
    collections: (String, String?, String?) -> UIViewController,
): (String, String?, String?, Boolean) -> UIViewController {
    return { slug, nickname, avatarUrl, checkInsOnly ->
        val profile = createIosPublicProfileSession(slug, data, checkIns, accounts, AppProfileAuthorVisibility(visibility), snapshots)
        factory(IosPublicProfile(profile, AppProfileAuthorActions(blocks), language, media,
            slug, nickname, avatarUrl, checkInsOnly,
            shelfController = { target, own, allowed -> nativeShelfController(shelf("public", target, own, allowed)) }, collections = collections))
    }
}
