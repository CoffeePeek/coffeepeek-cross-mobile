package com.coffeepeek.feature.discovery.impl.ui

sealed interface NativeDiscoveryEvent {
    data class OpenShop(val id: String) : NativeDiscoveryEvent
    data class MapExpanded(val expanded: Boolean) : NativeDiscoveryEvent
    data object SignIn : NativeDiscoveryEvent
    data object AddShop : NativeDiscoveryEvent
    data object AddRoaster : NativeDiscoveryEvent
    data object Back : NativeDiscoveryEvent
    data object Close : NativeDiscoveryEvent
    data object ContinueCheckIn : NativeDiscoveryEvent
    data object FocusConsumed : NativeDiscoveryEvent
    data object OpenLocationSettings : NativeDiscoveryEvent
    data object FavoriteFeedback : NativeDiscoveryEvent
}

enum class NativeDiscoveryPermissionTarget { Search, Map }
enum class NativeDiscoveryPermissionOutcome { Granted, Denied, PermanentlyDenied }
