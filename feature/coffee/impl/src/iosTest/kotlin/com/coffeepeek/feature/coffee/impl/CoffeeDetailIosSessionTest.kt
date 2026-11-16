package com.coffeepeek.feature.coffee.impl

import com.coffeepeek.feature.coffee.api.CoffeeDetailAnalytics
import kotlinx.coroutines.CompletableDeferred
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
class CoffeeDetailIosSessionTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test fun closingCancelsDetailAndObservers() = runTest(dispatcher) {
        val request = CompletableDeferred<Unit>()
        val repository = TestCoffeeRepository().apply {
            detail = { request.await(); Result.success(detail()) }
        }
        val session = createIosCoffeeDetailSession("coffee", repository, CoffeeDetailAnalytics.None)
        val deliveries = mutableListOf<String?>()
        val observation = session.observeState { deliveries.add(it.details?.coffee?.slug) }

        runCurrent()
        assertEquals(listOf(null), deliveries)
        session.close()
        request.complete(Unit)
        runCurrent()

        assertTrue(observation.isCancelled)
        assertEquals(listOf(null), deliveries)
    }

    @Test fun shareUsesLoadedCanonicalPath() = runTest(dispatcher) {
        val session = createIosCoffeeDetailSession("alias", TestCoffeeRepository(), CoffeeDetailAnalytics.None)
        val shares = mutableListOf<Pair<String, String>>()
        session.observeEvents({}, {}, {}, {}, { name, path -> shares.add(name to path) })
        try {
            runCurrent()
            session.share()
            runCurrent()
            assertEquals(listOf(detail().coffee.name to detail().canonicalPath), shares)
        } finally {
            session.close()
        }
    }
}
