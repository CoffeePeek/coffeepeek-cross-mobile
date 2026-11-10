@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.profile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountPortsTest {
    @Test fun sessionEndSignsOutOfThePlatformBeforeTheLoginRedirect() = runTest {
        val order = mutableListOf<String>()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val renderer = FeatureAccountScreenRenderer(
            edit = ThrowingEdit, deletion = ThrowingDeletion, appScope = scope,
            platformSignOut = { order += "signOut" }, openLogin = { order += "login" },
        )
        renderer.endSession()
        assertEquals(listOf("signOut", "login"), order)
    }

    private object ThrowingEdit : com.coffeepeek.feature.profile.api.EditProfileEntry {
        @androidx.compose.runtime.Composable
        override fun Content(onBack: () -> Unit, onOpenAccountDeletion: () -> Unit, modifier: androidx.compose.ui.Modifier) = error("unused")
    }

    private object ThrowingDeletion : com.coffeepeek.feature.profile.api.AccountDeletionEntry {
        @androidx.compose.runtime.Composable
        override fun Content(onClose: () -> Unit, onSessionEnded: () -> Unit, modifier: androidx.compose.ui.Modifier) = error("unused")
    }
}
