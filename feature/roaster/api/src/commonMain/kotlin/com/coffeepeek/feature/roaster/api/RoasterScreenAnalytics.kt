package com.coffeepeek.feature.roaster.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Composition-bound telemetry (visibility/lifecycle aware), owned by the application. */
interface RoasterScreenAnalytics {
    /** Reports the detail page once it is [loaded] and actually visible. */
    @Composable
    fun TrackDetail(loaded: Boolean)

    @Composable
    fun exposure(modifier: Modifier, section: RoasterExposure): Modifier

    companion object {
        val None: RoasterScreenAnalytics = object : RoasterScreenAnalytics {
            @Composable override fun TrackDetail(loaded: Boolean) = Unit
            @Composable override fun exposure(modifier: Modifier, section: RoasterExposure) = modifier
        }
    }
}
