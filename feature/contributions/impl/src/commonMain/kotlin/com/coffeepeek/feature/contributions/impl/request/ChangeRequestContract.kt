package com.coffeepeek.feature.contributions.impl.request

import com.coffeepeek.feature.contributions.domain.model.ChangeRequest

internal enum class RequestError { SignInRequired, NotFound, LoadFailed }

internal data class ChangeRequestState(
    val request: ChangeRequest? = null,
    val isLoading: Boolean = true,
    val error: RequestError? = null,
    /** The server's own wording of a failed load, shown instead of the default text. */
    val serverMessage: String? = null,
)

internal sealed interface ChangeRequestAction {
    data object Refresh : ChangeRequestAction
}

internal sealed interface ChangeRequestEvent
