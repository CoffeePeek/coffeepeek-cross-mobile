package com.coffeepeek.feature.homecheckin.impl

import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeHomeDetailSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = mutableListOf<NativeHomeDetailSession>()
    private var allowed = true
    private var reads = 0
    private var deletes = 0
    private var read: suspend () -> Result<HomePreparation> = { Result.success(entry()) }
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() {
        owners.forEach { it.close() }
        Dispatchers.resetMain()
    }
    private fun owner(): NativeHomeDetailSession {
        val repository = object : HomePreparationRepository {
            override suspend fun detail(id: String): Result<HomePreparation> {
                reads++
                return read()
            }
            override suspend fun delete(id: String): Result<Unit> {
                deletes++
                return Result.success(Unit)
            }
            override suspend fun create(input: HomePreparationInput): Result<HomePreparation> = error("Not an editor")
            override suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation> = error("Not an editor")
            override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> = error("Not history")
            override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> = error("Not history")
        }
        return NativeHomeDetailSession(repository, { allowed }, "home", 4).also(owners::add)
    }
    @Test fun finalDisposalCancelsTheFeatureReadAndRejectsAnotherLoad() = runTest(dispatcher) {
        var cancelled = false
        read = { try { awaitCancellation() } finally { cancelled = true } }
        val owner = owner()
        runCurrent()
        owner.close()
        owner.close()
        owner.action(HomeCheckInDetailAction.Reload)
        runCurrent()
        assertTrue(cancelled)
        assertFalse(owner.isCurrent())
        assertEquals(1, reads)
    }
    @Test fun revokedUiCannotConfirmDeletionBeforeTheNavigationOwnerDisposesIt() = runTest(dispatcher) {
        val owner = owner()
        runCurrent()
        owner.action(HomeCheckInDetailAction.RequestDelete)
        runCurrent()
        allowed = false
        owner.action(HomeCheckInDetailAction.ConfirmDelete)
        runCurrent()
        assertEquals(0, deletes)
        assertFalse(owner.isCurrent())
    }
    @Test fun aBufferedEditCannotNavigateAfterTheNativeDetailWasDisposed() = runTest(dispatcher) {
        val owner = owner()
        runCurrent()
        owner.action(HomeCheckInDetailAction.Edit)
        runCurrent()
        owner.close()
        val events = mutableListOf<HomeCheckInDetailEventEnvelope>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { owner.events.collect { events += it } }
        runCurrent()
        assertTrue(events.isEmpty())
    }
    private fun entry() = HomePreparation("home", "2026-11-25", "2026-11-25T09:00:00Z", "2026-11-25T09:00:00Z",
        "Private", null, null, "Coffee", null, null, null, null, null, null, emptyList(),
        HomePreparationVisibility.Private, HomePreparationModeration.NotSubmitted)
}
