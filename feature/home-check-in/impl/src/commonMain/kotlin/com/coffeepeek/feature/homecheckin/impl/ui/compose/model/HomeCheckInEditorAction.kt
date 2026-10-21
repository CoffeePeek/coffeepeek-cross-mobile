package com.coffeepeek.feature.homecheckin.impl.ui.compose.model

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationVisibility

internal sealed interface HomeCheckInEditorAction {
    data object Initialize : HomeCheckInEditorAction
    data object RetryDetail : HomeCheckInEditorAction
    data object RetryMethods : HomeCheckInEditorAction
    data class DateChosen(val civilDate: String, val preparedAt: String) : HomeCheckInEditorAction
    data class MethodChanged(val code: String?) : HomeCheckInEditorAction
    data class DoseChanged(val value: String) : HomeCheckInEditorAction
    data class DurationChanged(val seconds: Int?) : HomeCheckInEditorAction
    data class TasteRatingTapped(val score: Int) : HomeCheckInEditorAction
    data class NoteChanged(val value: String) : HomeCheckInEditorAction
    data class DrinkNameChanged(val value: String) : HomeCheckInEditorAction
    data class VisibilityChanged(val value: HomePreparationVisibility) : HomeCheckInEditorAction
    data object OpenBeanPicker : HomeCheckInEditorAction
    data object CloseBeanPicker : HomeCheckInEditorAction
    data class BeanQueryChanged(val value: String) : HomeCheckInEditorAction
    data object SearchBeans : HomeCheckInEditorAction
    data class BeanSelected(val slug: String) : HomeCheckInEditorAction
    data class CustomBeanChosen(val name: String) : HomeCheckInEditorAction
    data class PhotosAdded(val photos: List<HomePreparationUpload>) : HomeCheckInEditorAction
    data class RemovePhoto(val index: Int) : HomeCheckInEditorAction
    data object Save : HomeCheckInEditorAction
    data object Close : HomeCheckInEditorAction
    data object RevokeAccess : HomeCheckInEditorAction
}
