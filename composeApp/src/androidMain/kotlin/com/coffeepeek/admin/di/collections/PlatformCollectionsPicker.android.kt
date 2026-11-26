package com.coffeepeek.admin.di.collections

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.collections.api.CollectionTarget

@Composable
internal actual fun PlatformCollectionsPicker(target: CollectionTarget, onDismiss: () -> Unit,
    onSignIn: () -> Unit): Boolean = false
