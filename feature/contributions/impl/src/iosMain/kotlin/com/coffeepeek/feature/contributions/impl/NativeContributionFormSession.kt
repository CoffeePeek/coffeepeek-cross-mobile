package com.coffeepeek.feature.contributions.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.contributions.api.*
import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.impl.change.*
import com.coffeepeek.feature.contributions.impl.roaster.*
import com.coffeepeek.feature.contributions.impl.shop.*
import com.coffeepeek.feature.contributions.impl.ui.component.ContributionPhotoDelivery
import com.coffeepeek.feature.contributions.impl.ui.map.ContributionMapPickerState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class NativeContributionFormKind { AddShop, AddRoaster, Suggest, Editor }

/** A native presentation of the existing shared form models, owned by one UIKit destination. */
class NativeContributionFormSession(
    private val kind: NativeContributionFormKind, repository: ContributionsRepository,
    catalogs: ContributionCatalogs, private val account: ContributionsAccount,
    private val location: ContributionLocation, slug: String = "", section: ChangeSection = ChangeSection.Description,
    private val requestId: String? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = ViewModelStore()
    private val shop = if (kind == NativeContributionFormKind.AddShop) AddShopViewModel(repository, catalogs, location, account) else null
    private val roaster = if (kind == NativeContributionFormKind.AddRoaster) AddRoasterViewModel(repository, catalogs, account) else null
    private val suggest = if (kind == NativeContributionFormKind.Suggest) SuggestChangeViewModel(slug, catalogs, account) else null
    private val editor = if (kind == NativeContributionFormKind.Editor) ChangeEditorViewModel(slug, section, requestId, repository, catalogs, account) else null
    private var owner = account.currentAccountKey()
    private var version = 0
    private var closed = false
    private var dirty = false
    private var photoRevision = 0
    private var photoDelivery: ((List<PendingPhoto>) -> Unit)? = null
    private val deliveries = mutableMapOf<Boolean, ContributionPhotoDelivery>()
    private var selection: ContributionMapPickerState? = null
    private var mapJob: Job? = null
    private val changes = MutableStateFlow(0)

    init {
        listOfNotNull(shop, roaster, suggest, editor).forEachIndexed { index, model -> store.put("form:$index", model) }
        scope.launch { account.accountKey.distinctUntilChanged().collect { resetAccount(it) } }
        shop?.let { model -> scope.launch { model.state.collect { syncMap(it.mapPickerOpen) } } }
    }

    fun current(): NativeContributionFormState {
        if (closed || owner != account.currentAccountKey()) return NativeContributionFormState(kind.name, version, -1, true)
        val state = shop?.state?.value?.nativeForm(version) ?: roaster?.state?.value?.nativeForm(version)
            ?: suggest?.state?.value?.nativeForm(version) ?: checkNotNull(editor).state.value.nativeForm(version, requestId != null)
        return state.copy(hasEdits = dirty, map = selection?.state?.let {
            NativeContributionMap(it.point?.latitude, it.point?.longitude, it.cameraTarget?.latitude,
                it.cameraTarget?.longitude, it.address, it.isResolving, it.failed, it.hasCurrentAddress && !it.isResolving)
        })
    }

    fun observe(onState: () -> Unit): Job = scope.launch {
        launch { changes.collect { onState() } }
        shop?.let { launch { it.state.collect { onState() } } }
        roaster?.let { launch { it.state.collect { onState() } } }
        suggest?.let { launch { it.state.collect { onState() } } }
        editor?.let { launch { it.state.collect { onState() } } }
    }

    fun edit(field: String, value: String) {
        if (!canInteract()) return
        markEdited()
        shop?.let { editShop(it, field, value) }
        roaster?.let { editRoaster(it, field, value) }
        editor?.let { editEditor(it, field, value) }
    }

    fun toggle(group: String, id: String) {
        if (!canInteract() || current().groups.none { it.key == group && it.options.any { option -> option.id == id } }) return
        markEdited()
        shop?.let { toggleShop(it, group, id) }
        if (group == "city") roaster?.onAction(AddRoasterAction.City(id))
        editor?.onAction(ChangeEditorAction.Toggle(id))
    }

    fun control(action: String) {
        if (!isCurrent()) return
        when (action) {
            "next", "submit" -> submit()
            "previous" -> shop?.onAction(AddShopAction.Previous)
            "retry" -> retry()
            "dismissFailure" -> dismissFailure()
            "openMap" -> shop?.onAction(AddShopAction.OpenMap)
            "dismissMap" -> shop?.onAction(AddShopAction.DismissMap)
            "confirmMap" -> confirmMap()
        }
    }

    fun consumeBack(): Boolean {
        if (!isCurrent()) return false
        if (current().submit == "failed") {
            dismissFailure()
            return true
        }
        val state = shop?.state?.value ?: return false
        if (state.step == AddShopStep.Basic || state.submit != SubmitState.Idle) return false
        shop.onAction(AddShopAction.Previous)
        return true
    }

    fun selectedSection(value: String): ChangeSection? {
        val state = current()
        if (suggest == null || !isCurrent() || state.loading || value !in state.sections) return null
        return ChangeSection.entries.firstOrNull { it.name == value }
    }

    fun schedule(action: String, day: Int, value: Int) {
        if (!canInteract()) return
        markEdited()
        val model = shop ?: return
        when (action) {
            "perDay" -> model.onAction(AddShopAction.PerDay(value != 0))
            "closed" -> model.onAction(AddShopAction.ToggleClosedDay(day))
            "dayOpen" -> model.onAction(AddShopAction.SetDayOpen(day, value != 0))
            "open" -> model.onAction(AddShopAction.AdjustOpen(value))
            "close" -> model.onAction(AddShopAction.AdjustClose(value))
            "dayOpening" -> model.onAction(AddShopAction.AdjustDayOpen(day, value))
            "dayClosing" -> model.onAction(AddShopAction.AdjustDayClose(day, value))
            "preset" -> model.onAction(AddShopAction.Preset(nativeHourText(value), nativeHourText(day)))
        }
    }

    fun menu(slug: String, availability: String?, price: String?) {
        if (!canInteract()) return
        markEdited()
        availability?.let { value -> Availability.entries.firstOrNull { it.name == value }?.let {
            editor?.onAction(ChangeEditorAction.SetAvailability(slug, it))
        } }
        price?.let { editor?.onAction(ChangeEditorAction.SetPrice(slug, it)) }
    }

    fun beginPhotoPick(menu: Boolean): Int {
        if (!canInteract()) return -1
        val delivery = deliveries.getOrPut(menu) { ContributionPhotoDelivery({ remainingPhotos(menu) }, { acceptPhotos(it, menu) }) }
        photoDelivery = delivery.request(remainingPhotos(menu)) ?: return -1
        return ++photoRevision
    }

    fun pickedPhotos(revision: Int, photos: List<PendingPhoto>) {
        val deliver = photoDelivery
        if (!canInteract() || revision != photoRevision || deliver == null) return
        photoDelivery = null
        deliver(photos)
    }

    fun allowsMedia(expectedVersion: Int): Boolean = isCurrent() && expectedVersion == version

    fun localPhoto(key: String, expectedVersion: Int): ByteArray? {
        if (!allowsMedia(expectedVersion)) return null
        return listOf(false, true).firstNotNullOfOrNull { menu ->
            pendingPhotos(menu).mapIndexedNotNull { index, photo -> photo.takeIf { it.nativePhotoKey(index, menu) == key } }.firstOrNull()?.bytes?.copyOf()
        }
    }

    fun removePhoto(key: String) {
        if (!canInteract()) return
        val photo = current().photos.firstOrNull { it.key == key } ?: return
        markEdited()
        val pending = pendingPhotos(photo.menu).mapIndexed { index, value -> index to value.nativePhotoKey(index, photo.menu) }
            .firstOrNull { it.second == key }?.first ?: -1
        if (pending >= 0) removePendingPhoto(pending, photo.menu) else removeStoredPhoto(photo)
    }

    fun locationGranted(map: Boolean) {
        if (!isCurrent()) return
        if (!map) shop?.onAction(AddShopAction.LocationGranted)
        else {
            val expected = selection
            val epoch = version
            scope.launch {
                location.lastKnown()?.let { point ->
                    if (isCurrent() && version == epoch && selection === expected) expected?.showLocation(point)
                }
            }
        }
    }

    fun locationDenied() { if (isCurrent()) shop?.onAction(AddShopAction.LocationDenied) }
    fun moveMap(latitude: Double, longitude: Double) { if (isCurrent()) selection?.move(GeoPoint(latitude, longitude)) }
    fun cameraApplied() { selection?.cameraApplied() }

    fun close() {
        if (closed) return
        closed = true
        deliveries.values.forEach(ContributionPhotoDelivery::dispose)
        photoDelivery = null
        scope.cancel()
        store.clear()
    }

    private fun isCurrent(): Boolean = !closed && owner != null && owner == account.currentAccountKey()
    private fun canInteract(): Boolean = isCurrent() && current().submit == "idle"

    private fun resetAccount(value: String?) {
        owner = value
        version++
        dirty = false
        photoRevision++
        deliveries.values.forEach(ContributionPhotoDelivery::dispose)
        deliveries.clear()
        photoDelivery = null
        selection = null
        mapJob?.cancel()
        changes.value++
    }

    private fun syncMap(open: Boolean) {
        if (open && selection == null) {
            val state = checkNotNull(shop).state.value
            val picker = ContributionMapPickerState(state.gpsPoint?.let { GeoPoint(it.first, it.second) })
            selection = picker
            mapJob = scope.launch {
                launch { picker.states.map { it.point }.distinctUntilChanged().collectLatest { picker.resolve(location) } }
                picker.states.collect { changes.value++ }
            }
        } else if (!open && selection != null) {
            mapJob?.cancel()
            selection = null
            changes.value++
        }
    }

    private fun submit() {
        shop?.onAction(AddShopAction.Next)
        roaster?.onAction(AddRoasterAction.Submit)
        editor?.onAction(ChangeEditorAction.Submit)
    }

    private fun retry() {
        shop?.onAction(AddShopAction.RetryCatalogs)
        roaster?.onAction(AddRoasterAction.RetryCities)
        editor?.onAction(ChangeEditorAction.Reload)
    }

    private fun dismissFailure() {
        shop?.onAction(AddShopAction.DismissFailure)
        roaster?.onAction(AddRoasterAction.DismissFailure)
        editor?.onAction(ChangeEditorAction.DismissFailure)
    }

    private fun confirmMap() {
        selection?.selection()?.let { (point, address) ->
            markEdited()
            shop?.onAction(AddShopAction.MapPicked(point.latitude, point.longitude, address))
        }
    }

    private fun remainingPhotos(menu: Boolean): Int = current().let { state ->
        ((if (menu) state.menuPhotoLimit else state.photoLimit) - state.photos.count { it.menu == menu }).coerceAtLeast(0)
    }

    private fun pendingPhotos(menu: Boolean): List<PendingPhoto> = when {
        shop != null -> if (menu) shop.state.value.menuPhotos else shop.state.value.photos
        roaster != null -> roaster.state.value.photos
        editor != null -> if (menu) editor.state.value.newMenuPhotos else editor.state.value.newPhotos
        else -> emptyList()
    }

    private fun acceptPhotos(photos: List<PendingPhoto>, menu: Boolean) {
        markEdited()
        shop?.onAction(if (menu) AddShopAction.AddMenuPhotos(photos) else AddShopAction.AddPhotos(photos))
        roaster?.onAction(AddRoasterAction.AddPhotos(photos))
        editor?.onAction(if (menu) ChangeEditorAction.AddMenuPhotos(photos) else ChangeEditorAction.AddPhotos(photos))
    }

    private fun markEdited() {
        dirty = true
        changes.value++
    }

    private fun removePendingPhoto(index: Int, menu: Boolean) {
        shop?.onAction(if (menu) AddShopAction.RemoveMenuPhoto(index) else AddShopAction.RemovePhoto(index))
        roaster?.onAction(AddRoasterAction.RemovePhoto(index))
        editor?.onAction(if (menu) ChangeEditorAction.RemoveNewMenuPhoto(index) else ChangeEditorAction.RemoveNewPhoto(index))
    }

    private fun removeStoredPhoto(photo: NativeContributionPhoto) {
        if (photo.key.startsWith("uploaded:")) editor?.onAction(
            if (photo.menu) ChangeEditorAction.RemoveUploadedMenuPhotos else ChangeEditorAction.RemoveUploadedPhotos)
        else editor?.onAction(if (photo.menu) ChangeEditorAction.RemoveRetainedMenuPhoto(photo.key.substringAfter("retained:${photo.menu}:"))
            else ChangeEditorAction.RemoveRetainedPhoto(photo.key.substringAfter("retained:${photo.menu}:")))
    }
}

private fun nativeHourText(hour: Int): String = hour.coerceIn(0, 23).toString().padStart(2, '0') + ":00"

