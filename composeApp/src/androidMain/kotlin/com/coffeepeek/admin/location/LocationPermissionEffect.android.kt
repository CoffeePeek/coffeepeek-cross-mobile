package com.coffeepeek.admin.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun LocationPermissionEffect(
    requestKey: Int,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    requestOnce: Boolean,
) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("location_permission", Context.MODE_PRIVATE) }
    val onGrantedState = rememberUpdatedState(onGranted)
    val onDeniedState = rememberUpdatedState(onDenied)
    var requestInFlight by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        requestInFlight = false
        if (permissions.values.any { it }) {
            onGrantedState.value()
        } else {
            onDeniedState.value()
        }
    }

    LaunchedEffect(requestKey) {
        if (requestKey <= 0 || requestInFlight) return@LaunchedEffect
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            onGrantedState.value()
        } else if (requestOnce && preferences.getBoolean("asked", false)) {
            onDeniedState.value()
        } else {
            if (requestOnce) preferences.edit().putBoolean("asked", true).apply()
            requestInFlight = true
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
}
