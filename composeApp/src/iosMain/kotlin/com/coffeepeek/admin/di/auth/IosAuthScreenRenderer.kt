package com.coffeepeek.admin.di.auth

import com.coffeepeek.admin.auth.AppleAuth
import com.coffeepeek.admin.auth.IosGoogleAuthBridge
import com.coffeepeek.admin.ui.screen.auth.AuthScreenRenderer
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.impl.createLoginEntry
import com.coffeepeek.feature.auth.impl.createPasswordRecoveryEntry
import com.coffeepeek.feature.auth.impl.createRegisterEntry

internal fun iosAuthScreenRenderer(repository: AuthRepository, analytics: AuthAnalytics): AuthScreenRenderer {
    val google = IosGoogleSignInLauncher.takeIf { IosGoogleAuthBridge.isConfigured() }
    val apple = IosAppleAuthorizer.takeIf { AppleAuth.isSupported() }
    return FeatureAuthScreenRenderer(
        login = createLoginEntry(repository, analytics, google, apple),
        register = createRegisterEntry(repository, analytics, google, apple),
        recovery = createPasswordRecoveryEntry(repository),
    )
}
