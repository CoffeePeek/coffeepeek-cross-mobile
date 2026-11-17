package com.coffeepeek.admin.di.shop

import com.coffeepeek.feature.shop.impl.ShopCheckInListRow
import com.coffeepeek.feature.shop.impl.ShopCheckInListSnapshot

class IosShopCheckInListState internal constructor(
    val generation: Int,
    snapshot: ShopCheckInListSnapshot? = null,
    val observationFailed: Boolean = false,
) {
    val isLoading = snapshot?.isLoading ?: !observationFailed
    val hasLoaded = snapshot?.hasLoaded == true
    val hasLoadError = observationFailed || snapshot?.hasLoadError == true
    val hasVoteError = snapshot?.hasVoteError == true
    val shopTitle = snapshot?.shopTitle.orEmpty()
    val checkInCount = snapshot?.checkInCount ?: 0
    val overall = snapshot?.overall.orEmpty()
    val isRatingSample = snapshot?.isRatingSample == true
    val breakdown = snapshot?.breakdown.orEmpty().map { IosShopCheckInRating(it.value, it.label) }
    val rows = snapshot?.rows.orEmpty().map(::IosShopCheckInListRow)
    val showGuestPrompt = snapshot?.showGuestPrompt == true
    val hiddenCount = snapshot?.hiddenCount ?: 0
}

class IosShopCheckInRating internal constructor(val value: Double, val label: String)

class IosShopCheckInListRow internal constructor(row: ShopCheckInListRow) {
    private val source = row.checkIn
    val id = source.id
    val authorName = source.username
    val hasAvatar = !source.authorAvatarUrl.isNullOrBlank()
    val date = source.createdAt
    val note = source.note
    val drink = listOf(source.customDrinkName, source.drinkNameRu, source.drinkNameEn)
        .firstOrNull { !it.isNullOrBlank() }
    val ratingLabel = row.ratingLabel
    val coffeeRating = source.rating?.coffee ?: 0
    val serviceRating = source.rating?.service ?: 0
    val placeRating = source.rating?.place ?: 0
    val helpfulCount = source.helpfulCount
    val markedHelpful = source.isHelpfulByCurrentUser
    val isOwn = row.isOwn
    val isVoting = row.isVoting
    val canVote = row.canVote
    val canReport = row.canReport
    val canBlock = row.canBlock
    val canOpenAuthor = row.canOpenAuthor
    val showHelpful = row.showHelpful
    val photoCount = source.photoUrls.size
    fun canOpenPhoto(index: Int) = !source.photoUrls.getOrNull(index).isNullOrBlank()
}
