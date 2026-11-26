package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.SnackbarHostState
import com.coffeepeek.feature.shop.api.ShopDetailEffect

@Composable
internal expect fun ShopDetailPhotoViewer(photo: ShopDetailEffect.OpenPhoto, lease: ShopBrowsingSession, onDismiss: () -> Unit)

@Composable
internal expect fun ShopDetailHostFeedback(snackbar: SnackbarHostState, modifier: Modifier)

@Composable
internal expect fun PlatformShopSessionUnavailable(title: String, retry: String, onRetry: () -> Unit): Boolean
