package com.coffeepeek.admin.feature.appupdate

import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.feature.appupdate.domain.AppUpdateRepository
import com.coffeepeek.feature.appupdate.domain.InstallationStage
import com.coffeepeek.feature.appupdate.domain.InstallationState
import com.coffeepeek.admin.feature.appupdate.ui.AppUpdateState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the check state exposes to the UI, characterized before the logic moves to a ViewModel. */
class AppUpdateCheckBehaviourTest {
    private val url = "https://example.com/app.apk"
    private val failure = "Не удалось проверить обновления. Попробуйте позже."
    private val upToDate = "Установлена актуальная версия"

    private class Repo(var update: AppUpdate? = null, var dismissedStored: Long? = null) : AppUpdateRepository {
        var fetches = 0
        var dismissReads = 0
        var dismissWrites = 0
        var gate: CompletableDeferred<Unit>? = null
        var fetchError: Throwable? = null
        var dismissError: Throwable? = null
        override suspend fun fetch(): AppUpdate? { fetches++; gate?.await(); fetchError?.let { throw it }; return update }
        override suspend fun dismissedVersion(): Long? { dismissReads++; return dismissedStored }
        override suspend fun dismiss(version: Long) { dismissWrites++; dismissError?.let { throw it }; dismissedStored = version }
    }

    private suspend fun AppUpdateState.run(manual: Boolean) = apply { check(manual) }.state.first { !it.checking }

    private fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Test fun aManualCheckAnswersInWordsAndAnAutomaticOneStaysSilent() = runBlocking {
        val scope = scope()
        try {
            val repo = Repo(AppUpdate(100, 0, url))
            val current = AppUpdateState(repo, scope, currentVersion = 100)
            current.run(manual = true).let { assertEquals(upToDate, it.message); assertFalse(it.checkFailed); assertFalse(it.showPrompt); assertNull(it.update) }
            current.run(manual = false).let { assertNull(it.message); assertFalse(it.checkFailed) }

            repo.update = null
            current.run(manual = true).let { assertEquals(failure, it.message); assertTrue(it.checkFailed); assertTrue(it.manual) }
            current.run(manual = false).let { assertNull(it.message); assertFalse(it.checkFailed); assertFalse(it.manual) }

            repo.update = AppUpdate(140, 0, url)
            current.run(manual = true).let { assertNull(it.message); assertTrue(it.showPrompt); assertEquals(repo.update, it.update) }
        } finally { scope.cancel() }
    }

    @Test fun anErrorOrATimeoutIsAFailedCheckNotACrash() = runBlocking {
        val scope = scope()
        try {
            val repo = Repo(AppUpdate(140, 0, url))
            val state = AppUpdateState(repo, scope, currentVersion = 100)
            for (error in listOf<Throwable>(IllegalStateException("offline"), runCatching { withTimeout(1) { awaitCancellation() } }.exceptionOrNull()!!)) {
                repo.fetchError = error
                state.run(manual = true).let { assertEquals(failure, it.message); assertTrue(it.checkFailed); assertFalse(it.showPrompt) }
                state.run(manual = false).let { assertNull(it.message); assertFalse(it.checkFailed) }
            }
        } finally { scope.cancel() }
    }

    @Test fun anUnconfiguredVersionNeverFetchesAndAManualCheckReportsAFailure() = runBlocking {
        val scope = scope()
        try {
            val repo = Repo(AppUpdate(140, 0, url))
            val state = AppUpdateState(repo, scope, currentVersion = null)
            state.run(manual = false).let { assertFalse(it.showPrompt); assertNull(it.update) }
            state.run(manual = true).let { assertEquals(failure, it.message); assertTrue(it.checkFailed) }
            assertEquals(0, repo.fetches)
        } finally { scope.cancel() }
    }

    @Test fun aSecondCheckWhileOneRunsIsIgnored() = runBlocking {
        val scope = scope()
        try {
            val repo = Repo(AppUpdate(140, 0, url)).apply { gate = CompletableDeferred() }
            val state = AppUpdateState(repo, scope, currentVersion = 100)
            state.check()
            assertTrue(state.state.value.checking)
            state.check(manual = true)
            repo.gate!!.complete(Unit)
            val result = state.state.first { !it.checking }
            assertEquals(1, repo.fetches)
            assertFalse(result.manual)
        } finally { scope.cancel() }
    }

    @Test fun theDismissedVersionIsReadOnceAndAFailedWriteStillHidesThePrompt() = runBlocking {
        val scope = scope()
        try {
            val repo = Repo(AppUpdate(140, 0, url), dismissedStored = 130).apply { dismissError = IllegalStateException("disk full") }
            val state = AppUpdateState(repo, scope, currentVersion = 100)
            assertTrue(state.run(manual = false).showPrompt)          // a stored dismissal of an older version does not hide 140
            state.dismiss()
            assertFalse(state.state.value.showPrompt)
            state.run(manual = false).let { assertFalse(it.showPrompt) }
            assertEquals(1, repo.dismissReads)
            assertEquals(1, repo.dismissWrites)
        } finally { scope.cancel() }
    }

    @Test fun aRequiredUpdateCannotBeDismissedAndKeepsPrompting() = runBlocking {
        val scope = scope()
        try {
            val repo = Repo(AppUpdate(140, 120, url))
            val state = AppUpdateState(repo, scope, currentVersion = 100)
            assertTrue(state.run(manual = false).showPrompt)
            state.dismiss()
            assertTrue(state.state.value.showPrompt)
            assertEquals(0, repo.dismissWrites)
            assertTrue(state.run(manual = false).showPrompt)
        } finally { scope.cancel() }
    }

    @Test fun theInstallerStagesThatCountAsBusy() {
        val busy = InstallationStage.entries.filter { InstallationState(stage = it).busy }
        assertEquals(
            listOf(InstallationStage.Starting, InstallationStage.Downloading, InstallationStage.Verifying, InstallationStage.Installing),
            busy,
        )
        assertFalse(InstallationState(error = "x").busy)
        assertFalse(InstallationState(stage = InstallationStage.Ready).busy)
    }
}
