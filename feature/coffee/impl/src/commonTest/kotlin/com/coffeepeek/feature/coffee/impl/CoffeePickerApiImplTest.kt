package com.coffeepeek.feature.coffee.impl

import com.coffeepeek.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CoffeePickerApiImplTest {
    @Test fun pickerUsesTrimmedQueryFirstPageAndNoFilters() = runTest {
        val repository = TestCoffeeRepository().apply { search = { Result.success(page("one", "two")) } }
        val picker = createCoffeePicker(repository)
        val result = picker.search(" Roast ").getOrThrow()
        assertEquals(listOf(SearchCall("Roast", CoffeeFilters(), 1)), repository.searches)
        assertEquals(listOf(CoffeePickerItem("one", "Coffee one", "Roast"), CoffeePickerItem("two", "Coffee two", "Roast")), result)
    }

    @Test fun requestFailureIsPreservedWithoutThrowing() = runTest {
        val failure = IllegalStateException("test failure")
        val repository = TestCoffeeRepository().apply { search = { Result.failure(failure) } }
        assertSame(failure, createCoffeePicker(repository).search("").exceptionOrNull())
    }

    @Test fun thrownCancellationEscapesThePublicContract() = runTest {
        val repository = TestCoffeeRepository().apply { search = { throw CancellationException("cancelled") } }
        assertFailsWith<CancellationException> { createCoffeePicker(repository).search("") }
    }

    @Test fun cancellationInResultStillEscapesThePublicContract() = runTest {
        val repository = TestCoffeeRepository().apply { search = { Result.failure(CancellationException("cancelled")) } }
        assertFailsWith<CancellationException> { createCoffeePicker(repository).search("") }
    }

    @Test fun cancelledCallerCannotReceiveLatePickerRows() = runTest {
        val gate = CompletableDeferred<Unit>()
        var delivered = false
        val repository = TestCoffeeRepository().apply { search = {
            withContext(NonCancellable) { gate.await() }; Result.success(page())
        } }
        val job = launch { createCoffeePicker(repository).search(""); delivered = true }
        runCurrent(); job.cancel(); gate.complete(Unit); job.join()
        assertFalse(delivered)
    }
}
