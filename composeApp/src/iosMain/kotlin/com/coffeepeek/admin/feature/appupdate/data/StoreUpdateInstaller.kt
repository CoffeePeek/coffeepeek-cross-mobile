package com.coffeepeek.admin.feature.appupdate.data

import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.feature.appupdate.domain.InstallationState
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.admin.utils.OpenInBrowser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.native_store_open_failed
import org.jetbrains.compose.resources.getString

internal class StoreUpdateInstaller(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : UpdateInstaller {
    private val mutableState = MutableStateFlow(InstallationState())
    override val state = mutableState.asStateFlow()
    override fun download(update: AppUpdate) {
        scope.launch {
            try {
                OpenInBrowser.openInBrowser(update.url)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = InstallationState(update, error = getString(Res.string.native_store_open_failed))
            }
        }
    }
    override fun install() = Unit
    override fun cancel() = Unit
    override fun refresh() = Unit
}
