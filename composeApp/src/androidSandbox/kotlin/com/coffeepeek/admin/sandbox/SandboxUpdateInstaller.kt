package com.coffeepeek.admin.sandbox

import android.content.Intent
import android.content.IntentSender
import com.coffeepeek.feature.appupdate.api.AndroidUpdateInstaller
import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.feature.appupdate.domain.InstallationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Never binds Play Store, downloads an APK, requests consent or invokes Package Installer. */
internal class SandboxUpdateInstaller(private val unavailableMessage: String) : AndroidUpdateInstaller {
    private val mutableState = MutableStateFlow(InstallationState())
    override val state = mutableState.asStateFlow()
    override var requestConsent: ((IntentSender) -> Unit)? = null
    override var requestInstallation: ((Intent) -> Unit)? = null
    override fun download(update: AppUpdate) { mutableState.value = InstallationState(update = update, error = unavailableMessage) }
    override fun install() { mutableState.value = mutableState.value.copy(error = unavailableMessage) }
    override fun cancel() { mutableState.value = InstallationState() }
    override fun refresh() = Unit
    override fun consentResult(resultCode: Int) = Unit
    override fun installationResult() = Unit
}
