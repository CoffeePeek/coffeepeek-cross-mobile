package com.coffeepeek.feature.shelf.impl

import com.coffeepeek.feature.shelf.domain.model.ShelfArchivePage
import com.coffeepeek.feature.shelf.impl.ui.FakeShelfRepository
import com.coffeepeek.feature.shelf.impl.ui.ShelfArchiveViewModel
import com.coffeepeek.feature.shelf.impl.ui.ShelfBagHandoff
import com.coffeepeek.feature.shelf.impl.ui.bag
import com.coffeepeek.feature.shelf.impl.ui.testCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IosShelfArchiveSessionTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test fun nativeNavigationHandsTheArchivedBagToItsExistingScreen() = runTest(dispatcher) {
        val archived = bag("archived", finished = true)
        val repository = FakeShelfRepository().apply {
            archivePages = { page, _ -> Result.success(ShelfArchivePage(listOf(archived), 1, page, 20)) }
        }
        val handoff = ShelfBagHandoff()
        val session = IosShelfArchiveSession(ShelfArchiveViewModel(repository, testCalendar(), handoff))
        val opened = mutableListOf<String>()
        val observation = session.observeEvents({}, { opened += it })
        try {
            runCurrent()
            val row = session.current().sections.single().items.single()
            session.openBag(row.id)
            runCurrent()
            assertEquals(listOf("archived"), opened)
            assertEquals(archived, handoff.opened("archived"))
        } finally {
            session.close()
        }
        assertTrue(observation.isCancelled)
    }
}
