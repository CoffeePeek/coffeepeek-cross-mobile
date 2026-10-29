package com.coffeepeek.feature.appupdate.impl

import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.feature.appupdate.domain.AppUpdateRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DisabledUpdateControllerTest {
    @Test fun disabledBuildNeverFetchesReadsDismissalOrShowsAPrompt() = runTest {
        val repository = Repository()
        val controller = AppUpdateController(repository, FakeEnvironment(1, checksEnabled = false), backgroundScope)
        controller.check()
        controller.check(manual = true)
        controller.dismiss()
        runCurrent()
        assertEquals(0, repository.fetches)
        assertEquals(0, repository.storageReads)
        assertFalse(controller.state.value.checking)
        assertFalse(controller.state.value.showPrompt)
        assertFalse(controller.state.value.checkFailed)
        assertNull(controller.state.value.update)
        assertNull(controller.state.value.message)
    }

    @Test fun disablingDuringARequestRejectsEvenARequiredPolicy() = runTest {
        var enabled = true
        val environment = object : AppUpdateEnvironment by FakeEnvironment(1) {
            override val checksEnabled get() = enabled
        }
        val repository = Repository().apply { gate = CompletableDeferred() }
        val controller = AppUpdateController(repository, environment, backgroundScope)
        controller.check()
        runCurrent()
        assertEquals(1, repository.fetches)
        enabled = false
        repository.gate!!.complete(Unit)
        runCurrent()
        assertFalse(controller.state.value.showPrompt)
        assertNull(controller.state.value.update)
        assertEquals(0, repository.storageReads)
    }

    private class Repository : AppUpdateRepository {
        var fetches = 0
        var storageReads = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun fetch(): AppUpdate {
            fetches++
            gate?.await()
            return AppUpdate(9999, 9999, "https://apps.apple.com/app/fixture")
        }
        override suspend fun dismissedVersion(): Long? {
            storageReads++
            return null
        }
        override suspend fun dismiss(version: Long) = error("Disabled updates must not persist a dismissal")
    }
}
