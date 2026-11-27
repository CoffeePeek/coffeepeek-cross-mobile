package com.coffeepeek.feature.shop.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shop.api.ShopCheckInAnalytics
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInPublicationReader
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.impl.ui.ShopCheckInCreateViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlin.time.Clock
import kotlin.time.Instant

/** Native rendering observes the same draft, validation and delivery state machine as Compose. */
class NativeShopCheckInSession(
    initial: ShopCheckInCreateInput,
    repository: ShopCheckInRepository,
    drafts: ShopCheckInDraftStore,
    analytics: ShopCheckInAnalytics = ShopCheckInAnalytics.None,
    publication: ShopCheckInPublicationReader = ShopCheckInPublicationReader.None,
    now: () -> Instant = { Clock.System.now() },
) {
    private val model = ShopCheckInCreateViewModel(initial, repository, drafts, now, analytics, publication)
    private val store = ViewModelStore().apply { put("native-shop-check-in", model) }
    private val closed = MutableStateFlow(false)
    private val empty = ShopCheckInFormState(initial.copy(text = "", rating = ShopRating(0, 0, 0), drinkSlug = null,
        customDrinkName = null, photos = emptyList()), isLoading = false)
    val state = combine(model.state, closed) { state, closed -> NativeShopCheckInSnapshot(if (closed) empty else state, !closed) }
    val events = model.events.filter { !closed.value }.map { it.nativeEvent() }

    fun current() = NativeShopCheckInSnapshot(if (closed.value) empty else model.state.value, !closed.value)
    fun noteChanged(value: String) = send(ShopCheckInFormAction.TextChanged(value))
    fun ratingChanged(place: Int, service: Int, coffee: Int) = send(ShopCheckInFormAction.RatingChanged(ShopRating(place, service, coffee)))
    fun visitChanged(iso: String) = send(ShopCheckInFormAction.VisitDateChanged(iso))
    fun visibilityChanged(public: Boolean) = send(ShopCheckInFormAction.VisibilityChanged(if (public) ShopCheckInVisibility.Public else ShopCheckInVisibility.Private))
    fun drinkChanged(slug: String?) = send(ShopCheckInFormAction.DrinkChanged(slug))
    fun photosAdded(photos: List<ShopCheckInPhoto>) = send(ShopCheckInFormAction.PhotosAdded(photos))
    fun removePhoto(index: Int) = send(ShopCheckInFormAction.RemovePhoto(index))
    fun movePhoto(from: Int, to: Int) = send(ShopCheckInFormAction.MovePhoto(from, to))
    fun retryDrinks() = send(ShopCheckInFormAction.RetryDrinks)
    fun submit() = send(ShopCheckInFormAction.Submit)
    fun changeShop() = send(ShopCheckInFormAction.ChangeShop)
    fun dismiss() = send(ShopCheckInFormAction.Dismiss)
    fun askDiscard() = send(ShopCheckInFormAction.AskDiscard)
    fun cancelDiscard() = send(ShopCheckInFormAction.CancelDiscard)
    fun discard() = send(ShopCheckInFormAction.Discard)
    fun historyChecked() = send(ShopCheckInFormAction.HistoryChecked)
    fun openHistory() = send(ShopCheckInFormAction.OpenHistory)
    fun goToFeed() = send(ShopCheckInFormAction.GoToFeed)

    fun close() {
        if (closed.value) return
        closed.value = true
        store.clear()
    }

    private fun send(action: ShopCheckInFormAction) {
        if (!closed.value) model.onAction(action)
    }
}

class NativeShopCheckInSnapshot internal constructor(state: ShopCheckInFormState, val active: Boolean) {
    val input = state.input
    val loading = state.isLoading
    val draftFailed = state.draftFailed
    val hasDraft = state.hasDraft
    val confirmDiscard = state.confirmDiscard
    val discarding = state.isDiscarding
    val changingShop = state.isChangingShop
    val drinks = state.drinks
    val drinksLoading = state.drinksLoading
    val drinksFailed = state.drinksFailed
    val submitting = state.isSubmitting
    val submitted = state.submitted
    val publication = state.publication
    val deliveryUnconfirmed = state.deliveryUnconfirmed
    val failure = state.submitFailure
    val validation = state.validationError
    val editable = active && !loading && !submitting && !discarding && !submitted && !deliveryUnconfirmed && !confirmDiscard && !changingShop
}

enum class NativeShopCheckInEvent { ChangeShop, Dismiss, Submitted, OpenHistory, GoToFeed }

private fun ShopCheckInFormEvent.nativeEvent() = when (this) {
    ShopCheckInFormEvent.ChangeShop -> NativeShopCheckInEvent.ChangeShop
    ShopCheckInFormEvent.Dismiss -> NativeShopCheckInEvent.Dismiss
    is ShopCheckInFormEvent.Submitted -> NativeShopCheckInEvent.Submitted
    ShopCheckInFormEvent.OpenHistory -> NativeShopCheckInEvent.OpenHistory
    ShopCheckInFormEvent.GoToFeed -> NativeShopCheckInEvent.GoToFeed
}
