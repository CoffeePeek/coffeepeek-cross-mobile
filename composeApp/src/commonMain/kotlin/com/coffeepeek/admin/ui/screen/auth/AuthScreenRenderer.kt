package com.coffeepeek.admin.ui.screen.auth

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface AuthScreenRenderer {
    @Composable
    fun Login()

    @Composable
    fun Register()

    @Composable
    fun Recovery(initialEmail: String, onClose: () -> Unit, onBackToLogin: () -> Unit)
}
