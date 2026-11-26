package com.coffeepeek.feature.notifications.impl

import com.coffeepeek.feature.notifications.api.*
import com.coffeepeek.feature.notifications.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationCitySyncTest {
    @Test fun loadingNeverClearsTheServerAndAResolvedCitySyncsWithoutPushPermission() = runTest {
        val fixture = CitySyncFixture(backgroundScope)
        fixture.account.user.value = "alice"
        runCurrent()
        fixture.feature.foreground()
        runCurrent()
        assertTrue(fixture.repository.calls.isEmpty())
        fixture.cities.select("minsk")
        runCurrent()
        assertEquals(listOf<Pair<String?, String?>>("alice" to "minsk"), fixture.repository.calls)
        assertFalse(fixture.feature.state.value.pushEnabled)
        assertEquals(0, fixture.platform.permissionRequests)
    }

    @Test fun aGuestWaitsForLoginAndExplicitResolvedEmptySelectionSendsNull() = runTest {
        val fixture = CitySyncFixture(backgroundScope)
        fixture.cities.select("minsk")
        runCurrent()
        assertTrue(fixture.repository.calls.isEmpty())
        fixture.account.user.value = "alice"
        runCurrent()
        fixture.cities.select(null)
        runCurrent()
        assertEquals(listOf<Pair<String?, String?>>("alice" to "minsk", "alice" to null), fixture.repository.calls)
    }

    @Test fun automaticAndManualChangesAreSerializedAndOnlyTheLatestPendingCityIsWritten() = runTest {
        val fixture = CitySyncFixture(backgroundScope)
        val release = CompletableDeferred<Unit>()
        fixture.repository.onCity = { if (it == "minsk") release.await() }
        fixture.cities.select("minsk")
        fixture.account.user.value = "alice"
        runCurrent()
        fixture.cities.select("brest")
        fixture.cities.select("grodno")
        runCurrent()
        assertEquals(listOf<Pair<String?, String?>>("alice" to "minsk"), fixture.repository.calls)
        release.complete(Unit)
        runCurrent()
        assertEquals(listOf<Pair<String?, String?>>("alice" to "minsk", "alice" to "grodno"), fixture.repository.calls)
    }

    @Test fun failedCityRequestsRetryOnForegroundWithoutBlockingInboxOrChangingPushSettings() = runTest {
        val fixture = CitySyncFixture(backgroundScope)
        fixture.repository.onCity = { error("HTTP 404") }
        fixture.cities.select("minsk")
        fixture.account.user.value = "alice"
        runCurrent()
        assertTrue(fixture.feature.state.value.settingsLoaded)
        fixture.repository.onCity = {}
        fixture.feature.foreground()
        runCurrent()
        assertEquals(listOf<Pair<String?, String?>>("alice" to "minsk", "alice" to "minsk"), fixture.repository.calls)
        assertEquals(0, fixture.repository.settingsWrites)
        assertEquals(0, fixture.platform.permissionRequests)
    }

    @Test fun accountSwitchCancelsTheOldCityRequestAndSyncsTheNewAccount() = runTest {
        val fixture = CitySyncFixture(backgroundScope)
        val cancelled = CompletableDeferred<Unit>()
        fixture.repository.onCity = {
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        fixture.cities.select("minsk")
        fixture.account.user.value = "alice"
        runCurrent()
        fixture.repository.onCity = {}
        fixture.account.user.value = "bob"
        runCurrent()
        assertTrue(cancelled.isCompleted)
        assertEquals(listOf<Pair<String?, String?>>("alice" to "minsk", "bob" to "minsk"), fixture.repository.calls)
        assertEquals("bob", fixture.feature.state.value.accountId)
    }

    @Test fun aNoncancellableOldWriteFinishesBeforeTheCurrentCityAfterAccountReturn() = runTest {
        val fixture = CitySyncFixture(backgroundScope)
        val delayed = DelayedCityWrite()
        fixture.repository.onCity = delayed::write
        fixture.cities.select("minsk")
        fixture.account.user.value = "alice"
        runCurrent()
        fixture.account.user.value = "bob"
        runCurrent()
        fixture.cities.select("brest")
        fixture.account.user.value = "alice"
        runCurrent()
        assertEquals(listOf<Pair<String?, String?>>("alice" to "minsk"), fixture.repository.calls)
        delayed.release.complete(Unit)
        runCurrent()
        assertEquals(listOf<String?>("minsk", "brest"), delayed.completed)
        assertEquals(listOf<Pair<String?, String?>>("alice" to "minsk", "alice" to "brest"), fixture.repository.calls)
    }

    @Test fun returningToAnAccountResendsTheCurrentCityAndLogoutStopsFurtherWrites() = runTest {
        val fixture = CitySyncFixture(backgroundScope)
        fixture.cities.select("minsk")
        fixture.account.user.value = "alice"
        runCurrent()
        fixture.account.user.value = "bob"
        runCurrent()
        fixture.account.user.value = "alice"
        runCurrent()
        assertEquals(listOf<String?>("alice", "bob", "alice"), fixture.repository.calls.map { it.first })
        fixture.feature.beforeLogout()
        fixture.cities.select("brest")
        fixture.feature.foreground()
        runCurrent()
        assertEquals(3, fixture.repository.calls.size)
        assertNull(fixture.feature.state.value.accountId)
    }
}

private class CitySyncFixture(scope: CoroutineScope) {
    val account = CitySyncAccount()
    val cities = CitySyncSelection()
    val platform = CitySyncPlatform()
    val repository = CitySyncRepository { account.userId }
    val feature = NotificationPresenter(repository, account, platform, scope, cities)
}

private class CitySyncAccount : NotificationAccount {
    val user = MutableStateFlow<String?>(null)
    override val userId get() = user.value
    override fun observeUserId() = user
}

private class CitySyncSelection : NotificationCity {
    private val selected = MutableSharedFlow<NotificationCitySelection>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override fun observeSelectedCity() = selected
    fun select(slug: String?) { selected.tryEmit(NotificationCitySelection(slug)) }
}

private class CitySyncPlatform : PushDevicePlatform {
    private val mutable = MutableStateFlow(PushDeviceState())
    override val state = mutable.asStateFlow()
    var permissionRequests = 0
    override fun setAccount(userId: String?) { mutable.value = mutable.value.copy(userId = userId) }
    override fun requestPermission() { permissionRequests++ }
}

private class CitySyncRepository(private val account: () -> String?) : NotificationRepository {
    val calls = mutableListOf<Pair<String?, String?>>()
    var onCity: suspend (String?) -> Unit = {}
    var settingsWrites = 0
    override suspend fun setCity(citySlug: String?) {
        calls += account() to citySlug
        onCity(citySlug)
    }
    override suspend fun inbox(before: String?) = NotificationPage(emptyList(), null, 0)
    override suspend fun destination(notificationId: String): NotificationTarget? = null
    override suspend fun pushEnabled() = false
    override suspend fun setPushEnabled(enabled: Boolean) { settingsWrites++ }
    override suspend fun markRead(id: String) = Unit
    override suspend fun markAllRead(throughId: String) = Unit
    override suspend fun register(installationId: String, token: String) = Unit
    override suspend fun remove(installationId: String) = Unit
    override suspend fun activity() = Unit
}

private class DelayedCityWrite {
    val release = CompletableDeferred<Unit>()
    val completed = mutableListOf<String?>()
    private var first = true
    suspend fun write(citySlug: String?) {
        if (first) {
            first = false
            withContext(NonCancellable) { release.await() }
        }
        completed += citySlug
    }
}
