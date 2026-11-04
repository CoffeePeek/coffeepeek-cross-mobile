package com.coffeepeek.admin.ui.screen.auth

import com.coffeepeek.admin.analytics.*

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.auth.signInWithApple
import com.coffeepeek.admin.auth.socialLoginError
import com.coffeepeek.admin.auth.SocialLoginErrorCopy
import kotlinx.coroutines.CancellationException
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.InputValidationIssue
import com.coffeepeek.admin.utils.validateEmailRequired
import com.coffeepeek.admin.utils.validatePasswordRequired
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.feature.auth.domain.LoginFailure
import com.coffeepeek.feature.auth.domain.classifyLoginFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel() {

    private val _email = MutableStateFlow("")
    private val _password = MutableStateFlow("")
    private val _emailError = MutableStateFlow<InputValidationIssue?>(null)
    private val _passwordError = MutableStateFlow<InputValidationIssue?>(null)
    private val _loginError = MutableStateFlow<String?>(null)
    private val _loginFailure = MutableStateFlow<LoginFailure?>(null)

    val email = _email.asStateFlow()
    val password = _password.asStateFlow()
    val emailError = _emailError.asStateFlow()
    val passwordError = _passwordError.asStateFlow()
    val loginError = _loginError.asStateFlow()
    val loginFailure = _loginFailure.asStateFlow()
    private val _appleSigningIn = MutableStateFlow(false)
    val appleSigningIn = _appleSigningIn.asStateFlow()

    internal fun onAppleLogin(errorCopy: () -> SocialLoginErrorCopy) {
        if (!_appleSigningIn.compareAndSet(expect = false, update = true)) return
        clearRequestErrors()
        launchRequest(
            onSuccess = { Navigator.navigate(Navigator.Screen.Main) },
            onError = { if (it !is CancellationException) _loginError.value = socialLoginError("Apple", it.message, errorCopy()) },
        ) {
            try { authRepository.signInWithApple().also { signInAnalytics(it.userId, "apple", "login") } }
            finally { _appleSigningIn.value = false }
        }
    }

    fun onEmailChange(newValue: String) {
        _email.value = newValue
        clearRequestErrors()
        if (_emailError.value != null) {
            _emailError.value = validateEmailRequired(newValue)
        }
    }

    fun onPasswordChange(newValue: String) {
        _password.value = newValue
        clearRequestErrors()
        if (_passwordError.value != null) {
            _passwordError.value = validatePasswordRequired(newValue)
        }
    }

    fun onLoginClick() {
        val emailValue = _email.value.trim()
        val passwordValue = _password.value
        val emailErr = validateEmailRequired(emailValue)
        val passwordErr = validatePasswordRequired(passwordValue)
        _emailError.value = emailErr
        _passwordError.value = passwordErr
        clearRequestErrors()
        if (emailErr != null || passwordErr != null) return

        launchRequest(
            onSuccess = { Navigator.navigate(Navigator.Screen.Main) },
            onError = { error -> _loginFailure.value = classifyLoginFailure(error.message) },
        ) {
            authRepository.login(emailValue, passwordValue).getOrThrow().also { signInAnalytics(it.userId, "password", "login", emailValue) }
        }
    }

    internal fun onGoogleLogin(idToken: String, errorCopy: () -> SocialLoginErrorCopy) {
        clearRequestErrors()
        launchRequest(
            onSuccess = { Navigator.navigate(Navigator.Screen.Main) },
            onError = { error -> _loginError.value = socialLoginError("Google", error.message, errorCopy()) },
        ) {
            authRepository.googleLogin(idToken).getOrThrow().also { signInAnalytics(it.userId, "google", "login") }
        }
    }

    fun clearFieldErrors() {
        _emailError.value = null
        _passwordError.value = null
        clearRequestErrors()
    }

    internal fun onGoogleLoginError(message: String, copy: SocialLoginErrorCopy) {
        _loginFailure.value = null
        _loginError.value = socialLoginError("Google", message, copy)
    }

    private fun clearRequestErrors() {
        _loginError.value = null
        _loginFailure.value = null
    }
}
