package com.coffeepeek.admin.feature.appupdate.data

import android.content.Context
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.coffeepeek.BuildConfig
import com.coffeepeek.admin.feature.appupdate.api.AndroidUpdateInstaller
import com.coffeepeek.admin.feature.appupdate.domain.AppUpdate
import com.coffeepeek.admin.feature.appupdate.domain.InstallationStage
import com.coffeepeek.admin.feature.appupdate.domain.InstallationState
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.common.IntentSenderForResultStarter
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PlayUpdateInstaller(context: Context) : AndroidUpdateInstaller {
    private val manager = AppUpdateManagerFactory.create(context)
    private val mutableState = MutableStateFlow(InstallationState())
    override val state = mutableState.asStateFlow()
    override var requestConsent: ((IntentSender) -> Unit)? = null
    override var requestInstallation: ((Intent) -> Unit)? = null
    private var updateType = AppUpdateType.FLEXIBLE
    private val listener = InstallStateUpdatedListener { install ->
        when (install.installStatus()) {
            InstallStatus.DOWNLOADING -> mutableState.value = state.value.copy(
                stage = InstallationStage.Downloading,
                downloadedBytes = install.bytesDownloaded(), totalBytes = install.totalBytesToDownload(), error = null,
            )
            InstallStatus.DOWNLOADED -> mutableState.value = state.value.copy(
                stage = if (updateType == AppUpdateType.IMMEDIATE) InstallationStage.Installing else InstallationStage.Ready,
                error = null,
            )
            InstallStatus.INSTALLING -> mutableState.value = state.value.copy(stage = InstallationStage.Installing, error = null)
            InstallStatus.FAILED -> failed()
            InstallStatus.CANCELED -> consentResult(Activity.RESULT_CANCELED)
        }
    }

    init { manager.registerListener(listener) }

    override fun download(update: AppUpdate) {
        if (state.value.busy) return
        mutableState.value = InstallationState(update, InstallationStage.Starting)
        updateType = if (update.isRequired(BuildConfig.VERSION_CODE.toLong())) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.availableVersionCode().toLong() < update.minVersionCode) {
                failed("Обязательное обновление пока недоступно для вашего аккаунта Google Play. Попробуйте позже.")
            } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                mutableState.value = state.value.copy(stage = InstallationStage.Ready)
            } else if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startFlow(info, AppUpdateType.IMMEDIATE)
            } else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(updateType)) {
                startFlow(info, updateType)
            } else failed("Обновление пока недоступно для вашего аккаунта Google Play. Попробуйте позже.")
        }.addOnFailureListener { failed() }
    }

    override fun refresh() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            val update = state.value.update ?: AppUpdate(info.availableVersionCode().toLong(), 0, "")
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                if (state.value.stage != InstallationStage.Starting) {
                    mutableState.value = state.value.copy(update = update, stage = InstallationStage.Starting, error = null)
                    startFlow(info, AppUpdateType.IMMEDIATE)
                }
                return@addOnSuccessListener
            }
            when (info.installStatus()) {
                InstallStatus.DOWNLOADED -> mutableState.value = state.value.copy(update = update, stage = InstallationStage.Ready, error = null)
                InstallStatus.DOWNLOADING, InstallStatus.PENDING -> mutableState.value = state.value.copy(
                    update = update, stage = InstallationStage.Downloading,
                    downloadedBytes = info.bytesDownloaded(), totalBytes = info.totalBytesToDownload(),
                )
                InstallStatus.INSTALLING -> mutableState.value = state.value.copy(update = update, stage = InstallationStage.Installing, error = null)
            }
        }
    }

    override fun consentResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                if (state.value.stage == InstallationStage.Starting) mutableState.value = state.value.copy(stage = InstallationStage.Downloading)
                if (updateType == AppUpdateType.FLEXIBLE) refresh()
            }
            Activity.RESULT_CANCELED -> {
                if (updateType == AppUpdateType.IMMEDIATE) failed("Чтобы продолжить, установите обязательное обновление через Google Play.")
                else mutableState.value = state.value.copy(stage = InstallationStage.Available, error = null)
            }
            else -> failed()
        }
    }

    override fun install() {
        if (state.value.stage != InstallationStage.Ready) return
        mutableState.value = state.value.copy(stage = InstallationStage.Installing, error = null)
        manager.completeUpdate().addOnFailureListener { failed("Не удалось установить обновление. Попробуйте ещё раз.", InstallationStage.Ready) }
    }

    override fun installationResult() = Unit
    override fun cancel() = Unit

    private fun startFlow(info: AppUpdateInfo, type: Int) {
        updateType = type
        try {
            val launch = checkNotNull(requestConsent)
            val starter = IntentSenderForResultStarter { sender, _, _, _, _, _, _ -> launch(sender) }
            if (!manager.startUpdateFlowForResult(info, starter, AppUpdateOptions.newBuilder(type).build(), 0)) failed()
        } catch (_: Exception) { failed() }
    }

    private fun failed(message: String = "Не удалось запустить обновление через Google Play. Попробуйте ещё раз.", stage: InstallationStage = InstallationStage.Available) {
        mutableState.value = state.value.copy(stage = stage, error = message)
    }
}
