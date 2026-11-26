package com.coffeepeek.feature.shop.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.api.ShopAnalytics
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.*
import com.coffeepeek.feature.shop.impl.ui.ShopDetailViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.data.trackShopAction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter

fun createNativeShopDetailSession(shopId: String, details: ShopDetailsRepository, votes: ShopCheckInVoteRepository,
    favorites: ShopFavorites, viewer: suspend () -> Result<ShopViewer>, day: () -> Int, minute: () -> Int,
    generation: Int, selectCollections: Boolean, isCurrent: () -> Boolean, analytics: ShopAnalytics,
    store: ViewModelStore,
): NativeShopDetailSession {
    val model = ShopDetailViewModel(shopId, details, votes, favorites, viewer, day, minute,
        generation, selectCollectionsOnFavorite = selectCollections)
    store.put("native-shop-detail:$shopId", model)
    return NativeShopDetailSession(model, generation, isCurrent, analytics, store::clear)
}

/** The existing detail writer with an explicit native lifetime and generation guard. */
class NativeShopDetailSession internal constructor(private val model: ShopDetailViewModel, val generation: Int,
    private val current: () -> Boolean, private val analytics: ShopAnalytics, private val dispose: () -> Unit,
) {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    internal val state = model.state
    internal val events = model.events.filter { isCurrent() && it.sessionGeneration == generation }
    private var closed = false

    fun isCurrent() = !closed && current()
    fun resume() = action(ShopDetailAction.Resume)
    fun retry() = action(ShopDetailAction.Retry)
    fun refresh(generation: Long) = action(ShopDetailAction.Refresh(generation))
    internal fun action(action: ShopDetailAction) {
        if (!isCurrent()) return
        trackShopAction(action, state.value, analytics)
        model.onAction(action)
    }
    internal fun openLinkedCheckIn(request: NativeShopLinkedCheckIn) {
        val state = state.value
        if (!isCurrent() || !state.isLoggedIn || state.details == null || !request.consume()) return
        action(ShopDetailAction.OpenCheckIn)
    }
    fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        dispose()
    }
}

/** Construct once per route, so a new account/visibility binding cannot replay the same intent. */
class NativeShopLinkedCheckIn(requested: Boolean) {
    private var pending = requested
    internal fun consume(): Boolean {
        if (!pending) return false
        pending = false
        return true
    }
}
