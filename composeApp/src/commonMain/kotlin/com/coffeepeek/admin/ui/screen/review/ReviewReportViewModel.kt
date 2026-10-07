package com.coffeepeek.admin.ui.screen.review

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.domain.repository.ReviewRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewReportUiState(
    val text: String = "",
    val isPreview: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null,
)

class ReviewReportViewModel(
    private val reviewId: String,
    private val reviews: ReviewRepository,
    private val sessions: SessionRepository,
    private val isPreview: Boolean = false,
) : BaseViewModel() {
    private val _state = MutableStateFlow(ReviewReportUiState(isPreview = isPreview))
    val state = _state.asStateFlow()

    fun updateText(text: String) {
        _state.update { if (it.isSubmitting) it else it.copy(text = text.take(2000), error = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting || current.isSubmitted) return
        val text = current.text.trim()
        if (text.length !in 1..2000) {
            _state.update { it.copy(error = "Опишите проблему: от 1 до 2000 символов") }
            return
        }
        if (isPreview) {
            _state.update { it.copy(isSubmitted = true, text = "", error = null) }
            return
        }
        _state.update { it.copy(isSubmitting = true, error = null) }
        workScope.launch {
            if (requireAuthSession(sessions) == null) {
                _state.update { it.copy(isSubmitting = false, error = "Войдите в аккаунт, чтобы отправить жалобу") }
                Navigator.navigate(Navigator.Screen.Auth)
                return@launch
            }
            reviews.submitReviewReport(reviewId, text).onSuccess {
                _state.update { it.copy(isSubmitting = false, isSubmitted = true, text = "") }
            }.onFailure { error ->
                _state.update { it.copy(isSubmitting = false, error = error.message ?: "Не удалось отправить жалобу. Попробуйте ещё раз") }
            }
        }
    }
}
