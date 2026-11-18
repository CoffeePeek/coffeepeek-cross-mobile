package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewer
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.feature.shop.api.ShopDetailEffect

@Composable
internal actual fun ShopDetailPhotoViewer(photo: ShopDetailEffect.OpenPhoto, lease: ShopBrowsingSession, onDismiss: () -> Unit) {
    CoffeePeekPhotoViewer(CoffeePeekPhotoViewerImages(photo.imageUrls, photo.originalUrls), photo.initialIndex, onDismiss)
}

@Composable
internal actual fun ShopDetailHostFeedback(snackbar: SnackbarHostState, modifier: Modifier) { SnackbarHost(snackbar, modifier) }

@Composable
internal actual fun PlatformShopSessionUnavailable(title: String, retry: String, onRetry: () -> Unit): Boolean = false
