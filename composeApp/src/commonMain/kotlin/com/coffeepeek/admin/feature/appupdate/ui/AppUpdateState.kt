package com.coffeepeek.admin.feature.appupdate.ui

import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.feature.appupdate.domain.AppUpdate
import com.coffeepeek.admin.feature.appupdate.domain.AppUpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class UpdateUiState(
    val update: AppUpdate? = null,
    val showPrompt: Boolean = false,
    val checking: Boolean = false,
    val message: String? = null,
)

internal class AppUpdateState(private val repository: AppUpdateRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutableState = MutableStateFlow(UpdateUiState())
    val state = mutableState.asStateFlow()
    private var dismissed: Long? = null

    fun check(manual: Boolean = false) {
        if (mutableState.value.checking) return
        mutableState.value = mutableState.value.copy(checking = true, message = null)
        scope.launch {
            try {
                val current = AppConfig.versionCode
                val policy = if (current != null) repository.fetch() else null
                dismissed = dismissed ?: repository.dismissedVersion()
                val available = policy?.takeIf { current != null && it.isAvailable(current) }
                mutableState.value = UpdateUiState(
                    update = available,
                    showPrompt = available?.shouldPrompt(current!!, dismissed, manual) == true,
                    message = if (manual && available == null) {
                        if (policy == null) "Не удалось проверить обновления. Попробуйте позже." else "Установлена актуальная версия"
                    } else null,
                )
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                mutableState.value = UpdateUiState(message = if (manual) "Не удалось проверить обновления. Попробуйте позже." else null)
            }
        }
    }

    fun dismiss() {
        val update = mutableState.value.update ?: return
        if (update.isRequired(AppConfig.versionCode ?: return)) return
        dismissed = update.versionCode
        mutableState.value = mutableState.value.copy(showPrompt = false)
        scope.launch {
            try { repository.dismiss(update.versionCode) } catch (error: Exception) {
                if (error is CancellationException) throw error
            }
        }
    }

}
