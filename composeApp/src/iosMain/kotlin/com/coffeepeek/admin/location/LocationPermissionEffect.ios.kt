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
    val permanentlyDeniedState = rememberUpdatedState(onPermanentlyDenied)
    val permission = remember { IosLocationPermission() }
    DisposableEffect(permission) { onDispose { permission.close() } }
    LaunchedEffect(requestKey) {
        permission.request(requestKey, { grantedState.value() }, { deniedState.value() }, { permanentlyDeniedState.value() })
    }
}

/** One request source and native lifetime; an unsolicited delegate callback has no action. */
internal class IosLocationPermission {
    private val manager = CLLocationManager()
    private var lastKey = 0
    private var closed = false
    private var pending: LocationPermissionCallbacks? = null
    private val delegate = LocationPermissionDelegate(
        onGranted = { complete { it.granted() } }, onDenied = { complete { it.permanentlyDenied() } })
    init { manager.delegate = delegate }
    fun request(key: Int, granted: () -> Unit, denied: () -> Unit, permanentlyDenied: () -> Unit) {
        if (closed || key <= lastKey) return
        lastKey = key
        pending = LocationPermissionCallbacks(granted, denied, permanentlyDenied)
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> complete { it.granted() }
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> complete { it.permanentlyDenied() }
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            else -> complete { it.denied() }
        }
    }
    private fun complete(action: (LocationPermissionCallbacks) -> Unit) {
        val receiver = pending ?: return
        pending = null
        if (!closed) action(receiver)
    }
    fun close() {
        closed = true
        pending = null
        manager.delegate = null
    }
}

private class LocationPermissionCallbacks(val granted: () -> Unit, val denied: () -> Unit, val permanentlyDenied: () -> Unit)

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
