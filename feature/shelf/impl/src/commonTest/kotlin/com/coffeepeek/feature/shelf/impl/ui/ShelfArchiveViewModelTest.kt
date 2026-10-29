package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.shelf.domain.model.ShelfArchivePage
import com.coffeepeek.feature.shelf.domain.model.ShelfFailure
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveMessage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfArchiveViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(repository: FakeShelfRepository, handoff: ShelfBagHandoff = ShelfBagHandoff()) =
        ShelfArchiveViewModel(repository, testCalendar(), handoff).also { store.put("archive-${nextKey++}", it) }

    private fun TestScope.settle() { advanceTimeBy(350); runCurrent() }

    /** 25 finished bags, 20 per page: 24 in October, newest first, and the last one in September. */
    private fun pages(): (Int, String?) -> Result<ShelfArchivePage> {
        val all = (1..25).map {
            val finishedOn = if (it <= 24) "2026-10-${(26 - it).toString().padStart(2, '0')}" else "2026-09-30"
            bag("f$it", finished = true, finishedAt = "${finishedOn}T10:00:00Z")
        }
        return { page, query ->
            val matching = all.filter { query == null || it.name!!.endsWith(query) }
            Result.success(ShelfArchivePage(matching.drop((page - 1) * 20).take(20), matching.size, page, 20))
        }
    }

    @Test fun loadsPagesWithoutDuplicates() = runTest(dispatcher) {
        val repository = FakeShelfRepository().apply { archivePages = pages() }
        val model = model(repository)
        runCurrent()
        assertEquals(20, model.state.value.items.size)
        assertTrue(model.state.value.hasMore)

        model.onAction(ShelfArchiveAction.LoadMore)
        model.onAction(ShelfArchiveAction.LoadMore)
        runCurrent()
        assertEquals(25, model.state.value.items.map { it.id }.distinct().size)
        assertFalse(model.state.value.hasMore)
        assertEquals(listOf("archive:1:", "archive:2:"), repository.calls)
    }

    @Test fun typingDebouncesAndDropsTheOlderAnswer() = runTest(dispatcher) {
        val repository = FakeShelfRepository().apply { archivePages = pages() }
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfArchiveAction.QueryChanged("2"))
        runCurrent()
        model.onAction(ShelfArchiveAction.QueryChanged("25"))
        settle()
        assertEquals(listOf("archive:1:", "archive:1:25"), repository.calls)
        assertEquals(listOf("f25"), model.state.value.items.map { it.id })
    }

    @Test fun failedFirstPageCanBeRetried() = runTest(dispatcher) {
        val repository = FakeShelfRepository().apply { archivePages = { _, _ -> Result.failure(ShelfFailure(ShelfIssue.Offline)) } }
        val model = model(repository)
        runCurrent()
        assertTrue(model.state.value.loadFailed)

        repository.archivePages = pages()
        model.onAction(ShelfArchiveAction.Retry)
        runCurrent()
        assertFalse(model.state.value.loadFailed)
        assertEquals(20, model.state.value.items.size)
    }

    @Test fun aBagRestoredOrDeletedOnItsScreenLeavesTheArchive() = runTest(dispatcher) {
        val handoff = ShelfBagHandoff()
        val repository = FakeShelfRepository(emptyList()).apply { archivePages = pages() }
        val model = model(repository, handoff)
        runCurrent()
        model.onAction(ShelfArchiveAction.OpenBag("f1"))
        runCurrent()
        assertEquals(ShelfArchiveEvent.OpenBag("f1"), model.events.first())
        assertEquals("f1", handoff.opened("f1")?.id)

        handoff.report(ShelfBagOutcome.Restored("f1"))
        runCurrent()
        assertEquals(ShelfArchiveMessage.Restored, model.state.value.message)
        handoff.report(ShelfBagOutcome.Deleted("f2", archived = true))
        runCurrent()
        assertEquals(ShelfArchiveMessage.Deleted, model.state.value.message)
        assertFalse(model.state.value.items.any { it.id == "f1" || it.id == "f2" })
        assertNull(handoff.outcome.value)
    }

    @Test fun sectionsGroupByMonthNewestFirst() = runTest(dispatcher) {
        val repository = FakeShelfRepository().apply { archivePages = pages() }
        val model = model(repository)
        runCurrent()
        model.onAction(ShelfArchiveAction.LoadMore)
        runCurrent()
        val sections = model.state.value.sections
        assertEquals(listOf(2026 to 10, 2026 to 9), sections.map { it.year to it.month })
        assertEquals(listOf(24, 1), sections.map(ShelfArchiveSection::items).map { it.size })
    }
}
