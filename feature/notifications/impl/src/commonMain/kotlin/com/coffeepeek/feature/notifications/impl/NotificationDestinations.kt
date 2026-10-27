package com.coffeepeek.feature.notifications.impl

import com.coffeepeek.feature.notifications.api.NotificationDestination
import com.coffeepeek.feature.notifications.domain.Notification
import com.coffeepeek.feature.notifications.domain.NotificationTarget

internal fun Notification.canOpen(): Boolean {
    if (entityId.isNullOrBlank()) return false
    return when (type) {
        "NewShop", "ShopApproved", "ShopRejected" -> entityType == "Shop"
        "RoasterApproved", "RoasterRejected" -> entityType == "Roaster"
        "CorrectionModeration" -> entityType == "ShopChange"
        "CheckInModeration" -> entityType == "CheckIn"
        else -> false
    }
}

internal fun NotificationTarget.toDestination(): NotificationDestination? {
    if (value.isBlank() || value.length > 100 || value.any { it in "/\\?#" || it.isISOControl() } || value in listOf(".", "..")) return null
    return when (kind) {
        "Shop" -> NotificationDestination.Shop(value)
        "ShopChange" -> NotificationDestination.ShopChange(value)
        "ShopSubmission" -> NotificationDestination.ShopSubmission(value)
        "RoasterSubmission" -> NotificationDestination.RoasterSubmissions
        "CheckIns" -> NotificationDestination.CheckIns
        else -> null
    }
}
