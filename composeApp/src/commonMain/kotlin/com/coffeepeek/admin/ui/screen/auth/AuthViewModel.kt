package com.coffeepeek.admin.ui.screen.auth

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.auth.signInWithApple
import com.coffeepeek.admin.auth.socialLoginError
import kotlinx.coroutines.CancellationException
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.localizedLoginError
import com.coffeepeek.admin.utils.validateEmailRequired
import com.coffeepeek.admin.utils.validatePasswordRequired
import com.coffeepeek.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel() {

    private val _email = MutableStateFlow("")
    private val _password = MutableStateFlow("")
    private val _emailError = MutableStateFlow<String?>(null)
    private val _passwordError = MutableStateFlow<String?>(null)
    private val _loginError = MutableStateFlow<String?>(null)

    val email = _email.asStateFlow()
    val password = _password.asStateFlow()
    val emailError = _emailError.asStateFlow()
    val passwordError = _passwordError.asStateFlow()
    val loginError = _loginError.asStateFlow()
    private val _appleSigningIn = MutableStateFlow(false)
    val appleSigningIn = _appleSigningIn.asStateFlow()

    fun onAppleLogin() {
        if (!_appleSigningIn.compareAndSet(expect = false, update = true)) return
        _loginError.value = null
        launchRequest(
            onSuccess = { Navigator.navigate(Navigator.Screen.Main) },
            onError = { if (it !is CancellationException) _loginError.value = socialLoginError("Apple", it.message) },
        ) {
            try { authRepository.signInWithApple() }
            finally { _appleSigningIn.value = false }
        }
    }

    fun onEmailChange(newValue: String) {
        _email.value = newValue
        _loginError.value = null
        if (_emailError.value != null) {
            _emailError.value = validateEmailRequired(newValue)
        }
    }

    fun onPasswordChange(newValue: String) {
        _password.value = newValue
        _loginError.value = null
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
        _loginError.value = null
        if (emailErr != null || passwordErr != null) return

        launchRequest(
            onSuccess = { Navigator.navigate(Navigator.Screen.Main) },
            onError = { error -> _loginError.value = localizedLoginError(error.message) },
        ) {
            authRepository.login(emailValue, passwordValue).getOrThrow()
        }
    }

    fun onGoogleLogin(idToken: String) {
        _loginError.value = null
        launchRequest(
            onSuccess = { Navigator.navigate(Navigator.Screen.Main) },
            onError = { error -> _loginError.value = socialLoginError("Google", error.message) },
        ) {
            authRepository.googleLogin(idToken).getOrThrow()
        }
    }

    fun clearFieldErrors() {
        _emailError.value = null
        _passwordError.value = null
        _loginError.value = null
    }

    fun onGoogleLoginError(message: String) {
        _loginError.value = socialLoginError("Google", message)
    }
}
