package com.coffeepeek.feature.auth.impl.register

import com.coffeepeek.feature.auth.domain.EmailIssue
import com.coffeepeek.feature.auth.domain.PasswordIssue
import com.coffeepeek.feature.auth.domain.RegistrationIssue

internal enum class RegisterStep { Email, Details, Success }

/** A failed request: the server's own message when there was one, otherwise the screen shows its generic text. */
internal data class RequestFailure(val message: String?)

internal data class RegisterState(
    val step: RegisterStep = RegisterStep.Email,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val termsAccepted: Boolean = false,
    val analyticsAccepted: Boolean = false,
    val emailIssue: EmailIssue? = null,
    /** The e-mail check answered "taken". */
    val emailTaken: Boolean = false,
    val passwordIssue: PasswordIssue? = null,
    /** The single message a submit with invalid fields reports (the legacy priority); the button gating keeps it out of reach from the UI. */
    val reported: RegistrationIssue? = null,
    val failure: RequestFailure? = null,
    /** A request is running: the primary button shows a loader and the form ignores a second submit. */
    val busy: Boolean = false,
)

internal sealed interface RegisterAction {
    data class NameChanged(val value: String) : RegisterAction
    data class EmailChanged(val value: String) : RegisterAction
    data class PasswordChanged(val value: String) : RegisterAction
    data class TermsChanged(val checked: Boolean) : RegisterAction
    data class AnalyticsChanged(val checked: Boolean) : RegisterAction
    data object ContinueWithEmail : RegisterAction
    data object ChangeEmail : RegisterAction
    data object Submit : RegisterAction
}
