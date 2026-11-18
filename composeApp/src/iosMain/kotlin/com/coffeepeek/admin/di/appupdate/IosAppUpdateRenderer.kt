package com.coffeepeek.admin.di.appupdate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.coffeepeek.admin.di.system.IosRootModal
import com.coffeepeek.admin.di.system.IosRootModalObservation
import com.coffeepeek.admin.di.system.NativeRootModal
import com.coffeepeek.admin.di.homecheckin.nativeGuestOnboardingVisible
import com.coffeepeek.admin.ui.screen.appupdate.AppUpdateRenderer
import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.launch

internal class IosAppUpdateRenderer(
    private val controller: AppUpdateController,
    private val installer: UpdateInstaller,
    private val language: LanguagePort,
) : AppUpdateRenderer {
    @Composable
    override fun Prompt() {
        if (!controller.checksEnabled) return
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { controller.check() }
        val onboardingVisible by nativeGuestOnboardingVisible.collectAsState()
        if (onboardingVisible) return
        val state by controller.state.collectAsState()
        val update = state.update ?: return
        val currentVersion = AppConfigUpdateEnvironment.versionCode ?: return
        if (!state.showPrompt) return
        val model = remember(update) { IosAppUpdate(update, controller, installer, language, currentVersion) }
        NativeRootModal(model)
    }
}

class IosAppUpdate internal constructor(
    private val update: AppUpdate,
    private val controller: AppUpdateController,
    private val installer: UpdateInstaller,
    language: LanguagePort,
    currentVersion: Long,
) : IosRootModal(language) {
    val required = update.isRequired(currentVersion)
    fun currentError() = installer.state.value.error
    fun observeError(receive: (String?) -> Unit) = IosRootModalObservation(scope.launch {
        installer.state.collect { receive(it.error) }
    })
    fun openStore() { if (isActive) installer.download(update) }
    fun dismiss() { if (isActive && !required) controller.dismiss() }
}
