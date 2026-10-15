package com.coffeepeek.admin.ui.screen.auth.registr

import com.coffeepeek.admin.analytics.*

import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.email_no_exist
import coffeepeek.composeapp.generated.resources.error_email_name
import coffeepeek.composeapp.generated.resources.error_enter_name
import coffeepeek.composeapp.generated.resources.error_enter_password_length
import coffeepeek.composeapp.generated.resources.error_registr
import coffeepeek.composeapp.generated.resources.error_term_of_user
import coffeepeek.composeapp.generated.resources.error_valid_email
import coffeepeek.composeapp.generated.resources.maybe_later
import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.MIN_REGISTRATION_PASSWORD_LENGTH
import com.coffeepeek.admin.utils.validateEmailRequired
import com.coffeepeek.admin.utils.validatePasswordRequired
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.admin.auth.signInWithApple
import com.coffeepeek.admin.auth.socialLoginError
import com.coffeepeek.admin.utils.ErrorHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

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

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError = _passwordError.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError = _emailError.asStateFlow()

    fun onNameChange(value: String) {
        _name.value = value
    }

    fun onEmailChange(value: String) {
        _email.value = value
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
        _isTermsAccepted.value = checked
    }

    fun onAnalyticsCheckedChange(checked: Boolean) {
        _isAnalyticsAccepted.value = checked
    }

    fun onBack() {
        when (_step.value) {
            RegisterStep.Details -> {
                _step.value = RegisterStep.Email
                _passwordError.value = null
            }
            RegisterStep.Email, RegisterStep.Success -> Navigator.popBack()
        }
    }

    fun onContinueEmail() {
        val currentEmail = _email.value.trim()
        val formatError = validateEmailRequired(currentEmail)
        if (formatError != null) {
            _emailError.value = formatError
            return
        }

        launchRequest(
            errorMessage = Res.string.maybe_later,
        ) {
            val isTaken = authRepository.isEmailTaken(currentEmail).getOrThrow()
            if (isTaken) {
                _emailError.value = getString(Res.string.email_no_exist)
                return@launchRequest
            }
            _emailError.value = null
            _step.value = RegisterStep.Details
        }
    }

    fun onRegisterClick() {
        val analyticsAccepted = _isAnalyticsAccepted.value
        launchRequest(
            onSuccess = { _step.value = RegisterStep.Success },
            errorMessage = Res.string.error_registr,
        ) {
            val currentName = _name.value.trim()
            val currentEmail = _email.value.trim()
            val currentPassword = _password.value

            var isValid = true
            var error: StringResource = Res.string.maybe_later

            if (!_isTermsAccepted.value) {
                isValid = false
                error = Res.string.error_term_of_user
            }

            val passwordErr = validatePasswordRequired(
                password = currentPassword,
                minLength = MIN_REGISTRATION_PASSWORD_LENGTH,
            )
            if (passwordErr != null) {
                _passwordError.value = passwordErr
                isValid = false
                error = Res.string.error_enter_password_length
            }

            val emailErr = validateEmailRequired(currentEmail)
            if (emailErr != null) {
                _emailError.value = emailErr
                isValid = false
                error = if (currentEmail.isEmpty()) {
                    Res.string.error_email_name
                } else {
                    Res.string.error_valid_email
                }
            }

            if (currentName.isEmpty()) {
                isValid = false
                error = Res.string.error_enter_name
            }

            if (!isValid) {
                ErrorHandler.showError(getString(error))
                return@launchRequest
            }

            // The optional choice is separate from terms. No SDK setup is allowed before login.
            saveAnalyticsConsent(analyticsAccepted)
            authRepository.register(currentName, currentEmail, currentPassword).getOrThrow()
            Analytics.service?.rememberPasswordSignUp(currentEmail)
        }
    }

    private val _appleSigningIn = MutableStateFlow(false)
    val appleSigningIn = _appleSigningIn.asStateFlow()

    fun onAppleLogin() {
        if (!_appleSigningIn.compareAndSet(expect = false, update = true)) return
        launchRequest(
            onSuccess = { Navigator.navigate(Navigator.Screen.Main) },
            onError = { if (it !is CancellationException) ErrorHandler.showError(socialLoginError("Apple", it.message)) },
        ) {
            try {
                authRepository.signInWithApple().also { signInAnalytics(it.userId, "apple", "register") }
            }
            finally { _appleSigningIn.value = false }
        }
    }

    fun onGoogleLogin(idToken: String) {
        launchRequest(
            errorMessage = Res.string.maybe_later,
            onError = { if (it !is CancellationException) ErrorHandler.showError(socialLoginError("Google", it.message)) },
            onSuccess = { Navigator.navigate(Navigator.Screen.Main) },
        ) {
            authRepository.googleLogin(idToken).getOrThrow().also { signInAnalytics(it.userId, "google", "register") }
        }
    }

    fun goToLogin() {
        Navigator.popThenNavigate(Navigator.Screen.Auth)
    }
}
