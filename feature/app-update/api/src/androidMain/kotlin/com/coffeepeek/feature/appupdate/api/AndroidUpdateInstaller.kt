package com.coffeepeek.feature.appupdate.api

import android.content.Intent
import android.content.IntentSender
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller

interface AndroidUpdateInstaller : UpdateInstaller {
    var requestConsent: ((IntentSender) -> Unit)?
    var requestInstallation: ((Intent) -> Unit)?
    fun consentResult(resultCode: Int)
    fun installationResult()
}
