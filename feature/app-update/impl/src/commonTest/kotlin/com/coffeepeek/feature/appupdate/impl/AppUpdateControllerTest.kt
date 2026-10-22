package com.coffeepeek.feature.appupdate.impl

import com.coffeepeek.admin.config.iosUpdateVersionCode
import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.feature.appupdate.domain.AppUpdateRepository
import com.coffeepeek.feature.appupdate.domain.InstallationStage
import com.coffeepeek.feature.appupdate.domain.InstallationState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdateControllerTest {
    @Test
    fun iosCiBuildChecksForANewerStoreVersionAutomaticallyAndManually() = runBlocking {
        val repository = Updates(AppUpdate(49, 0, "https://apps.apple.com/app/id6821851267"))
        val controller = AppUpdateState(repository, this, currentVersion = iosUpdateVersionCode("48.1"))
        controller.check()
        controller.state.first { !it.checking }
        assertEquals(1, repository.fetches)
        assertTrue(controller.state.value.showPrompt)
        controller.dismiss()
        controller.check(manual = true)
        controller.state.first { !it.checking }
        assertEquals(2, repository.fetches)
        assertTrue(controller.state.value.showPrompt)
        assertFalse(controller.state.value.checkFailed)
    }

    @Test
    fun iosRequiredVersionCannotBeDismissedAndUploadRetriesAreNotUpdates() = runBlocking {
        val repository = Updates(AppUpdate(49, 49, "https://apps.apple.com/app/id6821851267"))
        val current = iosUpdateVersionCode("48.2")
        val controller = AppUpdateState(repository, this, currentVersion = current)
        controller.check()
        controller.state.first { !it.checking }
        assertTrue(controller.state.value.update!!.isRequired(current!!))
        controller.dismiss()
        assertNull(repository.dismissed)
        assertTrue(controller.state.value.showPrompt)
        repository.update = repository.update.copy(versionCode = 48, minVersionCode = 0)
        controller.check(manual = true)
        controller.state.first { !it.checking }
        assertNull(controller.state.value.update)
        assertFalse(controller.state.value.checkFailed)
    }

    @Test
    fun nativeConfirmationSkipsTheCardButRequiredDownloadsAndErrorsStillBlockOldVersions() {
        assertFalse(needsPlayFallback(true, InstallationState()))
        assertFalse(needsPlayFallback(true, InstallationState(stage = InstallationStage.Starting)))
        assertTrue(needsPlayFallback(true, InstallationState(error = "Native confirmation declined")))
        for (stage in listOf(InstallationStage.Downloading, InstallationStage.Ready, InstallationStage.Installing)) {
            assertTrue(needsPlayFallback(true, InstallationState(stage = stage)))
            assertFalse(needsPlayFallback(false, InstallationState(stage = stage)))
        }
        assertFalse(needsPlayFallback(false, InstallationState(error = "Play unavailable")))
    }

    @Test
    fun nativePromptIsConsumedOnceAndAnOptionalDeclineStillAllowsManualRetry() = runBlocking {
        val repository = Updates(AppUpdate(140, 0, "https://play.google.com/store/apps/details?id=com.coffeepeek"))
        val controller = AppUpdateController(repository, FakeEnvironment(100), this)
        controller.check()
        controller.state.first { !it.checking }
        assertEquals(repository.update, controller.takeNativePrompt(InstallationState()))
        assertNull(controller.takeNativePrompt(InstallationState()))
        assertFalse(controller.state.value.showPrompt)

        controller.dismiss()
        controller.check()
        controller.state.first { !it.checking }
        assertFalse(controller.state.value.showPrompt)
        val recreated = AppUpdateController(repository, FakeEnvironment(100), this)
        recreated.check()
        recreated.state.first { !it.checking }
        assertFalse(recreated.state.value.showPrompt)
        controller.check(manual = true)
        controller.state.first { !it.checking }
        assertEquals(repository.update, controller.takeNativePrompt(InstallationState()))
        repository.update = repository.update.copy(versionCode = 141)
        controller.check()
        controller.state.first { !it.checking }
        assertEquals(repository.update, controller.takeNativePrompt(InstallationState()))
    }

    @Test
    fun aFailedNativePromptDoesNotLoopAndCanBeRetriedManually() = runBlocking {
        val repository = Updates(AppUpdate(140, 120, "https://play.google.com/store/apps/details?id=com.coffeepeek"))
        val controller = AppUpdateController(repository, FakeEnvironment(100), this)
        val failed = InstallationState(error = "Google Play unavailable")
        controller.check()
        controller.state.first { !it.checking }
        assertNull(controller.takeNativePrompt(failed))
        assertFalse(controller.state.value.showPrompt)

        controller.dismiss()
        assertNull(repository.dismissed)
        controller.check(manual = true)
        controller.state.first { !it.checking }
        assertEquals(repository.update, controller.takeNativePrompt(failed))
        assertTrue(repository.update.isRequired(100))
    }

    @Test
    fun resumeDoesNotStartASecondFlowDuringDownloadOrInstallation() = runBlocking {
        val repository = Updates(AppUpdate(140, 0, "https://play.google.com/store/apps/details?id=com.coffeepeek"))
        val controller = AppUpdateController(repository, FakeEnvironment(100), this)
        for (stage in InstallationStage.entries.filter { it != InstallationStage.Available }) {
            controller.check(manual = true)
            controller.state.first { !it.checking }
            assertNull(controller.takeNativePrompt(InstallationState(stage = stage)))
            assertFalse(controller.state.value.showPrompt)
        }
    }

    @Test
    fun currentAndUnconfiguredVersionsDoNotLaunchNativeUpdates() = runBlocking {
        val repository = Updates(AppUpdate(100, 0, "https://play.google.com/store/apps/details?id=com.coffeepeek"))
        val controller = AppUpdateController(repository, FakeEnvironment(100), this)
        controller.check(manual = true)
        controller.state.first { !it.checking }
        assertNull(controller.takeNativePrompt(InstallationState()))
        repository.update = AppUpdate(110, 0, repository.update.url)
        val unknownVersion = AppUpdateController(repository, FakeEnvironment(null), this)
        unknownVersion.check()
        unknownVersion.state.first { !it.checking }
        assertNull(unknownVersion.takeNativePrompt(InstallationState()))
    }

    private class Updates(var update: AppUpdate) : AppUpdateRepository {
        var dismissed: Long? = null
        var fetches = 0
        override suspend fun fetch(): AppUpdate {
            fetches++
            return update
        }
        override suspend fun dismissedVersion() = dismissed
        override suspend fun dismiss(version: Long) { dismissed = version }
    }
}
