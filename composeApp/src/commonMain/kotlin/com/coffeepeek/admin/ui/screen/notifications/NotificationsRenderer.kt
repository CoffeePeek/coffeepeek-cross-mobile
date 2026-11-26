package com.coffeepeek.admin.ui.screen.notifications

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface NotificationsRenderer {
    suspend fun open(id: String, accountId: String, generation: Long): Boolean
    suspend fun observePendingOpen(loggedIn: Flow<Boolean>)

    @Composable
    fun Bell()

    @Composable
    fun Preference(label: String, description: String)

    @Composable
    fun HandlePendingOpen(isLoggedIn: Boolean) = Unit
}
