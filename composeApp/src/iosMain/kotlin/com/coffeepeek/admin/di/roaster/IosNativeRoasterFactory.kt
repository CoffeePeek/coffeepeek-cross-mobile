package com.coffeepeek.admin.di.roaster

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coffeepeek.admin.analytics.Entity
import com.coffeepeek.admin.analytics.NativeScreenAnalytics
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.feature.roaster.impl.NativeRoasterDetailHost
import com.coffeepeek.feature.roaster.impl.createNativeRoasterDetailSession
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

/** Native root assembly only: the feature owns its model; Swift owns the controller and final close. */
internal class IosNativeRoasterFactory(private val repository: RoasterRepository,
    private val favorites: RoasterFavoritesPort, private val shops: RoasterShopPhotos,
    private val language: LanguagePort, private val accounts: SessionReader,
    private val media: IosCatalogueMedia, private val ownership: BusinessOwnershipEntry) {
    fun detail(slug: String): IosRoasterDetail {
        val session = createNativeRoasterDetailSession(slug, repository, favorites, shops, RoasterAnalyticsAdapter.detail())
        val host = NativeRoasterDetailHost(Navigator::popBack,
            { Navigator.navigate(Navigator.Screen.ShopDetail(it)) }, OpenInBrowser::openInBrowser,
            { ShareHelper.shareText("https://coffeepeek.by$it") }, { platformHaptic(HapticEvent.Selection) }, Modifier)
        val source = IosRoasterDetail(session, host, language, accounts, media,
            { allowed -> roasterOwnershipController(session, ownership, language, media, allowed) }, session::close)
        val analytics = NativeScreenAnalytics(source.scope, "roaster_detail", source.screenVisible)
        analytics.entity(Entity.Roaster, session.state.map { it.details != null && source.isCurrent() })
        listOf("photos", "contacts", "shops").forEach { section ->
            analytics.feature("roaster_$section", "roaster_detail", source.fractions.map { (it[section] ?: 0.0) >= .1 })
        }
        analytics.feature("favorites", "roaster_card", source.fractions.map { it.any { (key, value) -> key.startsWith("favorite:") && value >= .1 } })
        source.scope.launch { ownership.profileChanges.collect { if (it > 0 && source.isCurrent()) session.retry() } }
        return source
    }
}

@Composable
internal fun iosRoasterDetailContent(): (String) -> UIViewController {
    val repository = koinInject<RoasterRepository>()
    val favorites = koinInject<RoasterFavoritesPort>()
    val shops = koinInject<RoasterShopPhotos>()
    val language = koinInject<LanguagePort>()
    val accounts = koinInject<SessionReader>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val ownership = koinInject<BusinessOwnershipEntry>()
    val factory = remember(repository, favorites, shops, language, accounts, fetcher, ownership) {
        IosNativeRoasterFactory(repository, favorites, shops, language, accounts,
            IosCatalogueMedia(fetcher, Constants.BASE_URL), ownership)
    }
    return { slug -> roasterController(factory.detail(slug)) }
}
