package com.coffeepeek.admin

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.coffeepeek.admin.theme.applyPersistedNightModeEarly
import com.coffeepeek.admin.auth.SessionRealtimeManager
import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.di.initPlatformKoin
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.core.session.SessionReader
import org.maplibre.android.MapLibre
import io.kamel.core.config.KamelConfig
import kotlinx.coroutines.CancellationException
import org.koin.core.Koin
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
        /** Bind an application-scoped context without starting production services. */
        internal fun bindContext(context: Context) { _context = context }
    }

    private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sessionRealtimeManager: SessionRealtimeManager? = null

    override fun onCreate() {
        super.onCreate()
        _context = this
        // Apply saved light/dark mode before any Activity/splash resolves resources.
        applyPersistedNightModeEarly()
        MapLibre.getInstance(this)
        com.coffeepeek.admin.notifications.AndroidPush.install(this)
        initPlatformKoin()

        val koin = GlobalContext.get()
        val sessions = koin.get<SessionReader>()
        startRealtime(koin, sessions)
        warmRuntimeCaches(koin, sessions)
    }

    private fun startRealtime(koin: Koin, sessions: SessionReader) {
        sessionRealtimeManager = SessionRealtimeManager(
            sessionRepository = sessions,
            userSessionCleaner = koin.get(),
            httpClient = koin.get<CoffeePeekClient>().client,
            baseUrl = AppConfig.baseUrl,
        ).also { manager -> manager.start() }
    }

    private fun warmRuntimeCaches(koin: Koin, sessions: SessionReader) {
        warmupScope.launch {
            try {
                sessions.getSession()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Startup remains available if secure storage is temporarily unavailable.
            }
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
