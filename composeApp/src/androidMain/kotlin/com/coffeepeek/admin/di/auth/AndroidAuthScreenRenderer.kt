package com.coffeepeek.admin.di.auth

import com.coffeepeek.BuildConfig
import com.coffeepeek.admin.ui.screen.auth.AuthScreenRenderer
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.GoogleSignInConfig
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.impl.createLoginEntry
import com.coffeepeek.feature.auth.impl.createPasswordRecoveryEntry
import com.coffeepeek.feature.auth.impl.createRegisterEntry

internal object AppGoogleSignInConfig : GoogleSignInConfig {
    override val webClientId: String get() = BuildConfig.GOOGLE_WEB_CLIENT_ID
}

/** The shared renderer with the Android Google chooser behind the Google button. */
internal fun androidAuthScreenRenderer(repository: AuthRepository, analytics: AuthAnalytics): AuthScreenRenderer =
    FeatureAuthScreenRenderer(
        login = createLoginEntry(repository, analytics, AppGoogleSignInConfig),
        register = createRegisterEntry(repository, analytics, AppGoogleSignInConfig),
        recovery = createPasswordRecoveryEntry(repository),
    )
