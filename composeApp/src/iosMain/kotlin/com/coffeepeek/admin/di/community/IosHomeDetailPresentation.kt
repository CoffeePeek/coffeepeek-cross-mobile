package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.homecheckin.api.*

/** Mechanical adaptation between independent feature UI contracts; all labels/policy arrive resolved. */
internal fun HomeCheckInDetailPresentation.nativeDetail(callbacks: HomeCheckInDetailCallbacks): NativeCheckInDetailPresentation {
    val controls = if (card == null) emptyList() else listOf(
        IosCheckInDetailAction("edit", editLabel, "edit" in callbacks.actions, false),
        IosCheckInDetailAction("delete", deleteLabel, "delete" in callbacks.actions, true))
    return NativeCheckInDetailPresentation(card?.nativeCard(), title, backLabel, active, dismissAllowed,
        loadingLabel, errorText, retryLabel, notice, controls,
        confirmation?.let { IosCheckInDetailConfirmation(it.title, it.body, it.confirmLabel, it.cancelLabel) })
}

private fun HomeCheckInDetailCard.nativeCard() = CommunityCardPresentation(
    key = key, header = CommunityCardHeader(name, null, dateLabel, ""),
    body = CommunityCardBody(true, beanName, note, rating?.toDouble(), ratingLabel, beanName, roasterName, bagUrl, recipe,
        photos.map { CommunityCardPhoto(it.previewUrl, it.detailUrl, it.label) }, " · ", ""),
    footer = CommunityCardFooter(null, false, "", drink, null),
    status = CommunityCardStatus(publication, rejectedTitle, rejection, null),
    menu = emptyList(), openLabel = "", menuLabel = "", horizontalInset = 16.0,
)
