package com.coffeepeek.admin.utils

import com.coffeepeek.core.network.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ErrorHandler {
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

fun <T> Result<T>.handleError(): Result<T> {
    return onFailure { exception ->
        when (exception) {
            is ApiException -> ErrorHandler.showError(exception.message)
            else -> ErrorHandler.showError(exception.message ?: "Неизвестная ошибка")
        }
    }
}

