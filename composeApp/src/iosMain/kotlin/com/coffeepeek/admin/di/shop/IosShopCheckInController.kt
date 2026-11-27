package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

object IosShopCheckInControllers {
    private lateinit var makeController: (IosShopCheckInRoute) -> UIViewController
    fun configure(factory: (IosShopCheckInRoute) -> UIViewController) { makeController = factory }
    internal fun controller(source: IosShopCheckInRoute) = makeController(source)
}
internal class IosNativeShopCheckInFactory(private val accounts: SessionReader, private val details: ShopDetailsRepository,
    private val forms: SessionShopCheckInEntry, private val language: LanguagePort,
) {
    fun controller(id: String, shopSelectionIncluded: Boolean = false) =
        IosShopCheckInControllers.controller(IosShopCheckInRoute(accounts, details, forms, language, id, shopSelectionIncluded))
}

@Composable
internal fun iosShopCheckInContent(): (String) -> UIViewController {
    val accounts = koinInject<SessionReader>()
    val details = koinInject<ShopDetailsRepository>()
    val forms = koinInject<ShopCheckInCreateEntry>() as SessionShopCheckInEntry
    val language = koinInject<LanguagePort>()
    val factory = remember(accounts, details, forms, language) { IosNativeShopCheckInFactory(accounts, details, forms, language) }
    return { id -> factory.controller(id) }
}
