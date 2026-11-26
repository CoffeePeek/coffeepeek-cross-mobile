package com.coffeepeek.admin.di.ownership

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.IosNativePhotoPicker
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.ownership.impl.NativeOwnershipPhoto
import com.coffeepeek.feature.ownership.impl.createIosOwnershipSession
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

/** Application assembly; the feature owns account guards, drafts and writes. */
internal class IosNativeOwnershipFactory(private val entry: BusinessOwnershipEntry,
    private val language: LanguagePort, private val media: IosCatalogueMedia) {
    fun controller(tab: Boolean, kind: String?, slug: String?, id: String?): UIViewController {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        lateinit var screen: IosOwnershipScreen
        val photos = IosNativePhotoPicker(scope) { screen.photoHost() }
        val session = createIosOwnershipSession(entry, tab, kind, slug, id,
            photos = { maximum, receive -> photos.show(false, maximum) { selected ->
                receive(selected.map { NativeOwnershipPhoto(it.fileName, it.contentType, it.bytes) })
            } }, onClose = Navigator::popBack, onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
            onDispose = { photos.close(); scope.cancel() })
        screen = IosOwnershipScreen(session, language, media)
        return ownershipFactory(screen)
    }
}

@Composable
internal fun iosOwnershipContent(): (Navigator.Screen) -> (() -> UIViewController)? {
    val entry = koinInject<BusinessOwnershipEntry>()
    val language = koinInject<LanguagePort>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val factory = remember(entry, language, fetcher) {
        IosNativeOwnershipFactory(entry, language, IosCatalogueMedia(fetcher, Constants.BASE_URL))
    }
    return { route -> when (route) {
        Navigator.Screen.BusinessTab -> { { factory.controller(true, null, null, null) } }
        is Navigator.Screen.MyBusiness -> { { factory.controller(false, route.kind, route.slug, route.businessId) } }
        else -> null
    } }
}
