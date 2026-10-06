package com.coffeepeek.admin.feature.appupdate.api

import android.content.Intent
import android.content.IntentSender
import com.coffeepeek.admin.feature.appupdate.domain.UpdateInstaller

internal interface AndroidUpdateInstaller : UpdateInstaller {
    var requestConsent: ((IntentSender) -> Unit)?
    var requestInstallation: ((Intent) -> Unit)?
    fun consentResult(accepted: Boolean)
    fun installationResult()
}
