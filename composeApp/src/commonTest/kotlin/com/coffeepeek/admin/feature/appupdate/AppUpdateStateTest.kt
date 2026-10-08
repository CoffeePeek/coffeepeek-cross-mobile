package com.coffeepeek.admin.feature.appupdate

import com.coffeepeek.admin.feature.appupdate.domain.AppUpdate
import com.coffeepeek.admin.feature.appupdate.domain.AppUpdateRepository
import com.coffeepeek.admin.feature.appupdate.domain.InstallationStage
import com.coffeepeek.admin.feature.appupdate.domain.InstallationState
import com.coffeepeek.admin.feature.appupdate.ui.AppUpdateState
import com.coffeepeek.admin.feature.appupdate.ui.needsPlayFallback
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdateStateTest {
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
        val controller = AppUpdateState(repository, this, currentVersion = 100)
        controller.check()
        controller.state.first { !it.checking }
        assertEquals(repository.update, controller.takeNativePrompt(InstallationState()))
        assertNull(controller.takeNativePrompt(InstallationState()))
        assertFalse(controller.state.value.showPrompt)

        controller.dismiss()
        controller.check()
        controller.state.first { !it.checking }
        assertFalse(controller.state.value.showPrompt)
        val recreated = AppUpdateState(repository, this, currentVersion = 100)
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
        val controller = AppUpdateState(repository, this, currentVersion = 100)
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
        val controller = AppUpdateState(repository, this, currentVersion = 100)
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
        val controller = AppUpdateState(repository, this, currentVersion = 100)
        controller.check(manual = true)
        controller.state.first { !it.checking }
        assertNull(controller.takeNativePrompt(InstallationState()))
        repository.update = AppUpdate(110, 0, repository.update.url)
        val unknownVersion = AppUpdateState(repository, this, currentVersion = null)
        unknownVersion.check()
        unknownVersion.state.first { !it.checking }
        assertNull(unknownVersion.takeNativePrompt(InstallationState()))
    }

    private class Updates(var update: AppUpdate) : AppUpdateRepository {
        var dismissed: Long? = null
        override suspend fun fetch() = update
        override suspend fun dismissedVersion() = dismissed
        override suspend fun dismiss(version: Long) { dismissed = version }
    }
}
