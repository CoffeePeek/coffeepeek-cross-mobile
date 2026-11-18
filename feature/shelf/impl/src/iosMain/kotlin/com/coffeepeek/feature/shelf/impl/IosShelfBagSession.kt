package com.coffeepeek.feature.shelf.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.shelf.impl.ui.ShelfBagViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import kotlinx.coroutines.launch

class IosShelfBagSession internal constructor(private val model: ShelfBagViewModel) {
    private val store = ViewModelStore().apply { put("shelf-bag", model) }
    fun current() = ShelfNativeBagSnapshot(model.state.value)
    fun observe(receive: () -> Unit) = model.viewModelScope.launch { model.state.collect { receive() } }
    fun events(receive: (String, String?) -> Unit) = model.viewModelScope.launch {
        model.events.collect { event -> when (event) {
            ShelfBagEvent.Back -> receive("back", null)
            is ShelfBagEvent.OpenCoffee -> receive("coffee", event.slug)
        } }
    }
    fun action(value: String) = model.onAction(when (value) {
        "retry" -> ShelfBagAction.Retry
        "retryBrews" -> ShelfBagAction.RetryBrews
        "coffee" -> ShelfBagAction.OpenCoffee
        "finish" -> ShelfBagAction.StartFinish
        "cancelFinish" -> ShelfBagAction.CancelFinish
        "restore" -> ShelfBagAction.Restore
        "delete" -> ShelfBagAction.RequestDelete
        "cancelDelete" -> ShelfBagAction.CancelDelete
        "confirmDelete" -> ShelfBagAction.Delete
        "dismiss" -> ShelfBagAction.DismissFailure
        "brew" -> ShelfBagAction.Brew
        else -> ShelfBagAction.Back
    })
    fun finish(rating: Int?, buyAgain: Boolean?) = model.onAction(ShelfBagAction.Finish(rating, buyAgain))
    fun brew() = model.state.value.brewing
    fun brewed(saved: Boolean) = model.onAction(ShelfBagAction.BrewClosed(saved))
    fun photo() = model.state.value.bag?.bag?.photo?.url
    fun close() = store.clear()
}

class ShelfNativeBagSnapshot internal constructor(state: ShelfBagState) {
    val item = state.bag?.let { ShelfNativeItem(it, state.today) }
    val missing = state.missing
    val failed = state.loadFailed
    val archived = state.archived
    val finishing = state.finishing
    val deleting = state.deleting
    val brewing = state.brewing != null
    val busy = state.busy
    val failure = state.failure?.name
    val brewsLoading = state.brews == ShelfBagBrews.Loading
    val brewsFailed = state.brews == ShelfBagBrews.Failed
    val leftGrams = (state.brews as? ShelfBagBrews.Loaded)?.leftGrams
    val brews = (state.brews as? ShelfBagBrews.Loaded)?.brews.orEmpty().map {
        ShelfNativeBrew(it.id, it.day?.iso, it.method, it.doseGrams, it.score)
    }
}

class ShelfNativeBrew internal constructor(val id: String, val date: String?, val method: String?, val dose: Double?, val score: Int?)
