package com.coffeepeek.feature.homecheckin.impl

import com.coffeepeek.feature.homecheckin.impl.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Reads the original feature-owned illustration for the native onboarding page. */
@OptIn(ExperimentalResourceApi::class)
suspend fun guestCheckInOnboardingPoster(index: Int): ByteArray {
    val names = listOf("cover", "discovery", "atmosphere", "check_ins", "favorites")
    val page = names.getOrNull(index) ?: return byteArrayOf()
    return Res.readBytes("drawable/home_check_in_onboarding_poster_$page.jpg")
}
