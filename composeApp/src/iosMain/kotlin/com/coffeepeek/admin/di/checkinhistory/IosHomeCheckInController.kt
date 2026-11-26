package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.api.LanguagePort
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

object IosHomeCheckInControllers {
    private lateinit var makeController: (IosHomeCheckInRoute) -> UIViewController
    fun configure(factory: (IosHomeCheckInRoute) -> UIViewController) { makeController = factory }
    internal fun controller(source: IosHomeCheckInRoute) = makeController(source)
}

internal class IosNativeHomeCheckInFactory(private val accounts: SessionReader, private val editors: HistoryHomeEditorFactory,
    private val language: LanguagePort, private val fetcher: ImageUrlFetcher,
) {
    fun controller(id: String, edit: Boolean) = IosHomeCheckInControllers.controller(IosHomeCheckInRoute(accounts, editors, language, fetcher, id, edit))
}

@Composable
internal fun iosHomeCheckInContent(): (String, Boolean) -> UIViewController {
    val accounts = koinInject<SessionReader>()
    val editors = koinInject<HistoryHomeEditorFactory>()
    val language = koinInject<LanguagePort>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val factory = remember(accounts, editors, language, fetcher) { IosNativeHomeCheckInFactory(accounts, editors, language, fetcher) }
    return factory::controller
}
