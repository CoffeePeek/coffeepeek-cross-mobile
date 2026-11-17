package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.feature.homecheckin.impl.NativeHomeCheckInSession
import com.coffeepeek.feature.homecheckin.impl.NativeHomeCheckInPhoto
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInPhotoPickerController
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import platform.UIKit.UIImage

/** Swift owns subscriptions; the composition entry owns this editor and its feature session. */
class IosHomeCheckInEditor internal constructor(
    private val session: NativeHomeCheckInSession,
    private val language: LanguagePort,
    private val media: IosCatalogueMedia,
    private val imageLease: CheckInHistorySession,
    private val photos: () -> HomeCheckInPhotoPickerController?,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    fun current() = session.current().iosState()
    fun today() = session.today()
    fun languageTag() = language.current().languageTag
    fun observe(onState: (IosHomeCheckInState) -> Unit, onLanguage: (String) -> Unit): IosHomeCheckInObservation {
        return IosHomeCheckInObservation(
            scope.launch { session.state.collect { onState(it.iosState()) } },
            scope.launch { language.observe().collect { onLanguage(it.languageTag); onState(current()) } },
        )
    }

    fun retryDetail() = session.retryDetail()
    fun retryMethods() = session.retryMethods()
    fun methodChanged(value: String?) = session.methodChanged(value)
    fun doseChanged(value: String) = session.doseChanged(value)
    fun durationChanged(seconds: Int?) = session.durationChanged(seconds)
    fun tasteTapped(score: Int) = session.tasteTapped(score)
    fun noteChanged(value: String) = session.noteChanged(value)
    fun drinkChanged(value: String) = session.drinkChanged(value)
    fun visibilityChanged(isPublic: Boolean) = session.visibilityChanged(isPublic)
    fun dateChanged(civilDate: String) = session.dateChanged(civilDate)
    fun openBeanPicker() = session.openBeanPicker()
    fun closeBeanPicker() = session.closeBeanPicker()
    fun beanQueryChanged(value: String) = session.beanQueryChanged(value)
    fun searchBeans() = session.searchBeans()
    fun beanSelected(slug: String) = session.beanSelected(slug)
    fun customBeanChosen(name: String) = session.customBeanChosen(name)
    fun shelfBagSelected(id: String) = session.shelfBagSelected(id)
    fun showShelfBagChanged(show: Boolean) = session.showShelfBagChanged(show)
    fun removePhoto(index: Int) = session.removePhoto(index)
    fun movePhoto(from: Int, to: Int) = session.movePhoto(from, to)
    fun save() = session.save()
    fun finishSaved() = session.finishSaved()
    fun requestClose() = session.requestClose()

    fun choosePhotos(camera: Boolean) {
        val controller = photos()?.takeUnless { it.isPreparing } ?: return
        val state = session.current()
        if (!state.status.canAddPhotos) return
        val remaining = if (camera) 1 else 5 - state.draft.photos.size
        val request = session.beginPhotos(remaining)
        if (request == 0L) return
        val receive: (List<com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload>) -> Unit = { uploads ->
            session.deliverPhotos(request, uploads.map { NativeHomeCheckInPhoto(it.fileName, it.contentType, it.bytes) })
        }
        if (camera) controller.takePhoto(receive) else controller.pickFromGallery(remaining, receive)
    }

    fun loadImage(url: String, onImage: (UIImage?) -> Unit): IosHomeCheckInObservation {
        return IosHomeCheckInObservation(scope.launch {
            if (!session.isCurrent()) return@launch
            val image = imageLease.withCurrentAccount { Result.success(media.loadImage(url)) }.getOrNull()
            if (session.isCurrent()) onImage(image)
        })
    }

    internal fun close() {
        scope.cancel()
        session.close()
    }
}

class IosHomeCheckInObservation internal constructor(private vararg val jobs: Job) {
    fun cancel() = jobs.forEach { it.cancel() }
}
