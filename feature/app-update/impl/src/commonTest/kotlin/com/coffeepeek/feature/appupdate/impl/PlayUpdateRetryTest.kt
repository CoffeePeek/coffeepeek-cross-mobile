package com.coffeepeek.feature.appupdate.impl

import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.feature.appupdate.domain.AppUpdateRepository
import com.coffeepeek.feature.appupdate.domain.InstallationStage
import com.coffeepeek.feature.appupdate.domain.InstallationState
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayUpdateRetryTest {
    @Test fun waitsForGooglePlayAndReturnsItsFailure() = runTest {
        val repository = Updates()
        val controller = AppUpdateController(repository, FakeEnvironment(100), backgroundScope)
        val installer = Installer()
        val request = async { retryPlayUpdate(controller, installer) }
        runCurrent()
        assertFalse(request.isCompleted)
        assertEquals(1, installer.downloads)
        assertTrue(controller.state.value.manual)
        installer.state.value = installer.state.value.copy(stage = InstallationStage.Available, error = "Google Play недоступен")
        val result = request.await()!!
        assertEquals("Google Play недоступен", result.message)
        assertTrue(result.canRetry)
    }

    @Test fun backendFailureDoesNotStartGooglePlayAndCanBeRetried() = runTest {
        val controller = AppUpdateController(Updates(fail = true), FakeEnvironment(100), backgroundScope)
        val installer = Installer()
        val result = retryPlayUpdate(controller, installer)!!
        assertTrue(result.canRetry)
        assertTrue(controller.state.value.checkFailed)
        assertEquals(0, installer.downloads)
    }

    @Test fun currentVersionIsAnInformationalResultWithoutRetry() = runTest {
        val controller = AppUpdateController(Updates(version = 100), FakeEnvironment(100), backgroundScope)
        val installer = Installer()
        val result = retryPlayUpdate(controller, installer)!!
        assertEquals("Установлена актуальная версия", result.message)
        assertFalse(result.canRetry)
        assertEquals(0, installer.downloads)
    }

    @Test fun acceptedDownloadEndsRetryProgress() = runTest {
        val controller = AppUpdateController(Updates(), FakeEnvironment(100), backgroundScope)
        val installer = Installer()
        val request = async { retryPlayUpdate(controller, installer) }
        runCurrent()
        installer.state.value = installer.state.value.copy(stage = InstallationStage.Downloading)
        assertNull(request.await())
    }

    @Test fun unresponsiveGooglePlayDoesNotLeaveProgressRunningForever() = runTest {
        val controller = AppUpdateController(Updates(), FakeEnvironment(100), backgroundScope)
        val request = async { retryPlayUpdate(controller, Installer()) }
        runCurrent()
        advanceTimeBy(30_001)
        assertTrue(request.await()!!.canRetry)
    }

    private class Updates(val version: Long = 140, val fail: Boolean = false) : AppUpdateRepository {
        override suspend fun fetch(): AppUpdate {
            if (fail) error("Backend unavailable")
            return AppUpdate(version, 0, "https://play.google.com/store/apps/details?id=com.coffeepeek")
        }
        override suspend fun dismissedVersion(): Long? = null
        override suspend fun dismiss(version: Long) = Unit
    }

    private class Installer : UpdateInstaller {
        override val state = MutableStateFlow(InstallationState(error = "Previous failure"))
        var downloads = 0
        override fun download(update: AppUpdate) {
            downloads++
            state.value = InstallationState(update, InstallationStage.Starting)
        }
        override fun install() = Unit
        override fun cancel() = Unit
        override fun refresh() = Unit
    }
}
