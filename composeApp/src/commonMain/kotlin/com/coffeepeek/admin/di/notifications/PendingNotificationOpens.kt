package com.coffeepeek.admin.di.notifications

import com.coffeepeek.admin.notifications.NotificationOpenRequest
import com.coffeepeek.admin.notifications.NotificationOpens
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import kotlinx.coroutines.flow.*

/** Application lifetime only; Swift receives the resolved navigation event. */
internal class PendingNotificationOpens(private val inbox: Flow<NotificationInboxState>,
    private val currentUserId: () -> String?,
    private val open: suspend (NotificationOpenRequest, Long) -> Unit,
    private val requests: Flow<NotificationOpenRequest?> = NotificationOpens.pending,
    private val consume: (NotificationOpenRequest) -> Unit = NotificationOpens::consume) {
    suspend fun observe(loggedIn: Flow<Boolean>) {
        combine(requests, loggedIn, inbox) { request, signedIn, state ->
            PendingNotificationContext(request, signedIn, state.accountId, state.generation)
        }.distinctUntilChanged().collectLatest { context -> handle(context) }
    }

    private suspend fun handle(context: PendingNotificationContext) {
        val request = context.request ?: return
        if (context.loggedIn && (context.account == null || context.account != currentUserId())) return
        try {
            if (context.loggedIn && context.account == request.accountId) open(request, context.generation)
        } finally {
            consume(request)
        }
    }
}

private data class PendingNotificationContext(val request: NotificationOpenRequest?, val loggedIn: Boolean,
    val account: String?, val generation: Long)
