package com.coffeepeek.feature.settings.impl.ui.model

import com.coffeepeek.feature.settings.domain.AppLanguage

internal data class LanguageState(
    val selected: AppLanguage,
    val isSaving: Boolean = false,
    val failedLanguage: AppLanguage? = null,
) {
    val hasSaveError: Boolean get() = failedLanguage != null
}

internal sealed interface LanguageAction {
    data class Select(val language: AppLanguage) : LanguageAction
    data object RetrySave : LanguageAction
}
