package com.coffeepeek.feature.shelf.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.shelf.api.ShelfPhoto
import com.coffeepeek.feature.shelf.domain.model.ShelfGrind
import com.coffeepeek.feature.shelf.domain.model.ShelfRoastPurpose
import com.coffeepeek.feature.shelf.impl.ui.ShelfAddViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import kotlinx.coroutines.launch

class IosShelfAddSession internal constructor(private val model: ShelfAddViewModel) {
    private val store = ViewModelStore().apply { put("shelf-add", model) }
    fun current() = ShelfNativeAddSnapshot(model.state.value)
    fun observe(receive: () -> Unit) = model.viewModelScope.launch { model.state.collect { receive() } }
    fun events(receive: (Boolean) -> Unit) = model.viewModelScope.launch {
        model.events.collect { receive(it == ShelfAddEvent.Saved) }
    }
    fun photo(bytes: ByteArray, camera: Boolean) = model.onAction(ShelfAddAction.PhotoPicked(ShelfPhoto(bytes, camera)))
    fun query(value: String) = model.onAction(ShelfAddAction.QueryChanged(value))
    fun coffee(slug: String) {
        val option = model.state.value.options.firstOrNull { it.slug == slug } ?: return
        model.onAction(ShelfAddAction.CoffeeChosen(option))
    }
    fun roaster(value: String) = model.onAction(ShelfAddAction.RoasterChanged(value))
    fun weight(grams: Int?) = model.onAction(ShelfAddAction.WeightChosen(grams))
    fun grind(value: String?) = model.onAction(ShelfAddAction.GrindChosen(ShelfGrind.entries.firstOrNull { it.name == value }))
    fun purpose(value: String?) = model.onAction(ShelfAddAction.RoastPurposeChosen(ShelfRoastPurpose.entries.firstOrNull { it.name == value }))
    fun date(value: String?) = model.onAction(ShelfAddAction.RoastDateChosen(ShelfDay.parse(value)))
    fun save(later: Boolean) = model.onAction(ShelfAddAction.Save(later))
    fun action(value: String) = model.onAction(when (value) {
        "download" -> ShelfAddAction.DownloadScanner
        "camera" -> ShelfAddAction.OpenCamera
        "closeCamera" -> ShelfAddAction.CloseCamera
        "revealed" -> ShelfAddAction.RevealFinished
        "landed" -> ShelfAddAction.CardLanded
        "photoFailed" -> ShelfAddAction.PhotoFailed
        "duplicate" -> ShelfAddAction.ConfirmDuplicate
        "cancelDuplicate" -> ShelfAddAction.CancelDuplicate
        "dismiss" -> ShelfAddAction.DismissMessage
        else -> ShelfAddAction.Close
    })
    fun photoBytes() = model.state.value.photo?.bytes
    fun cardBytes() = model.state.value.result?.card?.bytes
    fun close() = store.clear()
}

class ShelfNativeAddSnapshot internal constructor(state: ShelfAddState) {
    val step = state.step.name
    val scanner = when (state.scanner) {
        ShelfScannerStatus.Disabled -> "Disabled"
        ShelfScannerStatus.Checking -> "Checking"
        ShelfScannerStatus.Ready -> "Ready"
        is ShelfScannerStatus.NeedsDownload -> "NeedsDownload"
        is ShelfScannerStatus.Downloading -> "Downloading"
        ShelfScannerStatus.DownloadFailed -> "DownloadFailed"
    }
    val downloadBytes = (state.scanner as? ShelfScannerStatus.NeedsDownload)?.bytes ?: 0L
    val downloadDone = (state.scanner as? ShelfScannerStatus.Downloading)?.done ?: 0L
    val downloadTotal = (state.scanner as? ShelfScannerStatus.Downloading)?.total ?: 0L
    val stages = state.stageOutcomes().map { ShelfNativeScanStage(it.stage.name, it.status.name, it.reasons.map { it.name }) }
    val frame = (state.liveFrame ?: state.result?.frame)?.points.orEmpty().map { ShelfNativePoint(it.first, it.second) }
    val failure = state.failure?.name
    val query = state.form.query
    val roaster = state.form.roaster
    val grams = state.form.weightGrams
    val grind = state.form.grind?.name
    val purpose = state.form.roastPurpose?.name
    val roastedOn = state.form.roastedOn?.iso
    val today = state.today.iso
    val searching = state.searching
    val searchFailed = state.searchFailed
    val options = state.options.map { ShelfNativeCoffee(it.slug, it.name, it.roasterName) }
    val saving = state.saving
    val duplicate = state.confirmDuplicate
    val landing = state.cardLanding
    val hints = state.hints.map { it.name }
    val message = when (val message = state.message) {
        ShelfAddMessage.PhotoUnreadable -> "PhotoUnreadable"
        is ShelfAddMessage.Failed -> message.issue.name
        null -> null
    }
}

class ShelfNativePoint internal constructor(val x: Float, val y: Float)
class ShelfNativeScanStage internal constructor(val name: String, val status: String, val reasons: List<String>)
class ShelfNativeCoffee internal constructor(val slug: String, val title: String, val roaster: String)
