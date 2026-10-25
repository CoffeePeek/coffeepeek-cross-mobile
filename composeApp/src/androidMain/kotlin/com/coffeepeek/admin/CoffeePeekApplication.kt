package com.coffeepeek.admin

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.coffeepeek.admin.theme.applyPersistedNightModeEarly
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
        com.coffeepeek.admin.di.checkinhistory.widget.ActivityCalendarWidgetRuntime.install(this)

        val koin = GlobalContext.get()
        val sessionRepository = koin.get<SessionRepository>()
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
