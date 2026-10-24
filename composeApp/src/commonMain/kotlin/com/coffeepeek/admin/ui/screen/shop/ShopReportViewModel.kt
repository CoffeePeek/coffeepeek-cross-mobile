package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.domain.model.ShopIssueCategory
import com.coffeepeek.domain.repository.ShopIssueReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ShopReportReason(val title: String, val category: ShopIssueCategory) {
    OUTDATED_MENU("Меню больше не актуально", ShopIssueCategory.OutdatedMenu),
    SHOP_CLOSED("Кофейня закрылась", ShopIssueCategory.ShopClosed),
    INCORRECT_SCHEDULE("В расписании есть ошибка", ShopIssueCategory.WrongOpeningHours),
    INCORRECT_PHOTOS("Фотографии не соответствуют кофейне", ShopIssueCategory.IncorrectPhotos),
    INCORRECT_ADDRESS("Указан неверный адрес", ShopIssueCategory.IncorrectAddress),
    OTHER("Другая проблема", ShopIssueCategory.Other),
}

data class ShopReportUiState(
    val selectedReason: ShopReportReason? = null,
    val comment: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null,
)

class ShopReportViewModel(
    private val shopId: String,
    private val shopIssueReportRepository: ShopIssueReportRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(ShopReportUiState())
    val state = _state.asStateFlow()

    fun selectReason(reason: ShopReportReason) {
        _state.update { it.copy(selectedReason = reason, error = null) }
    }

    fun updateComment(comment: String) {
        _state.update { it.copy(comment = comment.take(MAX_COMMENT_LENGTH), error = null) }
    }

    fun submit() {
        val current = _state.value
        // A second tap while the first report is in flight, or after it was sent, must not send another one.
        if (current.isSubmitting || current.isSubmitted) return
        if (current.selectedReason == null) {
            _state.update { it.copy(error = "Выберите, какие данные нужно исправить") }
            return
        }
        if (current.selectedReason == ShopReportReason.OTHER && current.comment.isBlank()) {
            _state.update { it.copy(error = "Опишите проблему") }
            return
        }

        _state.update { it.copy(isSubmitting = true, error = null) }
        workScope.launch {
            shopIssueReportRepository.submitReport(
                shopId = shopId,
                category = current.selectedReason.category,
                description = current.comment.trim().takeIf(String::isNotEmpty),
            ).onSuccess {
                _state.update { it.copy(isSubmitting = false, isSubmitted = true) }
            }.onFailure { e ->
                _state.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }

    private companion object {
        const val MAX_COMMENT_LENGTH = 500
    }
}
