package com.coffeepeek.admin

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.coffeepeek.admin.theme.applyPersistedNightModeEarly
import com.coffeepeek.admin.analytics.ProductAnalytics
import com.coffeepeek.admin.locator.Locator
import com.posthog.kmp.PostHogContext
import com.coffeepeek.admin.auth.SessionRealtimeManager
import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.domain.repository.SessionRepository
import org.maplibre.android.MapLibre
import io.kamel.core.config.KamelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class CoffeePeekApplication : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var _context: Context? = null
        val context get() = _context!!
    }

    private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sessionRealtimeManager: SessionRealtimeManager? = null

    override fun onCreate() {
        super.onCreate()
        _context = this
        // Apply saved light/dark mode before any Activity/splash resolves resources.
        applyPersistedNightModeEarly()
        MapLibre.getInstance(this)
        initPlatformKoin()

        val koin = GlobalContext.get()
        val sessionRepository = koin.get<SessionRepository>()
        ProductAnalytics.start(PostHogContext(this), Locator.database.settingRepository, sessionRepository)
        sessionRealtimeManager = SessionRealtimeManager(
            sessionRepository = sessionRepository,
            userSessionCleaner = koin.get(),
            httpClient = koin.get<CoffeePeekClient>().client,
            baseUrl = AppConfig.baseUrl,
        ).also { manager -> manager.start() }

        warmupScope.launch {
            runCatching { sessionRepository.warmCache() }
            runCatching { koin.get<KamelConfig>() }
        }
    }

    override fun onTerminate() {
        sessionRealtimeManager?.close()
        sessionRealtimeManager = null
        _context = null
        super.onTerminate()
    }
}
