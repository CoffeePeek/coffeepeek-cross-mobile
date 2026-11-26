package com.coffeepeek.admin.di.shop

import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.navigation_back
import coffeepeek.composeapp.generated.resources.snackbar_dismiss
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.media.IosPhotoViewer
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.feature.shop.api.ShopDetailEffect
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

private lateinit var photoFactory: (IosPhotoViewer, String, () -> Unit) -> UIViewController
private lateinit var messageFactory: (IosShopDetailNotice) -> UIViewController
private lateinit var errorFactory: (IosShopDetailSessionError) -> UIViewController

internal fun configureNativeShopDetailOverlays(photo: (IosPhotoViewer, String, () -> Unit) -> UIViewController,
    message: (IosShopDetailNotice) -> UIViewController, error: (IosShopDetailSessionError) -> UIViewController) {
    photoFactory = photo
    messageFactory = message
    errorFactory = error
}

@Composable
internal actual fun ShopDetailPhotoViewer(photo: ShopDetailEffect.OpenPhoto, lease: ShopBrowsingSession, onDismiss: () -> Unit) {
    val fetcher = koinInject<ImageUrlFetcher>()
    val language = LocalCoffeePeekLanguageTag.current
    val close by rememberUpdatedState(onDismiss)
    val album = remember(photo, lease) { IosCatalogueMedia(fetcher, Constants.BASE_URL).viewer(
        CoffeePeekPhotoViewerImages(photo.imageUrls, photo.originalUrls), photo.initialIndex, lease::isCurrent) }
    val controller = remember(album) { photoFactory(album, language) { close() } }
    DisposableEffect(album, controller) { onDispose {
        album.setVisible(false)
        controller.presentedViewController?.dismissViewControllerAnimated(false, completion = null)
    } }
    NativeEditorControllerHost(controller, Modifier.fillMaxSize())
}

@Composable
internal actual fun ShopDetailHostFeedback(snackbar: SnackbarHostState, modifier: Modifier) {
    val data = snackbar.currentSnackbarData ?: return
    val closeLabel = stringResource(Res.string.snackbar_dismiss)
    val controller = remember(data) { messageFactory(IosShopDetailNotice(data.visuals.message, closeLabel) { data.dismiss() }) }
    NativeEditorControllerHost(controller, modifier.fillMaxWidth().height(110.dp))
}

@Composable
internal actual fun PlatformShopSessionUnavailable(title: String, retry: String, onRetry: () -> Unit): Boolean {
    val retryAction by rememberUpdatedState(onRetry)
    val back = stringResource(Res.string.navigation_back)
    val controller = remember(title, retry, back) { errorFactory(IosShopDetailSessionError(title, retry, back,
        { retryAction() }, Navigator::popBack)) }
    NativeEditorControllerHost(controller, Modifier.fillMaxSize())
    return true
}

class IosShopDetailNotice internal constructor(val message: String, val dismissLabel: String, val dismiss: () -> Unit)
class IosShopDetailSessionError internal constructor(val title: String, val retry: String, val back: String,
    val onRetry: () -> Unit, val onBack: () -> Unit)
