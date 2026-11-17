@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.auth.AppleAuth
import com.coffeepeek.admin.auth.IosGoogleAuthBridge
import com.coffeepeek.admin.legal.LegalUrls
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.ShopCheckInSignInHandoff
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.auth.api.AppleAuthorizer
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
    val sessions = koinInject<SessionReader>()
    val destinations = IosAuthDestinations(koinInject(), koinInject(), koinInject())
    return { screen, resetToLogin ->
        when (screen) {
            Navigator.Screen.Auth -> { { factory(destinations.fullScreenLogin()) } }
            Navigator.Screen.Register -> { { factory(destinations.fullScreenRegister()) } }
            is Navigator.Screen.ForgotPassword -> {
                { factory(destinations.recovery(screen.email, Navigator::popBack, resetToLogin)) }
            }
            is Navigator.Screen.ShopCheckInSignIn -> {
                {
                    val attempt = Navigator.shopCheckInSignInAttempt(screen.shopId, sessions)
                    factory(destinations.shopCheckInSignIn(attempt, Navigator::popBack))
                }
            }
            else -> null
        }
    }
}

private fun IosAuthDestinations.fullScreenLogin() = login(
    back = Navigator::closeAuth,
    onForgotPassword = { email -> Navigator.navigate(Navigator.Screen.ForgotPassword(email)) },
    onRegister = { Navigator.popThenNavigate(Navigator.Screen.Register) },
    onSignedIn = { Navigator.navigate(Navigator.Screen.Main) },
)

private fun IosAuthDestinations.fullScreenRegister() = registration(
    back = Navigator::closeAuth,
    onLogin = { Navigator.popThenNavigate(Navigator.Screen.Auth) },
    onSignedIn = { Navigator.navigate(Navigator.Screen.Main) },
)

internal fun IosAuthDestinations.shopCheckInSignIn(
    attempt: ShopCheckInSignInHandoff.Attempt,
    onPopBack: () -> Unit,
): IosCheckInAuthFlow {
    attempt.begin()
    return IosCheckInAuthFlow(this, onCancel = {
        attempt.cancel()
        onPopBack()
    }, onDismiss = attempt::dispose)
}

internal class IosAuthDestinations(
    private val repository: AuthRepository,
    private val analytics: AuthAnalytics,
    val language: LanguagePort,
    private val apple: AppleAuthorizer? = IosAppleAuthorizer.takeIf { AppleAuth.isSupported() },
    private val googleAvailable: Boolean = IosGoogleAuthBridge.isConfigured(),
) {
    fun login(back: () -> Unit, onForgotPassword: (String) -> Unit, onRegister: () -> Unit, onSignedIn: () -> Unit) =
        IosLogin(NativeLoginSession(repository, analytics, apple), language, googleAvailable,
            back, onForgotPassword, onRegister, onSignedIn)

    fun registration(back: () -> Unit, onLogin: () -> Unit, onSignedIn: () -> Unit) =
        IosRegister(NativeRegisterSession(repository, analytics, apple), language, googleAvailable,
            back, onLogin, onSignedIn, onTerms = { OpenInBrowser.openInBrowser(LegalUrls.TERMS) },
            onPrivacy = { OpenInBrowser.openInBrowser(LegalUrls.PRIVACY) })

    fun recovery(email: String, back: () -> Unit, onLogin: () -> Unit) =
        IosPasswordRecovery(NativePasswordRecoverySession(email, repository), language, back, onLogin)
}
