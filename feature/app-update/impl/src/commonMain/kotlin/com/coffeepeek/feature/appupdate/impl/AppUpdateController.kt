package com.coffeepeek.feature.appupdate.impl

import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.feature.appupdate.domain.AppUpdateRepository
import com.coffeepeek.feature.appupdate.domain.InstallationStage
import com.coffeepeek.feature.appupdate.domain.InstallationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateUiState(
    val update: AppUpdate? = null,
    val showPrompt: Boolean = false,
    val checking: Boolean = false,
    val message: String? = null,
    val manual: Boolean = false,
    val checkFailed: Boolean = false,
)

fun needsPlayFallback(required: Boolean, transfer: InstallationState): Boolean = required &&
    (transfer.error != null || (transfer.stage != InstallationStage.Available && transfer.stage != InstallationStage.Starting))

/** Process-wide update coordinator: the root prompt, the Settings row and the Play flow share one instance. */
class AppUpdateController(
    private val repository: AppUpdateRepository,
    private val environment: AppUpdateEnvironment,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val currentVersion: Long? get() = environment.versionCode
    private val mutableState = MutableStateFlow(UpdateUiState())
    val state = mutableState.asStateFlow()
    private var dismissed: Long? = null

    fun check(manual: Boolean = false) {
        if (mutableState.value.checking) return
        mutableState.value = mutableState.value.copy(checking = true, message = null)
        scope.launch {
            try {
                val current = currentVersion
                val policy = if (current != null) repository.fetch() else null
                dismissed = dismissed ?: repository.dismissedVersion()
                val available = policy?.takeIf { current != null && it.isAvailable(current) }
                mutableState.value = UpdateUiState(
                    update = available,
                    showPrompt = available?.shouldPrompt(current!!, dismissed, manual) == true,
                    manual = manual,
                    checkFailed = manual && policy == null,
                    message = if (manual && available == null) {
                        if (policy == null) "Не удалось проверить обновления. Попробуйте позже." else "Установлена актуальная версия"
                    } else null,
                )
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                mutableState.value = UpdateUiState(manual = manual, checkFailed = manual,
                    message = if (manual) "Не удалось проверить обновления. Попробуйте позже." else null)
            }
        }
    }

    fun takeNativePrompt(transfer: InstallationState): AppUpdate? {
        val pending = mutableState.value
        if (!pending.showPrompt) return null
        mutableState.value = pending.copy(showPrompt = false)
        return pending.update?.takeIf {
            transfer.stage == InstallationStage.Available && (transfer.error == null || pending.manual)
        }
    }

    fun dismiss() {
        val update = mutableState.value.update ?: return
        if (update.isRequired(currentVersion ?: return)) return
        dismissed = update.versionCode
        mutableState.value = mutableState.value.copy(showPrompt = false)
        scope.launch {
            try { repository.dismiss(update.versionCode) } catch (error: Exception) {
                if (error is CancellationException) throw error
            }
        }
    }

}
