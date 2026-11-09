package com.coffeepeek.feature.homecheckin.api

import androidx.compose.runtime.Composable

/** Introduces check-ins before the guest starts browsing. The app owns session and navigation. */
interface GuestCheckInOnboardingEntry {
    @Composable
    fun Content(onContinueAsGuest: () -> Unit, onSignIn: () -> Unit, onRegister: () -> Unit)
}
