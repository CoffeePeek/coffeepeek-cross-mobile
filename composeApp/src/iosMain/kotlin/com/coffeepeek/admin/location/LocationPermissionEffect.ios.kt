@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationState
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

/** Observes authorization only; collection never presents a permission dialog. */
internal fun locationAuthorizationChanges(): Flow<Boolean> = callbackFlow {
    val permission = IosLocationPermission { trySend(it) }
    if (UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive) trySend(PlatformLocation.hasPermission())
    awaitClose { permission.close() }
}

/** The current explicit request observes later Settings changes until its native lifetime ends. */
internal class IosLocationPermission(private val changed: (Boolean) -> Unit = {}) {
    private val manager = CLLocationManager()
    private val request = LocationPermissionRequest()
    private var callbacks: LocationPermissionCallbacks? = null
    private val delegate = LocationPermissionDelegate { authorizationChanged() }
    private val resumed = NSNotificationCenter.defaultCenter.addObserverForName(
        UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue,
    ) { authorizationChanged() }
    init { manager.delegate = delegate }

    fun request(key: Int, granted: () -> Unit, denied: () -> Unit, permanentlyDenied: () -> Unit) {
        if (!request.start(key)) return
        callbacks = LocationPermissionCallbacks(granted, denied, permanentlyDenied)
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> complete(LocationPermissionOutcome.Granted)
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> complete(LocationPermissionOutcome.PermanentlyDenied)
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            else -> complete(LocationPermissionOutcome.Denied)
        }
    }

    private fun authorizationChanged() {
        if (UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateActive) return
        val status = manager.authorizationStatus
        changed(status == kCLAuthorizationStatusAuthorizedAlways || status == kCLAuthorizationStatusAuthorizedWhenInUse)
        when (status) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> complete(LocationPermissionOutcome.Granted)
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> complete(LocationPermissionOutcome.PermanentlyDenied)
        }
    }

    private fun complete(outcome: LocationPermissionOutcome) {
        val active = UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive
        if (request.deliver(outcome, active)) callbacks?.deliver(outcome)
    }

    fun close() {
        request.close()
        callbacks = null
        manager.delegate = null
        NSNotificationCenter.defaultCenter.removeObserver(resumed)
    }
}

private class LocationPermissionCallbacks(val granted: () -> Unit, val denied: () -> Unit, val permanentlyDenied: () -> Unit) {
    fun deliver(outcome: LocationPermissionOutcome) = when (outcome) {
        LocationPermissionOutcome.Granted -> granted()
        LocationPermissionOutcome.Denied -> denied()
        LocationPermissionOutcome.PermanentlyDenied -> permanentlyDenied()
    }
}

private class LocationPermissionDelegate(private val changed: () -> Unit) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) = changed()
}
