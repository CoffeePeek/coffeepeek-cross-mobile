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
    private val lock = Mutex()
    private val mutable = MutableStateFlow(NotificationInboxState())
    override val state = mutable.asStateFlow()
    private var activityAt: TimeSource.Monotonic.ValueTimeMark? = null
    private var loggingOut = false
    private var loadJob: Job? = null
    private var settingsJob: Job? = null
    private var readJob: Job? = null
    private var refreshPending = false
    private fun account() = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId
    private fun current(snapshot: NotificationInboxState) = !loggingOut && account() == snapshot.accountId &&
        state.value.generation == snapshot.generation

    init {
        scope.launch {
            sessions.observeSession().map { it?.takeIf(sessions::isActiveSession)?.userId }.distinctUntilChanged().collect { user ->
                loadJob?.cancel(); settingsJob?.cancel(); readJob?.cancel()
                refreshPending = false
                loggingOut = false
                activityAt = null
                mutable.value = NotificationInboxState(accountId = user, generation = state.value.generation + 1)
                platform.setAccount(user)
                if (user != null) foreground()
            }
        }
        scope.launch {
            var receivedRevision = platform.state.value.notificationRevision
            platform.state.collectLatest { device ->
                mutable.update { it.copy(deviceAvailable = device.available, permissionGranted = device.permissionGranted) }
                if (receivedRevision != device.notificationRevision) {
                    receivedRevision = device.notificationRevision
                    refresh()
                }
                syncDevice()
            }
        }
    }

    private suspend fun syncDevice(refreshSettings: Boolean = false) = lock.withLock {
        val snapshot = state.value
        val user = snapshot.accountId ?: return@withLock
        if (!current(snapshot)) return@withLock
        if (!snapshot.settingsLoaded || refreshSettings) {
            try {
                val enabled = repository.pushEnabled()
                if (!current(snapshot)) return@withLock
                mutable.update { it.copy(pushEnabled = enabled, settingsLoaded = true, settingsError = null) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (current(snapshot)) mutable.update { it.copy(settingsError = "Не удалось загрузить настройки push") }
                return@withLock
            }
        }
        val device = platform.state.value
        if (!current(snapshot) || device.userId != user || device.installationId.isBlank() || !device.available) return@withLock
        try {
            if (state.value.pushEnabled && device.permissionGranted && !device.token.isNullOrBlank())
                repository.register(device.installationId, device.token)
            else if (!state.value.pushEnabled || !device.permissionGranted) repository.remove(device.installationId)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Retry device synchronization on foreground entry or a token update. */ }
    }

    override fun foreground() {
        scope.launch {
            syncDevice(refreshSettings = true)
            val snapshot = state.value
            if (snapshot.accountId == null || !current(snapshot) || (activityAt?.elapsedNow()?.inWholeSeconds ?: 60) < 60) return@launch
            try {
                repository.activity()
                if (current(snapshot)) activityAt = TimeSource.Monotonic.markNow()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { }
        }
        refresh()
    }

    override fun refresh() = load(more = false)
    override fun loadMore() = load(more = true)

    private fun load(more: Boolean) {
        val snapshot = state.value
        if (snapshot.accountId == null || !current(snapshot)) return
        if (loadJob?.isActive == true || snapshot.markingRead) {
            if (!more) refreshPending = true
            return
        }
        val cursor = if (more) snapshot.nextCursor ?: return else null
        if (!more) refreshPending = false
        mutable.update { it.copy(loading = !more, loadingMore = more, error = null) }
        loadJob = scope.launch {
            try {
                val page = repository.inbox(cursor)
                if (!current(snapshot)) return@launch
                val items = page.items.map { notification ->
                    NotificationItem(notification.id,
                        NotificationKind.entries.firstOrNull { it.name == notification.type } ?: NotificationKind.General,
                        notification.title, notification.body, notification.createdAtUtc, notification.readAtUtc != null, notification.status)
                }
                mutable.update { it.copy(items = if (more) (it.items + items).distinctBy(NotificationItem::id) else items,
                    unreadCount = page.unreadCount, nextCursor = page.nextCursor, loading = false, loadingMore = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (current(snapshot)) mutable.update { it.copy(loading = false, loadingMore = false, error = "Не удалось загрузить уведомления") }
            } finally {
                if (current(snapshot)) {
                    loadJob = null
                    if (refreshPending) refresh()
                }
            }
        }
        if (!snapshot.settingsLoaded) scope.launch { syncDevice() }
    }

    override fun setPushEnabled(enabled: Boolean) {
        val snapshot = state.value
        if (snapshot.accountId == null || !current(snapshot) || !snapshot.settingsLoaded || snapshot.savingSettings || !snapshot.deviceAvailable) return
        mutable.update { it.copy(savingSettings = true, settingsError = null) }
        settingsJob = scope.launch {
            try {
                lock.withLock {
                    if (!current(snapshot)) return@withLock
                    repository.setPushEnabled(enabled)
                    if (current(snapshot)) mutable.update { it.copy(pushEnabled = enabled, savingSettings = false) }
                }
                if (!current(snapshot)) return@launch
                if (enabled && !platform.state.value.permissionGranted) platform.requestPermissionFromSettings()
                syncDevice()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (current(snapshot)) mutable.update { it.copy(savingSettings = false, settingsError = "Не удалось сохранить настройки push. Попробуйте ещё раз.") }
            }
        }
    }

    override fun markRead(id: String) = read(id)
    override fun markAllRead() = read(null)

    private fun read(id: String?) {
        val snapshot = state.value
        if (snapshot.accountId == null || !current(snapshot) || snapshot.markingRead || snapshot.loading || snapshot.loadingMore) return
        val item = if (id == null) snapshot.items.firstOrNull() else snapshot.items.firstOrNull { it.id == id && !it.read }
        if (item == null || snapshot.unreadCount == 0) return
        mutable.update { it.copy(markingRead = true, error = null) }
        readJob = scope.launch {
            try {
                if (id == null) repository.markAllRead(item.id) else repository.markRead(id)
                if (!current(snapshot)) return@launch
                mutable.update { value -> value.copy(markingRead = false,
                    items = value.items.map { if (id == null || it.id == id) it.copy(read = true) else it },
                    unreadCount = if (id == null) 0 else (value.unreadCount - 1).coerceAtLeast(0)) }
                // Reload the count: messages arriving after this snapshot must stay unread.
                refresh()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (current(snapshot)) mutable.update { it.copy(markingRead = false, error = "Не удалось отметить уведомления прочитанными") }
            }
        }
    }

    override fun onCheckInCreated() {
        val snapshot = state.value
        val device = platform.state.value
        if (snapshot.accountId != null && current(snapshot) && snapshot.settingsLoaded && snapshot.pushEnabled && device.available && !device.permissionGranted)
            platform.requestPermission()
    }

    override suspend fun beforeLogout() {
        loggingOut = true
        loadJob?.cancel(); settingsJob?.cancel(); readJob?.cancel()
        mutable.value = NotificationInboxState(generation = state.value.generation + 1)
        try {
            withTimeoutOrNull(3_000) { lock.withLock { if (account() != null && platform.state.value.installationId.isNotBlank()) repository.remove(platform.state.value.installationId) } }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { }
        finally { platform.setAccount(null) }
    }
}
