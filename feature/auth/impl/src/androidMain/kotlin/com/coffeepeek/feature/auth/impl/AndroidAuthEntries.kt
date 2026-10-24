package com.coffeepeek.feature.auth.impl

import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.GoogleSignInConfig
import com.coffeepeek.feature.auth.api.LoginEntry
import com.coffeepeek.feature.auth.api.RegisterEntry
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.impl.ui.AndroidGoogleSignInLauncher

private fun GoogleSignInConfig.launcher() = webClientId.takeIf(String::isNotBlank)?.let(::AndroidGoogleSignInLauncher)

/** [createLoginEntry] with the Android Google chooser; a blank client id hides the button. */
fun createLoginEntry(repository: AuthRepository, analytics: AuthAnalytics, google: GoogleSignInConfig): LoginEntry =
    createLoginEntry(repository, analytics, google.launcher())

/** [createRegisterEntry] with the Android Google chooser; a blank client id hides the button. */
fun createRegisterEntry(repository: AuthRepository, analytics: AuthAnalytics, google: GoogleSignInConfig): RegisterEntry =
    createRegisterEntry(repository, analytics, google.launcher())
