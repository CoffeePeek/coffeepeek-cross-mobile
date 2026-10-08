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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.coffeepeek.admin.feature.appupdate.api.AndroidUpdateInstaller
import com.coffeepeek.admin.feature.appupdate.domain.InstallationStage
import com.coffeepeek.BuildConfig
import com.coffeepeek.admin.theme.CpDimens
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.update_downloaded_message
import coffeepeek.composeapp.generated.resources.update_restart_action
import coffeepeek.composeapp.generated.resources.update_retry_action
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal actual fun UpdateInstallationEffect() {
    val installer = koinInject<AndroidUpdateInstaller>()
    val controller = koinInject<AppUpdateState>()
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
    LaunchedEffect(policy.showPrompt) {
        controller.takeNativePrompt(transfer)?.let(installer::download)
    }

    val required = (policy.update ?: transfer.update)?.isRequired(BuildConfig.VERSION_CODE.toLong()) == true
    val snackbar = remember { SnackbarHostState() }
    val readyMessage = stringResource(Res.string.update_downloaded_message)
    val restart = stringResource(Res.string.update_restart_action)
    val retry = stringResource(Res.string.update_retry_action)
    LaunchedEffect(transfer.stage, transfer.error, required) {
        if (required && transfer.stage == InstallationStage.Ready && transfer.error == null) {
            installer.install()
        } else if (!required) {
            val ready = transfer.stage == InstallationStage.Ready
            val message = transfer.error ?: if (ready) readyMessage else return@LaunchedEffect
            if (snackbar.showSnackbar(
                    message = message,
                    actionLabel = if (ready && transfer.error == null) restart else retry,
                    duration = SnackbarDuration.Indefinite,
                ) == SnackbarResult.ActionPerformed
            ) {
                if (ready) installer.install() else controller.check(manual = true)
            }
        }
    }
    Box(
        Modifier.fillMaxSize().padding(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                CpDimens.floatingNavContentClearance + CpDimens.spacing2,
        ),
        contentAlignment = Alignment.BottomCenter,
    ) { SnackbarHost(snackbar) }
}
