@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.feature.homecheckin.impl.*
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

internal fun NativeHomeCheckInState.iosState() = IosHomeCheckInState(
    draft.iosDraft(), status.iosStatus(), catalog.iosCatalog(),
    IosHomeCheckInIssues(issues.loadKey, issues.saveKey, issues.fields.map { IosHomeCheckInFieldIssue(it.field, it.messageKey) }),
    saved?.let { IosHomeCheckInSaved(it.id, it.isPublic, it.moderation, it.visibilityUnconfirmed) },
)

private fun NativeHomeCheckInDraft.iosDraft() = IosHomeCheckInDraft(
    preparedOn, dateLabel, methodCode, dose, durationSeconds, tasteScore, note, drinkName, isPublic,
    beanName, shelfBagId, showShelfBag, shelfBagCardUrl, photos.map { it.iosPhoto() }, existingPhotoUrls, customBeanName,
)

private fun NativeHomeCheckInStatus.iosStatus() = IosHomeCheckInStatus(
    editing, ready, loading, saving, closed, revoked, canEdit, canAddPhotos, dismissAllowed, photosPreparing, saveAttempt,
)

private fun NativeHomeCheckInCatalog.iosCatalog() = IosHomeCheckInCatalog(
    beanPickerOpen, beanQuery, methods.map { it.iosOption() }, beans.map { it.iosOption() },
    shelfBags.map { it.iosOption() }, methodsLoading, beansLoading, methodsIssueKey, beansIssueKey,
)

private fun NativeHomeCheckInOption.iosOption() = IosHomeCheckInOption(id, name, detail, imageUrl)

private fun NativeHomeCheckInPhoto.iosPhoto(): IosCheckInPhoto {
    val data = if (bytes.isEmpty()) NSData() else bytes.usePinned { NSData.create(it.addressOf(0), bytes.size.toULong()) }
    return IosCheckInPhoto(fileName, contentType, data)
}
