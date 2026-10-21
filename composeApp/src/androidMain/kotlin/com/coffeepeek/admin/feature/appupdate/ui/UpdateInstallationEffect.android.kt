package com.coffeepeek.admin.feature.appupdate.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.coffeepeek.admin.feature.appupdate.api.AndroidUpdateInstaller
import com.coffeepeek.feature.appupdate.domain.InstallationStage
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.appupdate.impl.PlayUpdateRetryOutcome
import com.coffeepeek.feature.appupdate.impl.retryPlayUpdate
import com.coffeepeek.BuildConfig
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CpSnackbarHost
import com.coffeepeek.admin.ui.icons.CpIcons
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.update_downloaded_message
import coffeepeek.composeapp.generated.resources.update_restart_action
import coffeepeek.composeapp.generated.resources.update_retry_action
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@Composable
internal actual fun UpdateInstallationEffect() {
    val installer = koinInject<AndroidUpdateInstaller>()
    val controller = koinInject<AppUpdateController>()
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        installer.consentResult(it.resultCode)
        if (it.resultCode == Activity.RESULT_CANCELED) controller.dismiss()
    }
    val installation = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        installer.installationResult()
    }
    DisposableEffect(installer, consent, installation) {
        installer.requestConsent = { consent.launch(IntentSenderRequest.Builder(it).build()) }
        installer.requestInstallation = { installation.launch(it) }
        onDispose {
            installer.requestConsent = null
            installer.requestInstallation = null
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { installer.refresh() }
    if (BuildConfig.APK_UPDATES_ENABLED) return

    val policy by controller.state.collectAsState()
    val transfer by installer.state.collectAsState()
    var manualNoticeRequest by remember { mutableIntStateOf(0) }
    var retrying by remember { mutableStateOf(false) }
    var retryMessage by remember { mutableStateOf<String?>(null) }
    var retryOutcome by remember { mutableStateOf<PlayUpdateRetryOutcome?>(null) }
    var hideRetryResult by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(policy.showPrompt) {
        // A manual check can reveal a previously dismissed downloaded update as well.
        if (policy.showPrompt && policy.manual) {
            manualNoticeRequest++
            if (!retrying) {
                hideRetryResult = false
                retryOutcome = null
            }
        }
        controller.takeNativePrompt(transfer)?.let(installer::download)
    }
    LaunchedEffect(transfer.stage) {
        if (transfer.stage == InstallationStage.Ready) {
            hideRetryResult = false
            retryOutcome = null
        }
    }

    val required = (policy.update ?: transfer.update)?.isRequired(BuildConfig.VERSION_CODE.toLong()) == true
    val snackbar = remember { SnackbarHostState() }
    val readyMessage = stringResource(Res.string.update_downloaded_message)
    val restart = stringResource(Res.string.update_restart_action)
    val retry = stringResource(Res.string.update_retry_action)
    LaunchedEffect(transfer.stage, transfer.error, required, manualNoticeRequest, retrying, retryOutcome, hideRetryResult) {
        if (required && transfer.stage == InstallationStage.Ready && transfer.error == null) {
            installer.install()
        } else if (!required && !hideRetryResult) {
            val ready = transfer.stage == InstallationStage.Ready
            val message = if (retrying) retryMessage ?: return@LaunchedEffect
                else retryOutcome?.message ?: transfer.error ?: if (ready) readyMessage else return@LaunchedEffect
            val action = when {
                retrying -> retry
                retryOutcome != null -> retry.takeIf { retryOutcome?.canRetry == true }
                ready && transfer.error == null -> restart
                else -> retry
            }
            if (snackbar.showSnackbar(
                    message = message,
                    actionLabel = action,
                    withDismissAction = true,
                    duration = if (retrying) SnackbarDuration.Indefinite else SnackbarDuration.Long,
                ) == SnackbarResult.ActionPerformed
            ) {
                if (ready) installer.install() else controller.check(manual = true)
            } else {
                hideRetryResult = true
                controller.dismiss()
            }
        }
    }
    Box(
        Modifier.fillMaxSize().padding(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                CpDimens.floatingNavContentClearance + CpDimens.spacing2,
        ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        CpSnackbarHost(
            hostState = snackbar,
            actionIcon = CpIcons.Retry.takeIf { snackbar.currentSnackbarData?.visuals?.actionLabel == retry },
            actionInProgress = retrying,
            onAction = if (snackbar.currentSnackbarData?.visuals?.actionLabel == retry) ({
                if (!retrying) {
                    retryMessage = snackbar.currentSnackbarData?.visuals?.message
                    retryOutcome = null
                    hideRetryResult = false
                    retrying = true
                    scope.launch {
                        try { retryOutcome = retryPlayUpdate(controller, installer) }
                        finally { retrying = false }
                    }
                }
            }) else null,
        )
    }
}
