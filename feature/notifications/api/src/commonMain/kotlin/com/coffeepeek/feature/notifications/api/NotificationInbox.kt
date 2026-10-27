package com.coffeepeek.feature.notifications.api

enum class NotificationKind {
    ShopApproved, RoasterApproved, ShopRejected, RoasterRejected,
    CheckInModeration, CorrectionModeration, NewShop, Inactivity, General,
}

data class NotificationItem(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val createdAtUtc: String,
    val read: Boolean,
    val status: String? = null,
    val canOpen: Boolean = false,
)

data class NotificationInboxState(
    val accountId: String? = null,
    val generation: Long = 0,
    val items: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val nextCursor: String? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val markingRead: Boolean = false,
    val settingsLoaded: Boolean = false,
    val pushEnabled: Boolean = true,
    val savingSettings: Boolean = false,
    val deviceAvailable: Boolean = false,
    val permissionGranted: Boolean = false,
    val error: String? = null,
    val settingsError: String? = null,
    val registrationDiagnostic: String? = null,
)
