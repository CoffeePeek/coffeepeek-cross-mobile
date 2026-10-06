package com.coffeepeek.admin.feature.appupdate.data

import com.coffeepeek.admin.feature.appupdate.domain.AppUpdate
import com.coffeepeek.admin.feature.appupdate.domain.InstallationState
import com.coffeepeek.admin.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.admin.utils.OpenInBrowser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class StoreUpdateInstaller : UpdateInstaller {
    private val mutableState = MutableStateFlow(InstallationState())
    override val state = mutableState.asStateFlow()
    override fun download(update: AppUpdate) {
        try { OpenInBrowser.openInBrowser(update.url) } catch (_: Exception) {
            mutableState.value = InstallationState(update, error = "Не удалось открыть App Store.")
        }
    }
    override fun install() = Unit
    override fun cancel() = Unit
    override fun refresh() = Unit
}
