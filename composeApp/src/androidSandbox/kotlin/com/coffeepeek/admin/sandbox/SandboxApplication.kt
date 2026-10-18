package com.coffeepeek.admin.sandbox

import android.app.Application
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.coffeepeek.R
import com.coffeepeek.admin.CoffeePeekApplication
import com.coffeepeek.admin.di.initAndroidKoin
import com.coffeepeek.admin.di.shop.AndroidShopDrinkPhotoProcessor
import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.shop.impl.ui.data.ShopDrinkPhotoProcessor
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
        photoBytes = sandboxImageBytes(false)
        val cutout = sandboxImageBytes(true)
        backend = SandboxBackend(photoBytes, cutout)
        CoffeePeekApplication.bindContext(object : ContextWrapper(this) {
            override fun startActivity(intent: Intent) = blockedExternalAction()
            override fun startActivity(intent: Intent, options: Bundle?) = blockedExternalAction()
        })
        MapLibre.getInstance(this)
        val extractor = DrinkPhotoExtractor { listOf(PendingPhotoUpload("sandbox-cutout.png", "image/png", cutout.copyOf())) }
        initAndroidKoin(analyticsEnabled = false, extraModules = listOf(module {
            single<SessionRepository> { sessions }
            single<com.coffeepeek.domain.repository.AuthRepository> { SandboxAuthRepository(sessions) }
            single { CoffeePeekClient(SandboxBackend.BASE_URL, cacheDir.resolve("sandbox-http").path, false,
                getToken = { null }, saveToken = {}, apiClientFactory = backend::apiClient,
                uploadClientFactory = backend::uploadClient) }
            single<DrinkPhotoExtractor> { extractor }
            single<ShopDrinkPhotoProcessor> { AndroidShopDrinkPhotoProcessor(extractor) }
            single<io.kamel.core.config.KamelConfig> { sandboxImageConfig(backend.gateway) }
            single<com.coffeepeek.admin.feature.appupdate.api.AndroidUpdateInstaller> {
                SandboxUpdateInstaller(getString(R.string.sandbox_external_blocked))
            }
        }))
    }

    fun blockedExternalAction() {
        android.os.Handler(mainLooper).post { Toast.makeText(this, R.string.sandbox_external_blocked, Toast.LENGTH_SHORT).show() }
    }
}
