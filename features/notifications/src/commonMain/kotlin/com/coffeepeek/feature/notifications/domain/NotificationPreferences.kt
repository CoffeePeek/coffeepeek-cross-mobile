package com.coffeepeek.feature.notifications.domain

internal data class NotificationPreferences(val moderation: Boolean = true, val inactivity: Boolean = true, val custom: Boolean = true)
