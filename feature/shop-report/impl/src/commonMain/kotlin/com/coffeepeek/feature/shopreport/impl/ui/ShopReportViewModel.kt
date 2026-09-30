package com.coffeepeek.feature.shopreport.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import com.coffeepeek.feature.shopreport.domain.repository.ShopIssueReportRepository
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportAction
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportError
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportEvent
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportState
import kotlinx.coroutines.CancellationException

internal class ShopReportViewModel(
    private val shopId: String,
    private val repository: ShopIssueReportRepository,
) : MviViewModel<ShopReportState, ShopReportAction, ShopReportEvent>(ShopReportState()) {

    override suspend fun handleActionInternal(action: ShopReportAction) {
        when (action) {
            is ShopReportAction.SelectCategory -> updateState {
                copy(selectedCategory = action.category, error = null)
            }
            is ShopReportAction.ChangeDescription -> updateState {
                copy(description = action.description.take(MAX_DESCRIPTION_LENGTH), error = null)
            }
            ShopReportAction.Submit -> submit()
            ShopReportAction.Back -> sendEvent(ShopReportEvent.Back)
        }
    }

    private suspend fun submit() {
        val selectedCategory = currentState.selectedCategory
        if (selectedCategory == null) {
            updateState { copy(error = ShopReportError.CategoryRequired) }
            return
        }
        val description = currentState.description.trim()
        if (selectedCategory == ShopIssueCategory.Other && description.isEmpty()) {
            updateState { copy(error = ShopReportError.DescriptionRequired) }
            return
        }
        if (currentState.isSubmitting || currentState.isSubmitted) return

        updateState { copy(isSubmitting = true, error = null) }
        try {
            val result = repository.submitReport(
                shopId = shopId,
                category = selectedCategory,
                description = description.takeIf(String::isNotEmpty),
            )
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            updateState {
                copy(isSubmitting = false, isSubmitted = result.isSuccess,
                    error = if (result.isFailure) ShopReportError.SubmissionFailed else null)
            }
        } finally {
            updateState { if (isSubmitting) copy(isSubmitting = false) else this }
        }
    }

    private companion object {
        const val MAX_DESCRIPTION_LENGTH = 500
    }
}
