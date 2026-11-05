package com.coffeepeek.feature.contentreport.impl.ui.compose.model

import com.coffeepeek.feature.contentreport.domain.model.ContentReportProblem

internal data class ContentReportError(val problem: ContentReportProblem, val serverMessage: String? = null)

internal data class ContentReportState(
    val text: String = "", val isPreview: Boolean = false, val isCheckIn: Boolean = true,
    val isLoggedIn: Boolean = false, val accessAvailable: Boolean = true,
    val isSubmitting: Boolean = false, val isSubmitted: Boolean = false,
    val deliveryUnconfirmed: Boolean = false, val error: ContentReportError? = null,
) {
    val canEdit get() = accessAvailable && !isSubmitting && !isSubmitted && !deliveryUnconfirmed
}

internal sealed interface ContentReportAction {
    data class TextChanged(val text: String) : ContentReportAction
    data class Submit(val owner: Any = Unit) : ContentReportAction
    data class Back(val owner: Any = Unit) : ContentReportAction
    data class RetryAccess(val owner: Any = Unit) : ContentReportAction
    data object Resume : ContentReportAction
}

internal enum class ContentReportEvent { Back, SignIn, RetryAccess }
internal data class ContentReportEventEnvelope(val event: ContentReportEvent, val owner: Any) {
    fun forOwner(current: Any) = event.takeIf { owner === current }
}
