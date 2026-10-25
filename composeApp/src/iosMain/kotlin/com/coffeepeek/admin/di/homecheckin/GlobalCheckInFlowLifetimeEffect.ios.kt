package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
internal actual fun GlobalCheckInFlowLifetimeEffect(owner: GlobalCheckInFlowStore) {
    DisposableEffect(owner) { onDispose { owner.dispose(changingConfigurations = false) } }
}
