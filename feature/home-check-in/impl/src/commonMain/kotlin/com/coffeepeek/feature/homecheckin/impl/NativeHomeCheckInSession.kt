package com.coffeepeek.feature.homecheckin.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationVisibility
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.HomeCheckInEditorViewModel
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/** A native presentation owns this session; the existing feature ViewModel owns all edits and writes. */
class NativeHomeCheckInSession(
    repository: HomePreparationRepository, catalog: HomeCheckInEditorCatalog,
    private val dates: HomeCheckInEditorDates, private val canAccess: () -> Boolean,
    private val preparationId: String? = null, sessionGeneration: Int = 0,
    bean: HomeCheckInBean? = null, analytics: HomeCheckInEditorAnalytics = HomeCheckInEditorAnalytics.None,
) {
    private val instant = dates.currentInstant()
    private val model = HomeCheckInEditorViewModel(repository, catalog, instant, dates.localDate(instant),
        preparationId, sessionGeneration, canAccess, analytics = analytics,
        startBean = bean?.let { HomeCheckInStartBean(it.slug, it.name, it.shelfBagId) })
    private val store = ViewModelStore().apply { put("native-home-editor", model) }
    private val closed = MutableStateFlow(false)
    private var photoRevision = 0L
    private var photoDelivery: ((List<HomePreparationUpload>) -> Unit)? = null
    private val preparing = MutableStateFlow(false)
    private val delivery = HomeCheckInPhotoDelivery(::canReceivePhotos) { dispatch(HomeCheckInEditorAction.PhotosAdded(it)) }

    val state = combine(model.state, preparing, closed) { value, loading, _ -> visible(value).nativeSnapshot(preparationId != null, dates, loading) }
    val events = model.events.filter { !closed.value && model.canDeliver(it) }.map { it.event.nativeEvent() }

    fun current() = visible(model.state.value).nativeSnapshot(preparationId != null, dates, preparing.value)
    fun isCurrent(): Boolean = !closed.value && canAccess() && !model.state.value.isAccessRevoked
    fun today(): String = dates.localDate(dates.currentInstant())
    fun retryDetail() = dispatch(HomeCheckInEditorAction.RetryDetail)
    fun retryMethods() = dispatch(HomeCheckInEditorAction.RetryMethods)
    fun methodChanged(value: String?) = dispatch(HomeCheckInEditorAction.MethodChanged(value))
    fun doseChanged(value: String) = dispatch(HomeCheckInEditorAction.DoseChanged(value))
    fun durationChanged(seconds: Int?) = dispatch(HomeCheckInEditorAction.DurationChanged(seconds))
    fun tasteTapped(score: Int) = dispatch(HomeCheckInEditorAction.TasteRatingTapped(score))
    fun noteChanged(value: String) = dispatch(HomeCheckInEditorAction.NoteChanged(value))
    fun drinkSelected(slug: String?) = dispatch(HomeCheckInEditorAction.DrinkSelected(slug))
    fun retryDrinks() = dispatch(HomeCheckInEditorAction.RetryDrinks)
    fun visibilityChanged(isPublic: Boolean) = dispatch(HomeCheckInEditorAction.VisibilityChanged(
        if (isPublic) HomePreparationVisibility.Public else HomePreparationVisibility.Private))
    fun openBeanPicker() = dispatch(HomeCheckInEditorAction.OpenBeanPicker)
    fun closeBeanPicker() = dispatch(HomeCheckInEditorAction.CloseBeanPicker)
    fun beanQueryChanged(value: String) = dispatch(HomeCheckInEditorAction.BeanQueryChanged(value))
    fun searchBeans() = dispatch(HomeCheckInEditorAction.SearchBeans)
    fun beanSelected(slug: String) = dispatch(HomeCheckInEditorAction.BeanSelected(slug))
    fun customBeanChosen(name: String) = dispatch(HomeCheckInEditorAction.CustomBeanChosen(name))
    fun shelfBagSelected(id: String) = dispatch(HomeCheckInEditorAction.ShelfBagSelected(id))
    fun showShelfBagChanged(show: Boolean) = dispatch(HomeCheckInEditorAction.ShowShelfBagChanged(show))
    fun removePhoto(index: Int) = dispatch(HomeCheckInEditorAction.RemovePhoto(index))
    fun movePhoto(from: Int, to: Int) = dispatch(HomeCheckInEditorAction.MovePhoto(from, to))
    fun finishSaved() = dispatch(HomeCheckInEditorAction.FinishSaved)

    fun dateChanged(civilDate: String) {
        if (civilDate == model.state.value.draft.preparedOn || !current().status.canEdit) return
        dispatch(HomeCheckInEditorAction.DateChosen(civilDate, dates.civilDateToInstant(civilDate)))
    }

    fun save() {
        if (!preparing.value) dispatch(HomeCheckInEditorAction.Save)
    }

    fun requestClose() {
        cancelPhotos(photoRevision)
        dispatch(HomeCheckInEditorAction.Close)
    }

    fun beginPhotos(limit: Int = 5): Long {
        if (preparing.value) return 0L
        val remaining = (5 - model.state.value.draft.photos.size).coerceAtMost(limit)
        photoDelivery = delivery.request(remaining) ?: return 0L
        preparing.value = true
        return ++photoRevision
    }

    fun deliverPhotos(requestId: Long, photos: List<NativeHomeCheckInPhoto>) {
        if (requestId != photoRevision || !preparing.value) return
        val callback = photoDelivery
        photoDelivery = null
        preparing.value = false
        callback?.invoke(photos.map { HomePreparationUpload(it.fileName, it.contentType, it.bytes.copyOf()) })
    }

    fun cancelPhotos(requestId: Long) {
        if (requestId != photoRevision) return
        photoRevision++
        photoDelivery = null
        preparing.value = false
    }

    fun revokeAccess() {
        cancelPhotos(photoRevision)
        model.onAction(HomeCheckInEditorAction.RevokeAccess)
    }

    fun close() {
        if (closed.value) return
        closed.value = true
        cancelPhotos(photoRevision)
        delivery.dispose()
        store.clear()
    }

    private fun canReceivePhotos(): Boolean = !closed.value && canAccess() && preparationId == null && model.state.value.let {
        it.isReady && !it.isSaving && !it.isLoading && it.savedEntry == null && !it.isClosed && !it.isAccessRevoked
    }

    private fun dispatch(action: HomeCheckInEditorAction) {
        if (!closed.value && canAccess()) model.onAction(action)
    }

    private fun visible(value: HomeCheckInEditorState): HomeCheckInEditorState = when {
        closed.value -> HomeCheckInEditorState(isClosed = true)
        !canAccess() -> HomeCheckInEditorState(isAccessRevoked = true)
        else -> value
    }
}

private fun HomeCheckInEditorEvent.nativeEvent() = when (this) {
    HomeCheckInEditorEvent.Close -> NativeHomeCheckInEvent("close")
    is HomeCheckInEditorEvent.Saved -> NativeHomeCheckInEvent("saved", preparationId, visibilityUnconfirmed)
}
