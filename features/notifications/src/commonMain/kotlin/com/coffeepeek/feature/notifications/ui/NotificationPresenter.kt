package com.coffeepeek.feature.notifications.ui

import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.notifications.api.*
import com.coffeepeek.feature.notifications.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

internal class NotificationPresenter(
    private val repository: NotificationRepository,
    private val sessions: SessionRepository,
    private val platform: PushDevicePlatform,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : NotificationFeature {
    private val mutable = MutableStateFlow(NotificationSettingsState())
    override val state = mutable.asStateFlow()
    override val device = platform.state
    private val lock = Mutex()
    private var activityAt: TimeSource.Monotonic.ValueTimeMark? = null
    private var loggingOut = false
    private fun account() = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId

    init {
        scope.launch {
            sessions.observeSession().map { it?.takeIf(sessions::isActiveSession)?.userId }.distinctUntilChanged().collectLatest { user ->
                loggingOut = false
                activityAt = null
                mutable.value = NotificationSettingsState(signedIn = user != null)
                platform.setAccount(user)
                if (user != null) { refresh(); foreground() }
            }
        }
        scope.launch { platform.state.collectLatest { syncDevice() } }
    }

    private suspend fun syncDevice() = lock.withLock {
        val user = account() ?: return@withLock
        val device = device.value
        if (loggingOut || device.userId != user || device.installationId.isBlank() || !device.available) return@withLock
        try {
            if (device.permissionGranted && !device.token.isNullOrBlank()) repository.register(device.installationId, device.token)
            else if (!device.permissionGranted) repository.remove(device.installationId)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Retry registration on the next foreground or token update. */ }
    }

    override fun foreground() {
        scope.launch {
            syncDevice()
            val user = account() ?: return@launch
            if (loggingOut || (activityAt?.elapsedNow()?.inWholeSeconds ?: 60) < 60) return@launch
            try {
                repository.activity()
                if (account() == user) activityAt = TimeSource.Monotonic.markNow()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { }
        }
    }

    override fun refresh() = load(null)
    override fun save(value: NotificationPreferences) = load(value)
    private fun load(value: NotificationPreferences?) {
        val user = account() ?: return
        scope.launch {
            lock.withLock {
                if (account() != user || loggingOut) return@withLock
                mutable.update { it.copy(loading = true, error = null) }
                try {
                    val preferences = if (value == null) repository.preferences() else { repository.save(value); value }
                    if (account() == user) mutable.value = NotificationSettingsState(true, preferences = preferences)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    if (account() == user) mutable.update { it.copy(loading = false, error = "Не удалось сохранить или загрузить настройки. Повторите попытку.") }
                }
            }
        }
    }

    override fun requestPermission() = platform.requestPermission()
    override suspend fun beforeLogout() {
        loggingOut = true
        try {
            withTimeoutOrNull(3_000) { lock.withLock { if (account() != null && device.value.installationId.isNotBlank()) repository.remove(device.value.installationId) } }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { }
        finally { platform.setAccount(null) }
    }
}
