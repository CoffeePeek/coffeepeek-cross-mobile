package com.coffeepeek.feature.coffee.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Composition-bound telemetry (visibility/lifecycle aware), owned by the application. */
interface CoffeeScreenAnalytics {
    /** Reports the detail page once it is [loaded] and actually visible. */
    @Composable
    fun TrackDetail(loaded: Boolean)

    @Composable
    fun Overlay(content: @Composable () -> Unit) { content() }

    @Composable
    fun exposure(modifier: Modifier, section: CoffeeExposure): Modifier

    companion object {
        val None: CoffeeScreenAnalytics = object : CoffeeScreenAnalytics {
            @Composable override fun TrackDetail(loaded: Boolean) = Unit
            @Composable override fun exposure(modifier: Modifier, section: CoffeeExposure) = modifier
        }
    }
}
