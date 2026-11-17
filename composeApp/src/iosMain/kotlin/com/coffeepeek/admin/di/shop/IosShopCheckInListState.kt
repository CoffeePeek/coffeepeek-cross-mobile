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

class IosShopCheckInListRow internal constructor(private val row: ShopCheckInListRow) {
    val id = row.id
    val authorName = row.authorName
    val hasAvatar = row.hasAvatar
    val date = row.date
    val note = row.note
    val drink = row.drink
    val ratingLabel = row.ratingLabel
    val coffeeRating = row.coffeeRating
    val serviceRating = row.serviceRating
    val placeRating = row.placeRating
    val helpfulCount = row.helpfulCount
    val markedHelpful = row.markedHelpful
    val isOwn = row.isOwn
    val isVoting = row.isVoting
    val canVote = row.canVote
    val canReport = row.canReport
    val canBlock = row.canBlock
    val canOpenAuthor = row.canOpenAuthor
    val showHelpful = row.showHelpful
    val photoCount = row.photoCount
    fun canOpenPhoto(index: Int) = row.canOpenPhoto(index)
}
