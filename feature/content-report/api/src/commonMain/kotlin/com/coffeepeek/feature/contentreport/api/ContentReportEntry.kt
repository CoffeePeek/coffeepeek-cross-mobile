package com.coffeepeek.feature.contentreport.api

import androidx.compose.runtime.Composable

/** Composition owns route identity, account lifetime, access retry and navigation. */
interface ContentReportEntry {
    @Composable
    fun Content(id: String, isCheckIn: Boolean, isPreview: Boolean, isLoggedIn: Boolean, accessAvailable: Boolean,
        onBack: () -> Unit, onSignIn: () -> Unit, onRetryAccess: () -> Unit)
}
