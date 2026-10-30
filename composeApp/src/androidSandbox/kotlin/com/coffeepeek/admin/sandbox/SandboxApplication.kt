package com.coffeepeek.admin.sandbox

import android.app.Application
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.coffeepeek.R
import com.coffeepeek.admin.CoffeePeekApplication
import com.coffeepeek.admin.di.initAndroidKoin
import com.coffeepeek.admin.di.shelf.shelfModels
import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.shelf.data.scan.BagScannerModelSource
import com.coffeepeek.feature.shelf.data.scan.createAndroidBagScanner
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import org.koin.dsl.module
import org.maplibre.android.MapLibre

/** Production screens/graph, isolated bootstrap: no realtime, analytics, updater or real session. */
internal class SandboxApplication : Application() {
    lateinit var backend: SandboxBackend
        private set
    val sessions = SandboxSessionRepository()
    lateinit var photoBytes: ByteArray
        private set

    override fun onCreate() {
        super.onCreate()
        photoBytes = sandboxImageBytes()
        backend = SandboxBackend(photoBytes, bagCard = ::sandboxBagCardBytes)
        CoffeePeekApplication.bindContext(object : ContextWrapper(this) {
            override fun startActivity(intent: Intent) = blockedExternalAction()
            override fun startActivity(intent: Intent, options: Bundle?) = blockedExternalAction()
        })
        MapLibre.getInstance(this)
        initAndroidKoin(analyticsEnabled = false, extraModules = listOf(module {
            single<SessionStore> { sessions }
            single { ApiClients(SandboxBackend.BASE_URL, cacheDir.resolve("sandbox-http").path, false,
                credentials = SandboxNoCredentials,
                refreshWith = { { Result.failure(IllegalStateException("The Sandbox has no account to refresh")) } },
                requireHttps = false, apiClientFactory = backend::apiClient,
                uploadClientFactory = backend::uploadClient) }
            single<io.kamel.core.config.KamelConfig> { sandboxImageConfig(backend.gateway) }
            single<com.coffeepeek.feature.appupdate.api.AndroidUpdateInstaller> {
                SandboxUpdateInstaller(getString(R.string.sandbox_external_blocked))
            }
            single<BagScanner> {
                // The Sandbox has no network: both models come from the files dir, pushed there with adb as for the probe.
                val pushed = BagScannerModelSource.directory { getExternalFilesDir(null) }
                val models = shelfModels(pushed, pushed)
                createAndroidBagScanner({ this@SandboxApplication }, models.segmenter, models.inpainter)
            }
        }))
    }

    fun blockedExternalAction() {
        android.os.Handler(mainLooper).post { Toast.makeText(this, R.string.sandbox_external_blocked, Toast.LENGTH_SHORT).show() }
    }
}

/** The fake backend is open: the Sandbox transport never carries credentials. */
private object SandboxNoCredentials : SessionCredentials {
    override suspend fun load(): SessionTokens? = null
    override suspend fun save(tokens: SessionTokens?) = Unit
    override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?) = false
}
