@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.shop

import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_CHECK_IN_PHOTOS
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.impl.NativeShopCheckInEvent
import com.coffeepeek.feature.shop.impl.NativeShopCheckInSession
import com.coffeepeek.feature.shop.impl.NativeShopCheckInSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPickerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import platform.UIKit.UIImage

/** The composition owns this form and its existing account-bound dependencies. */
class IosShopCheckInEditor internal constructor(
    private val form: ShopCheckInNativeForm,
    private val language: LanguagePort,
    private val picker: () -> ShopCheckInPhotoPickerController?,
    private val callbacks: () -> ShopCheckInNativeCallbacks,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val presentation = NativeShopCheckInSession(
        ShopCheckInCreateInput(form.shopSlug, "", ShopRating(4, 4, 4), form.dates.visitInstant(form.dates.nowMillis())),
        form.repository, form.drafts, AppShopCheckInAnalytics(form.isCurrent), form.publication,
    )
    private val preparing = MutableStateFlow(false)
    private val imageCache = mutableMapOf<ShopCheckInPhoto, UIImage?>()
    private var closed = false
    private var photoRequest = 0

    init { scope.launch { presentation.events.collect(::handleEvent) } }

    fun current() = presentation.current().toIos()
    fun languageTag() = language.current().languageTag
    fun observe(onState: (IosShopCheckInState) -> Unit, onLanguage: (String) -> Unit) = IosShopCheckInObservation(
        scope.launch {
            combine(presentation.state, preparing, language.observe()) { state, _, locale ->
                onLanguage(locale.languageTag)
                state.toIos()
            }.collect { onState(it) }
        },
    )

    fun noteChanged(value: String) = action { presentation.noteChanged(value) }
    fun ratingChanged(place: Int, service: Int, coffee: Int) = action { presentation.ratingChanged(place, service, coffee) }
    fun visibilityChanged(isPublic: Boolean) = action { presentation.visibilityChanged(isPublic) }
    fun drinkChanged(slug: String?) = action { presentation.drinkChanged(slug) }
    fun customDrinkChanged(value: String) = action { presentation.customDrinkChanged(value) }
    fun retryDrinks() = action(presentation::retryDrinks)
    fun removePhoto(index: Int) = action { presentation.removePhoto(index) }
    fun movePhoto(from: Int, to: Int) = action { presentation.movePhoto(from, to) }
    fun submit() { if (picker()?.isPreparing != true) action(presentation::submit) }
    fun dismiss() = action(presentation::dismiss)
    fun askDiscard() = action(presentation::askDiscard)
    fun cancelDiscard() = action(presentation::cancelDiscard)
    fun discard() = action(presentation::discard)
    fun historyChecked() = action(presentation::historyChecked)
    fun openHistory() = action(presentation::openHistory)
    fun goToFeed() = action(presentation::goToFeed)
    fun changeShop() { if (current().editable) action(callbacks().changeShop) }

    fun dateChanged(civilDate: String) {
        val millis = try { LocalDate.parse(civilDate).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }
            catch (_: IllegalArgumentException) { return }
        action { presentation.visitChanged(form.dates.visitInstant(millis, presentation.current().input.visitedAtIso)) }
    }

    fun choosePhotos(camera: Boolean) {
        val controller = picker() ?: return
        val state = current()
        val remaining = MAX_SHOP_CHECK_IN_PHOTOS - state.photos.size
        if (!state.editable || controller.isPreparing || remaining <= 0) return
        val request = ++photoRequest
        val deliver: (List<ShopCheckInPhoto>) -> Unit = { photos ->
            if (isCurrent() && request == photoRequest && presentation.current().editable) presentation.photosAdded(photos)
        }
        if (camera) controller.takePhoto(deliver) else controller.pickFromGallery(remaining, deliver)
    }

    internal fun syncPicker() { preparing.value = picker()?.isPreparing == true }

    fun close() {
        if (closed) return
        closed = true
        photoRequest++
        scope.cancel()
        presentation.close()
        imageCache.clear()
    }

    private fun isCurrent() = !closed && form.isCurrent()

    private fun action(block: () -> Unit) { if (isCurrent()) block() }

    private fun handleEvent(event: NativeShopCheckInEvent) {
        if (!isCurrent()) return
        when (event) {
            NativeShopCheckInEvent.Dismiss -> callbacks().dismiss()
            NativeShopCheckInEvent.Submitted -> callbacks().submitted()
            NativeShopCheckInEvent.OpenHistory -> callbacks().openHistory()
            NativeShopCheckInEvent.GoToFeed -> callbacks().goToFeed()
        }
    }

    private fun NativeShopCheckInSnapshot.toIos(): IosShopCheckInState {
        val english = languageTag().substringBefore('-') == "en"
        return IosShopCheckInState(form.shopName, form.shopAddress, input.text,
            input.rating.place, input.rating.service, input.rating.coffee,
            civilDate(form.dates.pickerMillis(input.visitedAtIso)), civilDate(form.dates.nowMillis()), form.dates.label(input.visitedAtIso),
            input.visibility == ShopCheckInVisibility.Public, input.drinkSlug, input.customDrinkName.orEmpty(),
            drinks.map { IosShopCheckInDrink(it.slug, (if (english) it.nameEn.ifBlank { it.nameRu } else it.nameRu.ifBlank { it.nameEn }).ifBlank { it.slug }) },
            photos(input.photos), active && isCurrent(), loading, editable && isCurrent() && !preparing.value,
            preparing.value, draftFailed, hasDraft, confirmDiscard, discarding, drinksLoading, drinksFailed,
            submitting, submitted, publication.name, deliveryUnconfirmed, failure?.shopMessageKey(),
            failure?.serverMessage, validation?.shopMessageKey())
    }

    private fun photos(items: List<ShopCheckInPhoto>): List<IosShopCheckInPhoto> {
        imageCache.keys.retainAll(items.toSet())
        return items.mapIndexed { index, photo ->
            IosShopCheckInPhoto(index, imageCache.getOrPut(photo) { photo.nativeImage() }, photo.fileName)
        }
    }
}

private fun civilDate(millis: Long?): String = millis?.let {
    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()
}.orEmpty()
