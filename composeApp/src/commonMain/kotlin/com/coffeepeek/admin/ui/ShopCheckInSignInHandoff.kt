package com.coffeepeek.admin.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/** Keeps the scanned shop through the navigation reset after authentication. */
internal class ShopCheckInSignInHandoff {
    private class Request(val shopId: String)

    private val pending = MutableStateFlow<Request?>(null)

    fun attempt(shopId: String, hasActiveSession: () -> Boolean): Attempt {
        val request = Request(shopId)
        return Attempt(
            begin = { pending.value = request },
            cancel = { pending.compareAndSet(request, null) },
            hasActiveSession = hasActiveSession,
        )
    }

    class Attempt internal constructor(
        val begin: () -> Unit,
        val cancel: () -> Unit,
        private val hasActiveSession: () -> Boolean,
    ) {
        fun dispose() {
            if (!hasActiveSession()) cancel()
        }
    }

    fun take(): Navigator.Screen.ShopCheckInEditor? =
        pending.getAndUpdate { null }?.let { Navigator.Screen.ShopCheckInEditor(it.shopId) }
}
