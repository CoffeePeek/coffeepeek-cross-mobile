package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.homecheckin.impl.createGuestCheckInOnboardingEntry

@Composable
internal fun GuestCheckInOnboardingHost(onFinish: (Navigator.Screen?) -> Unit) {
    val entry = remember { createGuestCheckInOnboardingEntry() }
    entry.Content(
        onContinueAsGuest = { onFinish(null) },
        onSignIn = { onFinish(Navigator.Screen.Auth) },
        onRegister = { onFinish(Navigator.Screen.Register) },
    )
}
