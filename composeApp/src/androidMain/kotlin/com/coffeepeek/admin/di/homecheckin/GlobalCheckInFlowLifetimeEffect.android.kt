package com.coffeepeek.admin.di.homecheckin

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
internal actual fun GlobalCheckInFlowLifetimeEffect(owner: GlobalCheckInFlowStore) {
    val activity = LocalActivity.current
    DisposableEffect(owner, activity) {
        onDispose { owner.dispose(activity?.isChangingConfigurations == true) }
    }
}
