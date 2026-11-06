package com.coffeepeek.feature.authorblocking.api

import androidx.compose.runtime.Composable

/** Root navigation owns callbacks; the feature owns the list and global confirmation UI. */
interface AuthorBlockingEntry {
    @Composable
    fun Screen(onBack: () -> Unit, onLogin: () -> Unit, onRegister: () -> Unit)

    /** Install once in the visible platform root, never in every nested sheet. */
    @Composable
    fun Host(onLogin: () -> Unit, bottomClearanceDp: Float = 0f)
}
