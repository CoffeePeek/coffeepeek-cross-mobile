package com.coffeepeek.admin.feature.appupdate.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.coffeepeek.admin.feature.appupdate.api.AndroidUpdateInstaller
import org.koin.compose.koinInject

@Composable
internal actual fun UpdateInstallationEffect() {
    val installer = koinInject<AndroidUpdateInstaller>()
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        installer.consentResult(it.resultCode == Activity.RESULT_OK)
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
}
