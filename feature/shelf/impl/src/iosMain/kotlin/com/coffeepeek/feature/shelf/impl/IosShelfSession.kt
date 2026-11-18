package com.coffeepeek.feature.shelf.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.shelf.impl.ui.ShelfViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import kotlinx.coroutines.launch

/** Native rendering delegates every write, handoff and arrival to the shared shelf model. */
class IosShelfSession internal constructor(private val model: ShelfViewModel) {
    private val store = ViewModelStore().apply { put("shelf", model) }
    fun current() = ShelfNativeSnapshot(model.state.value)
    fun observe(receive: () -> Unit) = model.viewModelScope.launch { model.state.collect { receive() } }
    fun events(receive: (String, String?) -> Unit) = model.viewModelScope.launch {
        model.events.collect { event -> when (event) {
            ShelfEvent.AddBag -> receive("add", null)
            ShelfEvent.OpenArchive -> receive("archive", null)
            ShelfEvent.SignIn -> receive("signIn", null)
            is ShelfEvent.OpenBag -> receive("bag", event.id)
        } }
    }
    fun refresh() = model.onAction(ShelfAction.Refresh)
    fun open(id: String) = model.onAction(ShelfAction.OpenBag(id))
    fun move(id: String, position: Int) = model.onAction(ShelfAction.Move(id, position))
    fun startFinish(id: String) = model.onAction(ShelfAction.StartFinish(id))
    fun cancelFinish() = model.onAction(ShelfAction.CancelFinish)
    fun finish(id: String, rating: Int?, buyAgain: Boolean?) = model.onAction(ShelfAction.Finish(id, rating, buyAgain))
    fun undo() {
        val message = model.state.value.message as? ShelfMessage.Finished ?: return
        model.onAction(ShelfAction.UndoFinish(message.bagId, message.position))
    }
    fun dismissMessage() = model.onAction(ShelfAction.DismissMessage)
    fun arrived(id: String) = model.onAction(ShelfAction.ArrivalPresented(id))
    fun archive() = model.onAction(ShelfAction.OpenArchive)
    fun add() = model.onAction(ShelfAction.AddBag)
    fun photo(id: String) = model.state.value.bags.firstOrNull { it.id == id }?.bag?.photo?.url
    fun close() = store.clear()
}

class ShelfNativeSnapshot internal constructor(state: ShelfState) {
    val items = state.bags.map { ShelfNativeItem(it, state.today) }
    val loading = state.loading
    val loaded = state.loaded
    val failed = state.loadFailed
    val finishingId = state.finishingId
    val arrivingId = state.arrivingId
    val grams = state.summary.grams
    val roasters = state.summary.roasters
    val undo = state.message is ShelfMessage.Finished
    val failure = (state.message as? ShelfMessage.Failed)?.issue?.name
}
