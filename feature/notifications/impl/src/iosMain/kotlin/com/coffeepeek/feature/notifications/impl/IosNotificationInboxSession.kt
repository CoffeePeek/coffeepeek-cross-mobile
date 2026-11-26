
package com.coffeepeek.feature.notifications.impl

import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationInboxError
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import com.coffeepeek.feature.notifications.api.NotificationItem
import com.coffeepeek.feature.notifications.impl.ui.isToday
import com.coffeepeek.feature.notifications.impl.ui.localDateTime
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

fun createIosNotificationInboxSession(
    feature: NotificationFeature,
    onOpen: suspend (String, String, Long) -> Boolean,
    onSignIn: () -> Unit,
): IosNotificationInboxSession = IosNotificationInboxSession(feature, onOpen, onSignIn, Dispatchers.Main.immediate)

data class IosInboxPresentation(
    val sheetVersion: Long,
    val signedIn: Boolean,
    val unreadCount: Int,
    val items: List<IosInboxItem>,
    val loading: Boolean,
    val loadingMore: Boolean,
    val markingRead: Boolean,
    val hasMore: Boolean,
    val openingId: String?,
    val error: NotificationInboxError?,
)

class IosInboxItem internal constructor(item: NotificationItem) {
    val id = item.id
    val kind = item.kind
    val title = item.title
    val body = item.body
    val date = localDateTime(item.createdAtUtc)
    val today = isToday(item.createdAtUtc)
    val read = item.read
    val rejected = item.status == "Rejected"
    val canOpen = item.canOpen
}

class IosNotificationInboxSession internal constructor(
    private val feature: NotificationFeature,
    private val onOpen: suspend (String, String, Long) -> Boolean,
    private val onSignIn: () -> Unit,
    context: CoroutineContext,
) {
    private val scope = CoroutineScope(context + SupervisorJob(context[Job]))
    private val sheet = MutableStateFlow<OpenInbox?>(null)
    private var nextVersion = 0L
    private var offeredPermission = false
    private var visibleVersion = 0L
    private var opening: Job? = null

    init {
        scope.launch { feature.state.collect(::accountChanged) }
    }

    fun current(): IosInboxPresentation = presentation(feature.state.value, sheet.value)

    fun observe(receive: (IosInboxPresentation) -> Unit): Job = scope.launch {
        combine(feature.state, sheet, ::presentation).collect(receive)
    }

    fun show() {
        val state = feature.state.value
        if (sheet.value?.matches(state) == true) return
        sheet.value = OpenInbox(++nextVersion, state.generation, state.accountId)
        offeredPermission = false
        visibleVersion = 0
        feature.refresh()
    }

    fun appeared(version: Long) {
        if (currentSheet(version) == null) return
        visibleVersion = version
        offerPermission(feature.state.value)
    }

    fun dismiss(version: Long) {
        if (sheet.value?.version != version) return
        clear()
    }

    fun refresh(version: Long) {
        if (currentSheet(version)?.accountId != null) feature.refresh()
    }

    fun loadMore(version: Long) {
        if (currentSheet(version)?.accountId != null) feature.loadMore()
    }

    fun markAllRead(version: Long) {
        if (currentSheet(version)?.accountId != null) feature.markAllRead()
    }

    fun seen(version: Long, id: String) {
        if (currentSheet(version)?.accountId == null || visibleVersion != version) return
        val item = feature.state.value.items.firstOrNull { it.id == id } ?: return
        if (!item.read) feature.markRead(id)
    }

    fun select(version: Long, id: String) {
        val displayed = currentSheet(version) ?: return
        val accountId = displayed.accountId ?: return
        val state = feature.state.value
        if (displayed.openingId != null || state.loading || state.loadingMore) return
        val item = state.items.firstOrNull { it.id == id } ?: return
        if (item.canOpen) open(displayed, accountId, id) else seen(version, id)
    }

    fun signIn(version: Long) {
        val displayed = currentSheet(version) ?: return
        if (displayed.accountId != null) return
        clear()
        onSignIn()
    }

    fun close() {
        clear()
        scope.cancel()
    }

    private fun open(displayed: OpenInbox, accountId: String, id: String) {
        sheet.value = displayed.copy(openingId = id)
        opening = scope.launch(start = CoroutineStart.LAZY) {
            try {
                if (onOpen(id, accountId, displayed.generation)) dismiss(displayed.version)
            } finally {
                currentSheet(displayed.version)?.let { sheet.value = it.copy(openingId = null) }
            }
        }
        opening?.start()
    }

    private fun accountChanged(state: NotificationInboxState) {
        val displayed = sheet.value ?: return
        if (!displayed.matches(state)) clear() else offerPermission(state)
    }

    private fun offerPermission(state: NotificationInboxState) {
        val displayed = currentSheet(visibleVersion) ?: return
        if (displayed.accountId == null || offeredPermission || !state.settingsLoaded) return
        offeredPermission = true
        feature.onInboxOpened()
    }

    private fun currentSheet(version: Long): OpenInbox? =
        sheet.value?.takeIf { it.version == version && it.matches(feature.state.value) }

    private fun clear() {
        sheet.value = null
        visibleVersion = 0
        offeredPermission = false
        opening?.cancel()
        opening = null
    }

    private fun presentation(state: NotificationInboxState, displayed: OpenInbox?): IosInboxPresentation {
        val active = displayed?.takeIf { it.matches(state) }
        val items = if (active?.accountId != null) state.items.map(::IosInboxItem) else emptyList()
        return IosInboxPresentation(active?.version ?: 0, state.accountId != null,
            state.unreadCount, items, state.loading, state.loadingMore, state.markingRead,
            state.nextCursor != null, active?.openingId, state.error)
    }

    private data class OpenInbox(
        val version: Long,
        val generation: Long,
        val accountId: String?,
        val openingId: String? = null,
    ) {
        fun matches(state: NotificationInboxState): Boolean =
            generation == state.generation && accountId == state.accountId
    }
}
