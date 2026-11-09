package com.coffeepeek.admin.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/** Keeps the scanned shop through the navigation reset after authentication. */
internal class ShopCheckInSignInHandoff {
    class Request(val shopId: String)

    private val pending = MutableStateFlow<Request?>(null)

    fun begin(request: Request) {
        pending.value = request
    }

    fun cancel(request: Request) {
        pending.compareAndSet(request, null)
    }

    fun take(): Navigator.Screen.ShopDetail? =
        pending.getAndUpdate { null }?.let { Navigator.Screen.ShopDetail(it.shopId, openCheckIn = true) }
}
