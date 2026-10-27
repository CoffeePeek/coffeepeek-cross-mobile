package com.coffeepeek.feature.appupdate.impl

import com.coffeepeek.feature.appupdate.domain.InstallationStage
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

data class PlayUpdateRetryOutcome(val message: String, val canRetry: Boolean)

/** A retry covers both the backend policy check and Google Play's response. */
suspend fun retryPlayUpdate(controller: AppUpdateController, installer: UpdateInstaller): PlayUpdateRetryOutcome? = try {
    withTimeout(30_000) {
        controller.state.first { !it.checking }
        controller.check(manual = true)
        val policy = controller.state.first { !it.checking }
        if (policy.update == null) {
            PlayUpdateRetryOutcome(policy.message ?: RetryFailure, policy.checkFailed)
        } else {
            controller.takeNativePrompt(installer.state.value)?.let(installer::download)
            val transfer = installer.state.first { it.stage != InstallationStage.Starting }
            transfer.error?.let { PlayUpdateRetryOutcome(it, canRetry = true) }
        }
    }
} catch (error: Exception) {
    if (error is CancellationException && error !is TimeoutCancellationException) throw error
    PlayUpdateRetryOutcome(RetryFailure, canRetry = true)
}

private const val RetryFailure = "Не удалось проверить обновления. Попробуйте позже."
