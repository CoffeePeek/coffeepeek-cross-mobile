package com.coffeepeek.feature.homecheckin.impl

import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInEditorState
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInEditorDates
import com.coffeepeek.feature.homecheckin.impl.ui.data.matching

internal fun HomeCheckInEditorState.nativeSnapshot(editing: Boolean, dates: HomeCheckInEditorDates,
    preparing: Boolean): NativeHomeCheckInState = NativeHomeCheckInState(
    nativeDraft(dates), nativeStatus(editing, preparing), nativeCatalog(), nativeIssues(),
    savedEntry?.let { NativeHomeCheckInSaved(it.id, it.visibility.publicValue(), it.moderation.name, savedVisibilityUnconfirmed) },
)

private fun HomeCheckInEditorState.nativeDraft(dates: HomeCheckInEditorDates) = NativeHomeCheckInDraft(
    preparedOn = draft.preparedOn.orEmpty(), dateLabel = draft.preparedOn?.let(dates.formatDate).orEmpty(),
    methodCode = methods.firstOrNull { it.code == draft.brewMethodCode || draft.brewMethodCode in it.previousCodes }?.code
        ?: draft.brewMethodCode, dose = draft.doseGrams.orEmpty(), durationSeconds = draft.brewDurationSeconds,
    tasteScore = draft.tasteScore, note = draft.text.orEmpty(),
    drinkName = draft.customDrinkName ?: entry?.drinkName.takeIf { draft.drinkSlug != null }.orEmpty(),
    isPublic = draft.visibility.publicValue(), beanName = beanSelectionName ?: draft.customBeanName,
    shelfBagId = draft.shelfBagId, showShelfBag = draft.showShelfBag, shelfBagCardUrl = shelfBagCardUrl,
    photos = draft.photos.map { NativeHomeCheckInPhoto(it.fileName, it.contentType, it.bytes.copyOf()) },
    existingPhotoUrls = entry?.photos.orEmpty().map { it.thumbnailUrl ?: it.url },
    customBeanName = draft.customBeanName,
)

private fun HomeCheckInEditorState.nativeStatus(editing: Boolean, preparing: Boolean): NativeHomeCheckInStatus {
    val editable = isReady && !isLoading && !isSaving && savedEntry == null && !isClosed && !isAccessRevoked
    return NativeHomeCheckInStatus(editing, isReady, isLoading, isSaving, isClosed, isAccessRevoked,
        editable, editable && !editing && !preparing && draft.photos.size < 5,
        !isSaving && savedEntry == null, preparing, saveAttempt)
}

private fun HomeCheckInEditorState.nativeCatalog() = NativeHomeCheckInCatalog(
    beanPickerOpen = isBeanPickerOpen, beanQuery = beanQuery,
    methods = methods.map { NativeHomeCheckInOption(it.code, it.name, null, null) },
    beans = beans.map { NativeHomeCheckInOption(it.slug, it.name, it.roasterName, null) },
    shelfBags = shelfBags.matching(beanQuery).map { NativeHomeCheckInOption(it.id, it.name, it.roasterName, it.imageUrl) },
    methodsLoading = methodsLoading, beansLoading = beansLoading,
    methodsIssueKey = nativeHomeIssueKey(methodsIssue), beansIssueKey = nativeHomeIssueKey(beansIssue),
)

private fun HomeCheckInEditorState.nativeIssues() = NativeHomeCheckInIssues(
    loadKey = if (loadIssue == HomePreparationIssue.RequestFailed || loadIssue == HomePreparationIssue.InvalidResponse)
        "home_check_in_load_failed" else nativeHomeIssueKey(loadIssue),
    saveKey = if (saveIssue != null && failedOperation == HomePreparationOperation.PhotoUpload)
        "home_check_in_photo_upload_failed" else nativeHomeIssueKey(saveIssue),
    fields = fieldErrors.map { NativeHomeCheckInFieldIssue(it.key.name, checkNotNull(nativeHomeIssueKey(it.value))) },
)

private fun HomePreparationVisibility?.publicValue(): Boolean? = this?.let { it == HomePreparationVisibility.Public }
