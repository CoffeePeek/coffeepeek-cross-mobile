@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.auth.AppleAuth
import com.coffeepeek.admin.auth.IosGoogleAuthBridge
import com.coffeepeek.admin.legal.LegalUrls
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.impl.NativeLoginSession
import com.coffeepeek.feature.auth.impl.NativePasswordRecoverySession
import com.coffeepeek.feature.auth.impl.NativeRegisterSession
import com.coffeepeek.feature.settings.api.LanguagePort
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosAuthController = staticCompositionLocalOf<(IosAuthScreen) -> UIViewController> {
    error("The iOS app must provide an authentication controller")
}

@Composable
internal fun iosAuthContent(): (Navigator.Screen, () -> Unit) -> (() -> UIViewController)? {
    val factory = LocalIosAuthController.current
    val repository = koinInject<AuthRepository>()
    val analytics = koinInject<AuthAnalytics>()
    val language = koinInject<LanguagePort>()
    return { screen, resetToLogin ->
        when (screen) {
            Navigator.Screen.Auth -> { { factory(nativeLogin(repository, analytics, language)) } }
            Navigator.Screen.Register -> { { factory(nativeRegister(repository, analytics, language)) } }
            is Navigator.Screen.ForgotPassword -> {
                { factory(IosPasswordRecovery(NativePasswordRecoverySession(screen.email, repository), language, Navigator::popBack, resetToLogin)) }
            }
            else -> null
        }
    }
}

private fun nativeLogin(repository: AuthRepository, analytics: AuthAnalytics, language: LanguagePort) = IosLogin(
    NativeLoginSession(repository, analytics, IosAppleAuthorizer.takeIf { AppleAuth.isSupported() }),
    language,
    googleAvailable = IosGoogleAuthBridge.isConfigured(),
    back = Navigator::closeAuth,
    onForgotPassword = { email -> Navigator.navigate(Navigator.Screen.ForgotPassword(email)) },
    onRegister = { Navigator.popThenNavigate(Navigator.Screen.Register) },
    onSignedIn = { Navigator.navigate(Navigator.Screen.Main) },
)

private fun nativeRegister(repository: AuthRepository, analytics: AuthAnalytics, language: LanguagePort) = IosRegister(
    NativeRegisterSession(repository, analytics, IosAppleAuthorizer.takeIf { AppleAuth.isSupported() }),
    language,
    googleAvailable = IosGoogleAuthBridge.isConfigured(),
    back = Navigator::closeAuth,
    onLogin = { Navigator.popThenNavigate(Navigator.Screen.Auth) },
    onSignedIn = { Navigator.navigate(Navigator.Screen.Main) },
    onTerms = { OpenInBrowser.openInBrowser(LegalUrls.TERMS) },
    onPrivacy = { OpenInBrowser.openInBrowser(LegalUrls.PRIVACY) },
)
