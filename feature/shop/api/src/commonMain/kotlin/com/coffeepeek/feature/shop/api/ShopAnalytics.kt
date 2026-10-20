package com.coffeepeek.feature.shop.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Only bounded interaction metadata crosses this port; never shop/user IDs or text. */
interface ShopAnalytics {
    fun action(action: ShopActionMetric) {}
    fun gate(action: ShopActionMetric) {}
    fun checkInStarted(authenticated: Boolean) {}
    fun favoriteRequiresSignIn(desired: Boolean) {}
    fun content(section: ShopContentMetric, action: ShopContentAction) {}
    @Composable fun TrackDetail(loaded: Boolean) {}
    @Composable fun Overlay(content: @Composable () -> Unit) { content() }
    @Composable fun exposure(modifier: Modifier, feature: ShopExposure): Modifier = modifier

    companion object { val None: ShopAnalytics = object : ShopAnalytics {} }
}

enum class ShopActionMetric { Edit, Report, Directions, ShowOnMap, Share, PhoneCopy, PhoneCall, Instagram, Website, Email }
enum class ShopContentMetric { Photos, Menu, Roasters, Hours }
enum class ShopContentAction { Open, Close, Select }
enum class ShopExposure { Photos, Hours, Menu, Roasters, Contacts, Favorite, Share, CheckIn, Directions }
