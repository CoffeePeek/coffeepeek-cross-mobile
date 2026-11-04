package com.coffeepeek.admin.di.homecheckin

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberRecreationCheck(): () -> Boolean {
    val activity = LocalActivity.current
    return { activity?.isChangingConfigurations == true }
}
