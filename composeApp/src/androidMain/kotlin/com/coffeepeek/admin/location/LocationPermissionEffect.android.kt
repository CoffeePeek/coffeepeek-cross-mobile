package com.coffeepeek.admin.location

import android.Manifest
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
actual fun LocationPermissionEffect(
    requestKey: Int,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onPermanentlyDenied: () -> Unit,
    requestOnce: Boolean,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val preferences = remember(context) { context.getSharedPreferences("location_permission", Context.MODE_PRIVATE) }
    val callbacks = rememberUpdatedState(LocationPermissionCallbacks(onGranted, onDenied, onPermanentlyDenied))
    var inFlight by rememberSaveable { mutableStateOf(false) }
    val request = remember { LocationPermissionRequest().also { if (inFlight) it.start(requestKey) } }
    val canAskAgain = { activity == null || locationPermissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) } }
    val report = { outcome: LocationPermissionOutcome -> if (request.deliver(outcome)) callbacks.value.deliver(outcome) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        inFlight = false
        report(locationPermissionOutcome(permissions.values.any { it } || PlatformLocation.hasPermission(), canAskAgain()))
    }
    DisposableEffect(request) { onDispose { request.close() } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!inFlight) report(locationPermissionOutcome(PlatformLocation.hasPermission(), canAskAgain()))
    }
    LaunchedEffect(requestKey) {
        if (inFlight || !request.start(requestKey)) return@LaunchedEffect
        when (locationPermissionAction(PlatformLocation.hasPermission(), preferences.getBoolean("asked", false), canAskAgain())) {
            LocationPermissionAction.Granted -> report(LocationPermissionOutcome.Granted)
            LocationPermissionAction.Settings -> report(LocationPermissionOutcome.PermanentlyDenied)
            LocationPermissionAction.Request -> {
                preferences.edit().putBoolean("asked", true).apply()
                inFlight = true
                launcher.launch(locationPermissions)
            }
        }
    }
}

private val locationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

private class LocationPermissionCallbacks(val granted: () -> Unit, val denied: () -> Unit, val permanentlyDenied: () -> Unit) {
    fun deliver(outcome: LocationPermissionOutcome) = when (outcome) {
        LocationPermissionOutcome.Granted -> granted()
        LocationPermissionOutcome.Denied -> denied()
        LocationPermissionOutcome.PermanentlyDenied -> permanentlyDenied()
    }
}
