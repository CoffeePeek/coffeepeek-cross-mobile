package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.settings.api.LanguagePort
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosGuestProfileController = staticCompositionLocalOf<(IosGuestProfile) -> UIViewController> {
    error("The iOS app must provide a guest profile controller")
}

@Composable
internal fun iosGuestProfileContent(): () -> UIViewController {
    val factory = LocalIosGuestProfileController.current
    val language = koinInject<LanguagePort>()
    return iosGuestProfileContent(factory, language)
}

internal fun iosGuestProfileContent(factory: (IosGuestProfile) -> UIViewController, language: LanguagePort): () -> UIViewController {
    return {
        factory(IosGuestProfile(language,
            onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
            onRegister = { Navigator.navigate(Navigator.Screen.Register) },
        ))
    }
}
