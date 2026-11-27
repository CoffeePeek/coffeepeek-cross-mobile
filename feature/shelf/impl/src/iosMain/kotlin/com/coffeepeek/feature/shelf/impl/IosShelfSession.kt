package com.coffeepeek.feature.shelf.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.shelf.impl.ui.ShelfViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Native rendering delegates every write, handoff and arrival to the shared shelf model. */
class IosShelfSession internal constructor(private val model: ShelfViewModel) {
    private val store = ViewModelStore().apply { put("shelf", model) }
    private val finishDraft = ShelfNativeFinishDraft()
    private var lastMessage: ShelfMessage? = null
    private var messageRevision = 0
    fun current() = ShelfNativeSnapshot(model.state.value, finishDraft, messageKey())
    fun observe(receive: () -> Unit) = model.viewModelScope.launch {
        combine(model.state, finishDraft.state) { _, _ -> Unit }.collect { receive() }
    }
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
    fun startFinish(id: String) {
        finishDraft.reset()
        model.onAction(ShelfAction.StartFinish(id))
    }
    fun rate(value: Int) { if (model.state.value.finishingId != null) finishDraft.rate(value) }
    fun chooseAgain(value: Boolean) { if (model.state.value.finishingId != null) finishDraft.chooseAgain(value) }
    fun confirmFinish(id: String, skip: Boolean) {
        if (id != model.state.value.finishingId) return
        finish(id, finishDraft.rating.takeIf { !skip && it > 0 }, finishDraft.buyAgain.takeUnless { skip })
    }
    fun cancelFinish() = model.onAction(ShelfAction.CancelFinish)
    fun finish(id: String, rating: Int?, buyAgain: Boolean?) = model.onAction(ShelfAction.Finish(id, rating, buyAgain))
    fun undo() {
        val message = model.state.value.message as? ShelfMessage.Finished ?: return
        model.onAction(ShelfAction.UndoFinish(message.bagId, message.position))
    }
    fun dismissMessage(expectedKey: String? = null) {
        if (expectedKey != null && expectedKey != messageKey()) return
        model.onAction(ShelfAction.DismissMessage)
    }
    private fun messageKey(): String? {
        val message = model.state.value.message
        if (message !== lastMessage) {
            lastMessage = message
            messageRevision++
        }
        return message?.let { messageRevision.toString() }
    }
    fun arrived(id: String) = model.onAction(ShelfAction.ArrivalPresented(id))
    fun archive() = model.onAction(ShelfAction.OpenArchive)
    fun add() = model.onAction(ShelfAction.AddBag)
    fun photo(id: String) = model.state.value.bags.firstOrNull { it.id == id }?.bag?.photo?.url
    fun close() = store.clear()
}

class ShelfNativeSnapshot internal constructor(state: ShelfState, draft: ShelfNativeFinishDraft, val messageKey: String?) {
    val finishRating = draft.rating
    val finishBuyAgain = draft.buyAgain
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
