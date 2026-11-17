package com.coffeepeek.feature.shop.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.shop.api.ShopCheckInAuthor
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.ui.ShopCheckInListViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListState
import com.coffeepeek.feature.shop.impl.ui.data.blockableAuthor
import com.coffeepeek.feature.shop.impl.ui.data.formatShopRating
import kotlinx.coroutines.launch

fun createIosShopCheckInListSession(
    shopId: String,
    details: ShopDetailsRepository,
    votes: ShopCheckInVoteRepository,
    currentViewer: suspend () -> Result<ShopViewer>,
    generation: Int,
    store: ViewModelStore,
): IosShopCheckInListSession {
    val model = ShopCheckInListViewModel(shopId, details, votes, currentViewer, generation)
    store.put("ios-shop-check-in-list", model)
    return IosShopCheckInListSession(model)
}

class ShopCheckInListSnapshot internal constructor(state: ShopCheckInListState) {
    val isLoading = state.isLoading
    val hasLoaded = state.hasLoaded
    val hasLoadError = state.hasLoadError
    val hasVoteError = state.hasVoteError
    val shopTitle = state.shopTitle
    val checkInCount = maxOf(state.checkInCount, state.ratingSampleSize)
    val isRatingSample = state.checkInCount > state.ratingSampleSize
    val overall = formatShopRating(state.overallRating?.takeIf { it > 0 && it.isFinite() }
        ?: state.ratingSummary?.overall ?: 0.0)
    val breakdown = state.ratingSummary?.let {
        listOf(ShopCheckInRatingValue(it.coffee), ShopCheckInRatingValue(it.service), ShopCheckInRatingValue(it.place))
    }.orEmpty()
    val rows = state.checkIns.map { ShopCheckInListRow(it, state) }
    val hiddenCount = state.hiddenCheckInCount
    val showGuestPrompt = !state.isLoggedIn && rows.isNotEmpty()
}

class ShopCheckInRatingValue internal constructor(val value: Double) {
    val label = formatShopRating(value)
}

class ShopCheckInListRow internal constructor(val checkIn: ShopCheckIn, state: ShopCheckInListState) {
    val isOwn = state.owns(checkIn)
    val canVote = state.pendingVoteId == null && !state.isLoading
    val isVoting = state.pendingVoteId == checkIn.id
    val canReport = !isOwn
    val canBlock = !state.isLoading && checkIn.blockableAuthor(isOwn, state.isLoggedIn, state.currentUserId) != null
    val canOpenAuthor = !checkIn.authorSlug.isNullOrBlank()
    val showHelpful = checkIn.visibility == ShopCheckInVisibility.Public || checkIn.helpfulCount > 0
    val ratingLabel = checkIn.rating?.let { formatShopRating(it.average) }
    fun thumbnail(index: Int): String? =
        (checkIn.photoThumbnailUrls.getOrNull(index) ?: checkIn.photoUrls.getOrNull(index))?.takeIf(String::isNotBlank)
}

interface ShopCheckInListCallbacks {
    fun back()
    fun signIn()
    fun register()
    fun report(id: String)
    fun author(slug: String, nickname: String, avatar: String?)
    fun block(author: ShopCheckInAuthor)
    fun photo(urls: List<String>, originals: List<String>, index: Int)
}

class IosShopCheckInListSession internal constructor(private val model: ShopCheckInListViewModel) {
    fun current() = ShopCheckInListSnapshot(model.state.value)

    fun observeState(receive: (ShopCheckInListSnapshot) -> Unit) = model.viewModelScope.launch {
        model.state.collect { receive(ShopCheckInListSnapshot(it)) }
    }

    fun observeEvents(callbacks: ShopCheckInListCallbacks) = model.viewModelScope.launch {
        model.events.collect { envelope ->
            when (val event = envelope.event) {
                ShopCheckInListEvent.Back -> callbacks.back()
                ShopCheckInListEvent.SignIn -> callbacks.signIn()
                ShopCheckInListEvent.Register -> callbacks.register()
                is ShopCheckInListEvent.Report -> callbacks.report(event.checkInId)
                is ShopCheckInListEvent.OpenAuthor -> callbacks.author(event.slug, event.nickname, event.avatarUrl)
                is ShopCheckInListEvent.BlockAuthor -> callbacks.block(event.author)
                is ShopCheckInListEvent.OpenPhoto -> callbacks.photo(event.urls, event.originals, event.index)
            }
        }
    }

    fun resume() = model.onAction(ShopCheckInListAction.Resume)
    fun retry() = model.onAction(ShopCheckInListAction.Retry)
    fun signIn() = model.onAction(ShopCheckInListAction.SignIn)
    fun register() = model.onAction(ShopCheckInListAction.Register)
    fun vote(id: String) = model.onAction(ShopCheckInListAction.ToggleHelpful(id))
    fun report(id: String) = model.onAction(ShopCheckInListAction.Report(id))
    fun author(id: String) = model.onAction(ShopCheckInListAction.OpenAuthor(id))
    fun block(id: String) = model.onAction(ShopCheckInListAction.BlockAuthor(id))
    fun photo(id: String, index: Int) = model.onAction(ShopCheckInListAction.OpenPhoto(id, index))
}
