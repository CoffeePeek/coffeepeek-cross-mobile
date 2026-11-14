package com.coffeepeek.admin.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.darwin.NSObject

@Composable
actual fun LocationPermissionEffect(
    requestKey: Int,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onPermanentlyDenied: () -> Unit,
    requestOnce: Boolean,
) {
    val grantedState = rememberUpdatedState(onGranted)
    val deniedState = rememberUpdatedState(onDenied)
    // iOS has no transient denial: once the user refuses, only Settings can undo it.
    val permanentlyDeniedState = rememberUpdatedState(onPermanentlyDenied)
    val delegate = remember {
        LocationPermissionDelegate(
            onGranted = { grantedState.value() },
            onDenied = { permanentlyDeniedState.value() },
        )
    }
    val manager = remember { CLLocationManager() }

    DisposableEffect(manager, delegate) {
        manager.delegate = delegate
        onDispose { manager.delegate = null }
    }

    LaunchedEffect(requestKey) {
        if (requestKey <= 0) return@LaunchedEffect
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> grantedState.value()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> permanentlyDeniedState.value()
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            else -> deniedState.value()
        }
    }
}

private class LocationPermissionDelegate(
    private val onGranted: () -> Unit,
    private val onDenied: () -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> onGranted()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> onDenied()
            else -> Unit
        }
    }
}
