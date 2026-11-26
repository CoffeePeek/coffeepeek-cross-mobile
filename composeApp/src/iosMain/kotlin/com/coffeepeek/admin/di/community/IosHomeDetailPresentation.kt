package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.homecheckin.api.*

/** Mechanical adaptation between independent feature UI contracts; all labels/policy arrive resolved. */
internal fun HomeCheckInDetailPresentation.nativeDetail(callbacks: HomeCheckInDetailCallbacks): NativeCheckInDetailPresentation {
    return NativeCheckInDetailPresentation(card?.nativeCard(this, callbacks), title, backLabel, active, dismissAllowed,
        loadingLabel, errorText, retryLabel, notice,
        confirmation = confirmation?.let { IosCheckInDetailConfirmation(it.title, it.body, it.confirmLabel, it.cancelLabel) })
}

private fun HomeCheckInDetailCard.nativeCard(detail: HomeCheckInDetailPresentation, callbacks: HomeCheckInDetailCallbacks) = CommunityCardPresentation(
    key = key, header = CommunityCardHeader(name, null, dateLabel, ""),
    body = CommunityCardBody(true, beanName, note, rating?.toDouble(), ratingLabel, beanName, roasterName, bagUrl, recipe,
        photos.map { CommunityCardPhoto(it.previewUrl, it.detailUrl, it.label) }, " · ", ""),
    footer = CommunityCardFooter(null, false, "", drink, null),
    status = CommunityCardStatus(publication, rejectedTitle, rejection, null),
    menu = listOf(CommunityCardMenuItem("edit", detail.editLabel, "edit" in callbacks.actions),
        CommunityCardMenuItem("delete", detail.deleteLabel, "delete" in callbacks.actions, destructive = true)),
    openLabel = "", menuLabel = detail.menuLabel, horizontalInset = 16.0,
)
