package com.coffeepeek.admin.notifications

import com.coffeepeek.feature.notifications.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Native application boundary; Firebase types never enter shared feature code. */
object NativePushBridge : PushDevicePlatform {
    private val mutable = MutableStateFlow(PushDeviceState())
    override val state = mutable.asStateFlow()
    private var permission: () -> Unit = {}
    private var accountChanged: (String?) -> Unit = {}
    fun configure(installationId: String, available: Boolean, permissionGranted: Boolean,
        requestPermission: () -> Unit, changeAccount: (String?) -> Unit) {
        permission = requestPermission
        accountChanged = changeAccount
        mutable.update { it.copy(installationId = installationId, available = available, permissionGranted = permissionGranted) }
        accountChanged(mutable.value.userId)
    }
    override fun setAccount(userId: String?) {
        mutable.update { it.copy(userId = userId, token = null) }
        accountChanged(userId)
    }
    override fun requestPermission() = permission()
    fun updatePermission(granted: Boolean) { mutable.update { it.copy(permissionGranted = granted) } }
    fun updateToken(userId: String, token: String) {
        mutable.update { if (it.userId == userId) it.copy(token = token) else it }
    }
    fun accepts(userId: String?): Boolean = userId != null && mutable.value.userId == userId
}
