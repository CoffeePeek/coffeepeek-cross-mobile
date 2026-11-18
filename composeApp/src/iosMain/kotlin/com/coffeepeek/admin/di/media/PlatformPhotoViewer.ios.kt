@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.media

import androidx.compose.runtime.*
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.component.PresentNativeController
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

object IosCheckInPhotoControllers {
    internal lateinit var factory: (IosCheckInPhotoPresentation) -> UIViewController
    fun configure(factory: (IosCheckInPhotoPresentation) -> UIViewController) { this.factory = factory }
}

@Composable
internal actual fun PlatformPhotoViewer(images: CoffeePeekPhotoViewerImages, initialIndex: Int, onDismiss: () -> Unit) {
    val accounts = koinInject<SessionReader>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val language = koinInject<LanguagePort>()
    val dismiss = rememberUpdatedState(onDismiss)
    val source = remember(images, initialIndex) { photoPresentation(images, initialIndex, accounts, fetcher, language) { dismiss.value() } }
    val controller = remember(source) { IosCheckInPhotoControllers.factory(source) }
    PresentNativeController(controller, LocalUIViewController.current)
    LaunchedEffect(source) {
        accounts.observeSession().catch {
            if (it is CancellationException) throw it
            source.dismiss()
        }.collect { if (!source.isCurrent()) source.dismiss() }
    }
    DisposableEffect(source) { onDispose { source.close() } }
}

private fun photoPresentation(images: CoffeePeekPhotoViewerImages, index: Int, accounts: SessionReader,
    fetcher: ImageUrlFetcher, language: LanguagePort, onDismiss: () -> Unit): IosCheckInPhotoPresentation {
    val generation = accounts.accountGeneration()
    val user = accounts.peekSession()?.userId
    val signedIn = accounts.isActiveSession(accounts.peekSession())
    val media = IosCatalogueMedia(fetcher, Constants.BASE_URL)
    return IosCheckInPhotoPresentation(language.current().languageTag,
        { accounts.accountGeneration() == generation && accounts.peekSession()?.userId == user &&
            accounts.isActiveSession(accounts.peekSession()) == signedIn }, onDismiss,
        { allowed -> media.viewer(images, index, allowed) })
}
