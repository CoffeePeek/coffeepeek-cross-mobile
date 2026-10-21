package com.coffeepeek.feature.notifications.api

import kotlinx.coroutines.flow.StateFlow

data class PushDeviceState(
    val installationId: String = "", val available: Boolean = false,
    val permissionGranted: Boolean = false, val userId: String? = null, val token: String? = null,
    val notificationRevision: Long = 0,
)

interface PushDevicePlatform {
    val state: StateFlow<PushDeviceState>
    fun setAccount(userId: String?)
    fun requestPermission()
    fun requestPermissionFromSettings() = requestPermission()
}
