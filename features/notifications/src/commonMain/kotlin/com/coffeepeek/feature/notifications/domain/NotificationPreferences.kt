package com.coffeepeek.feature.notifications.domain

data class NotificationPreferences(val moderation: Boolean = true, val inactivity: Boolean = false, val custom: Boolean = true)
