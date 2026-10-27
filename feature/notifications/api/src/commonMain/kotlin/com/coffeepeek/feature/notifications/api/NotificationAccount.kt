package com.coffeepeek.feature.notifications.api

import kotlinx.coroutines.flow.Flow

/** The signed-in account the inbox belongs to; the application adapts its session storage to it. */
interface NotificationAccount {
    /** The id of the signed-in user, or null when nobody is signed in. */
    val userId: String?
    fun observeUserId(): Flow<String?>
}
