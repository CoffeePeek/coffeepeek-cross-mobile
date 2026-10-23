package com.coffeepeek.feature.notifications.domain

data class Notification(
    val id: String, val type: String, val title: String, val body: String,
    val createdAtUtc: String, val readAtUtc: String?, val status: String?,
)

data class NotificationPage(
    val items: List<Notification>, val nextCursor: String?, val unreadCount: Int,
)
