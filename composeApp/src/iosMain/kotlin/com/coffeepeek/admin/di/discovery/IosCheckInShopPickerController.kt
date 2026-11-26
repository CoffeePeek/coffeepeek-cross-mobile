package com.coffeepeek.admin.di.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.screen.checkins.CheckInShopPickerRenderer
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.api.LanguagePort
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

object IosCheckInShopPickerControllers {
    private lateinit var makeController: (IosCheckInShopPicker) -> UIViewController
    fun configure(factory: (IosCheckInShopPicker) -> UIViewController) { makeController = factory }
    internal fun controller(source: IosCheckInShopPicker) = makeController(source)
}

internal class IosNativeCheckInShopPickerFactory(private val factory: NativeCheckInShopPickerSessionFactory,
    private val accounts: SessionReader, private val language: LanguagePort, private val media: IosCatalogueMedia,
) {
    fun controller() = IosCheckInShopPickerControllers.controller(IosCheckInShopPicker(factory, accounts, language, media))
}

@Composable
internal fun iosCheckInShopPickerContent(): () -> UIViewController {
    val factory = koinInject<CheckInShopPickerRenderer>() as NativeCheckInShopPickerSessionFactory
    val accounts = koinInject<SessionReader>()
    val language = koinInject<LanguagePort>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val owner = remember(factory, accounts, language, fetcher) {
        IosNativeCheckInShopPickerFactory(factory, accounts, language, IosCatalogueMedia(fetcher, Constants.BASE_URL))
    }
    return owner::controller
}
