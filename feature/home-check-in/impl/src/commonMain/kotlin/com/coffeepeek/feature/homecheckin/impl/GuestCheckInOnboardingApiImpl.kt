package com.coffeepeek.feature.homecheckin.impl

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import com.coffeepeek.core.designsystem.component.CoffeePeekFullScreenDialog
import com.coffeepeek.feature.homecheckin.api.GuestCheckInOnboardingEntry
import com.coffeepeek.feature.homecheckin.impl.ui.compose.GuestCheckInOnboardingContent

fun createGuestCheckInOnboardingEntry(): GuestCheckInOnboardingEntry = GuestCheckInOnboardingApiImpl

private object GuestCheckInOnboardingApiImpl : GuestCheckInOnboardingEntry {
    @Composable
    override fun Content(onContinueAsGuest: () -> Unit, onSignIn: () -> Unit, onRegister: () -> Unit) {
        val hostInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .union(WindowInsets.safeGestures.only(WindowInsetsSides.Bottom))
        CoffeePeekFullScreenDialog(
            onDismiss = onContinueAsGuest,
            darkSystemBarIcons = MaterialTheme.colorScheme.surfaceVariant.luminance() > 0.5f,
        ) {
            GuestCheckInOnboardingContent(onContinueAsGuest, onSignIn, onRegister, hostInsets = hostInsets)
        }
    }
}
