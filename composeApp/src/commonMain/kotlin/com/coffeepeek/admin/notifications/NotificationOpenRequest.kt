package com.coffeepeek.admin.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class NotificationOpenRequest(val id: String, val accountId: String)

/** Native tap handoff survives the interval before root navigation and the account are ready. */
internal object NotificationOpens {
    private val mutable = MutableStateFlow<NotificationOpenRequest?>(null)
    val pending = mutable.asStateFlow()

    fun submit(data: Map<String, String?>) {
        val id = data["notificationId"]?.takeIf(::validNotificationPart) ?: return
        val account = data["userId"]?.takeIf(::validNotificationPart) ?: return
        mutable.value = NotificationOpenRequest(id, account)
    }

    fun consume(request: NotificationOpenRequest) {
        mutable.compareAndSet(request, null)
    }
}

private fun validNotificationPart(value: String): Boolean = value.length in 1..100 &&
    value.none { it in "/\\?#" || it.isISOControl() || it.isWhitespace() } && value !in listOf(".", "..")
