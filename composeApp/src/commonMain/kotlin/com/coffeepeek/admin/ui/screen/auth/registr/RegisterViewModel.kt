package com.coffeepeek.admin.ui.screen.auth.registr

import com.coffeepeek.admin.analytics.*

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.InputValidationIssue
import com.coffeepeek.admin.utils.MIN_REGISTRATION_PASSWORD_LENGTH
import com.coffeepeek.admin.utils.validateEmailRequired
import com.coffeepeek.admin.utils.validatePasswordRequired
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.feature.auth.domain.RegistrationIssue
import com.coffeepeek.admin.auth.signInWithApple
import com.coffeepeek.admin.auth.socialLoginError
import com.coffeepeek.admin.auth.SocialLoginErrorCopy
import com.coffeepeek.admin.utils.ErrorHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RegisterStep {
    Email,
    Details,
    Success,
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val saveAnalyticsConsent: suspend (Boolean) -> Unit = { accepted ->
        Analytics.service?.setConsent(if (accepted) AnalyticsConsent.Accepted else AnalyticsConsent.Declined)
    },
) : BaseViewModel() {

    private val _step = MutableStateFlow(RegisterStep.Email)
    val step = _step.asStateFlow()

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _isTermsAccepted = MutableStateFlow(false)
    val isTermsAccepted = _isTermsAccepted.asStateFlow()

    private val _isAnalyticsAccepted = MutableStateFlow(false)
    val isAnalyticsAccepted = _isAnalyticsAccepted.asStateFlow()

    private val _passwordError = MutableStateFlow<InputValidationIssue?>(null)
    val passwordError = _passwordError.asStateFlow()

    private val _emailError = MutableStateFlow<InputValidationIssue?>(null)
    val emailError = _emailError.asStateFlow()
    private val _emailTaken = MutableStateFlow(false)
    val emailTaken = _emailTaken.asStateFlow()

    fun onNameChange(value: String) {
        _name.value = value
    }

    fun onEmailChange(value: String) {
        _email.value = value
        _emailTaken.value = false
        if (_emailError.value != null) {
            _emailError.value = null
        }
    }

    fun onPasswordChange(value: String) {
        _password.value = value
        if (_passwordError.value != null) {
            _passwordError.value = validatePasswordRequired(
                password = value,
                minLength = MIN_REGISTRATION_PASSWORD_LENGTH,
            )
        }
    }

    fun onTermsCheckedChange(checked: Boolean) {
        if (registering) return
        _isTermsAccepted.value = checked
    }

    fun onAnalyticsCheckedChange(checked: Boolean) {
        if (registering) return
        _isAnalyticsAccepted.value = checked
    }

    fun onBack() {
        // While the registration is being sent the form is frozen: going back would change the step under the request.
        if (registering) return
        when (_step.value) {
            RegisterStep.Details -> {
                _step.value = RegisterStep.Email
                _passwordError.value = null
            }
            RegisterStep.Email, RegisterStep.Success -> Navigator.popBack()
        }
    }

    internal fun onContinueEmail(errorCopy: () -> RegistrationErrorCopy) {
        val currentEmail = _email.value.trim()
        val formatError = validateEmailRequired(currentEmail)
        if (formatError != null) {
            _emailError.value = formatError
            return
        }

        launchRequest(
            onError = { showRequestError(it, errorCopy().checkFailed) },
        ) {
            val isTaken = authRepository.isEmailTaken(currentEmail).getOrThrow()
            _emailTaken.value = isTaken
            _emailError.value = null
            if (!isTaken) _step.value = RegisterStep.Details
        }
    }

    // Set synchronously by the click, so a second tap or a consent/e-mail edit while the request runs is ignored.
    private var registering = false

    internal fun onRegisterClick(errorCopy: () -> RegistrationErrorCopy) {
        if (registering) return
        val form = RegisterFormSnapshot(_name.value.trim(), _email.value.trim(), _password.value,
            _isTermsAccepted.value, _isAnalyticsAccepted.value)
        if (!validateRegistration(form, errorCopy())) return
        registering = true
        launchRequest(
            onSuccess = { _step.value = RegisterStep.Success },
            onError = { showRequestError(it, errorCopy().registrationFailed) },
        ) {
            try { registerForm(form) } finally { registering = false }
        }
    }

    private fun validateRegistration(form: RegisterFormSnapshot, copy: RegistrationErrorCopy): Boolean {
        val passwordIssue = validatePasswordRequired(form.password, MIN_REGISTRATION_PASSWORD_LENGTH)
        val emailIssue = validateEmailRequired(form.email)
        _passwordError.value = passwordIssue
        _emailError.value = emailIssue
        val reported = when {
            form.name.isEmpty() -> RegistrationIssue.NameRequired
            emailIssue != null && form.email.isEmpty() -> RegistrationIssue.EmailRequired
            emailIssue != null -> RegistrationIssue.EmailInvalid
            passwordIssue != null -> RegistrationIssue.PasswordTooShort
            !form.termsAccepted -> RegistrationIssue.TermsRequired
            else -> return true
        }
        ErrorHandler.showError(reported.message(copy))
        return false
    }

    private suspend fun registerForm(form: RegisterFormSnapshot) {
        // The optional choice is separate from terms. No SDK setup is allowed before login.
        saveAnalyticsConsent(form.analyticsAccepted)
        authRepository.register(form.name, form.email, form.password).getOrThrow()
        Analytics.service?.rememberPasswordSignUp(form.email)
    }

    private fun showRequestError(error: Exception, fallback: String) {
        ErrorHandler.showError(error.message?.takeIf(String::isNotBlank) ?: fallback)
    }

    private val _appleSigningIn = MutableStateFlow(false)
    val appleSigningIn = _appleSigningIn.asStateFlow()

    internal fun onAppleLogin(errorCopy: () -> SocialLoginErrorCopy) {
        if (!_appleSigningIn.compareAndSet(expect = false, update = true)) return
        launchRequest(
            onSuccess = { Navigator.navigate(Navigator.Screen.Main) },
            onError = { if (it !is CancellationException) ErrorHandler.showError(socialLoginError("Apple", it.message, errorCopy())) },
        ) {
            try {
                authRepository.signInWithApple().also { signInAnalytics(it.userId, "apple", "register") }
            }
            finally { _appleSigningIn.value = false }
        }
    }

    internal fun onGoogleLogin(idToken: String, errorCopy: () -> SocialLoginErrorCopy) {
        launchRequest(
            onError = { if (it !is CancellationException) ErrorHandler.showError(socialLoginError("Google", it.message, errorCopy())) },
            onSuccess = { Navigator.navigate(Navigator.Screen.Main) },
        ) {
            authRepository.googleLogin(idToken).getOrThrow().also { signInAnalytics(it.userId, "google", "register") }
        }
    }

    fun goToLogin() {
        Navigator.popThenNavigate(Navigator.Screen.Auth)
    }

    internal fun onGoogleLoginError(message: String, copy: SocialLoginErrorCopy) {
        ErrorHandler.showError(socialLoginError("Google", message, copy))
    }
}

private data class RegisterFormSnapshot(
    val name: String, val email: String, val password: String, val termsAccepted: Boolean, val analyticsAccepted: Boolean,
)
