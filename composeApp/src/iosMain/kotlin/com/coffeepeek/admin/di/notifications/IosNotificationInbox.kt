
package com.coffeepeek.admin.di.notifications

import com.coffeepeek.feature.notifications.api.NotificationInboxError
import com.coffeepeek.feature.notifications.impl.IosInboxItem
import com.coffeepeek.feature.notifications.impl.IosInboxPresentation
import com.coffeepeek.feature.notifications.impl.IosNotificationInboxSession
import kotlinx.coroutines.Job

class IosNotificationInbox internal constructor(private val session: IosNotificationInboxSession) {
    fun current() = session.current().toIos()

    fun observe(receive: (IosNotificationInboxState) -> Unit): IosNotificationInboxObservation =
        IosNotificationInboxObservation(session.observe { receive(it.toIos()) })

    fun show() = session.show()
    fun appeared(version: Long) = session.appeared(version)
    fun dismiss(version: Long) = session.dismiss(version)
    fun refresh(version: Long) = session.refresh(version)
    fun loadMore(version: Long) = session.loadMore(version)
    fun markAllRead(version: Long) = session.markAllRead(version)
    fun seen(version: Long, id: String) = session.seen(version, id)
    fun select(version: Long, id: String) = session.select(version, id)
    fun signIn(version: Long) = session.signIn(version)
    fun close() = session.close()
}

class IosNotificationInboxObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}

data class IosNotificationInboxState(
    val sheetVersion: Long,
    val signedIn: Boolean,
    val unreadCount: Int,
    val items: List<IosNotificationInboxRow>,
    val loading: Boolean,
    val loadingMore: Boolean,
    val markingRead: Boolean,
    val hasMore: Boolean,
    val openingId: String?,
    val errorKey: String?,
)

data class IosNotificationInboxRow(
    val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val date: String,
    val today: Boolean,
    val read: Boolean,
    val rejected: Boolean,
    val canOpen: Boolean,
)

private fun IosInboxPresentation.toIos() = IosNotificationInboxState(
    sheetVersion, signedIn, unreadCount, items.map(IosInboxItem::toIos), loading, loadingMore, markingRead,
    hasMore, openingId, when (error) {
        NotificationInboxError.LoadFailed -> "notifications_inbox_load_failed"
        NotificationInboxError.MarkReadFailed -> "notifications_mark_read_failed"
        null -> null
    },
)

private fun IosInboxItem.toIos() = IosNotificationInboxRow(id, kind.name, title, body, date, today, read, rejected, canOpen)
