package com.coffeepeek.feature.homecheckin.impl

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.compose.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.CompletableDeferred

/** All state/ports are fixtures. No live writes or production application startup. */
class HomeCheckInDetailContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(dark: Boolean = false, content: @Composable () -> Unit): ActivityScenario<HomeCheckInTestActivity> =
        ActivityScenario.launch(HomeCheckInTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = dark) { Surface { content() } }
            } }
        }

    private fun awaitText(text: String) {
        // Dialogs use another window: wait for layout, not only the parent composition.
        compose.waitUntil(timeoutMillis = 5_000) { compose.onNodeWithText(text).isDisplayed() }
    }

    @Composable private fun Content(
        state: HomeCheckInDetailState = HomeCheckInDetailState(entry = previewHomePreparation()),
        actions: (HomeCheckInDetailAction) -> Unit = {},
        image: @Composable (String, String, Modifier, Boolean) -> Unit = { _, _, modifier, _ -> Box(modifier) },
    ) = HomeCheckInDetailScreenContent(state, actions, { _, _ -> "17.10.2026 · 10:30" }, { "Hario V60" }, image)

    @Test fun recipeRatingAndVisibilityPreserveCurrentLabelsInBothThemes() {
        for (dark in listOf(false, true)) render(dark) { Content() }.use {
            compose.onNodeWithText("Эфиопия").assertIsDisplayed()
            compose.onNodeWithText("Только мне").assertIsDisplayed()
            compose.onNodeWithText("Hario V60").assertIsDisplayed()
            compose.onNodeWithText("18 г").assertIsDisplayed()
            compose.onNodeWithText("3 мин").assertIsDisplayed()
            compose.onNodeWithContentDescription("Оценка вкуса 4 из 5").assertExists()
        }
    }

    @Test fun publicDoesNotImplyModerationApprovalAndUnknownVisibilityIsNotInvented() {
        val labels = mapOf(HomePreparationModeration.Approved to "Виден всем", HomePreparationModeration.Pending to "На модерации",
            HomePreparationModeration.Rejected to "Не опубликован", HomePreparationModeration.NotSubmitted to "Ожидает публикации",
            HomePreparationModeration.Unknown to "Публичный")
        for ((moderation, label) in labels) render { Content(HomeCheckInDetailState(entry = previewHomePreparation().copy(
            visibility = HomePreparationVisibility.Public, moderation = moderation))) }.use {
            compose.onNodeWithText(label).assertIsDisplayed()
            compose.onNodeWithText("Только мне").assertDoesNotExist()
        }
        render { Content(HomeCheckInDetailState(entry = previewHomePreparation().copy(visibility = null))) }.use {
            (labels.values + "Только мне").forEach { label -> compose.onNodeWithText(label).assertDoesNotExist() }
        }
    }

    @Test fun originalsAndCutoutUseInjectedPortsAndSeparateActions() {
        val actions = mutableListOf<HomeCheckInDetailAction>()
        val requested = java.util.concurrent.CopyOnWriteArraySet<Pair<String, Boolean>>()
        val entry = previewHomePreparation().copy(photos = (1..5).map { HomePreparationPhoto("$it", "original-$it") },
            drinkPhotoUrl = "transparent-cutout")
        render { Content(HomeCheckInDetailState(entry = entry), actions::add) { url, _, modifier, cutout ->
            requested += url to cutout; Box(modifier)
        } }.use {
            compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Фотография 3 из 5"))
            compose.onNodeWithContentDescription("Фотография 3 из 5").performClick()
            compose.onNodeWithText("+2").assertExists()
            compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Фото напитка"))
            compose.onNodeWithContentDescription("Фото напитка").performClick()
            assertEquals(listOf(HomeCheckInDetailAction.OpenPhoto(2), HomeCheckInDetailAction.OpenDrinkPhoto), actions)
            assertTrue(requested.containsAll(setOf("original-1" to false, "original-2" to false, "original-3" to false,
                "transparent-cutout" to true)))
            assertFalse(requested.any { it.first == "original-4" || it.first == "original-5" })
        }
    }

    @Test fun revokedOrClosedStateNeverComposesPrivateContentEvenIfEntryRemainsInSnapshot() {
        for (state in listOf(HomeCheckInDetailState(entry = previewHomePreparation(), isAccessRevoked = true),
                HomeCheckInDetailState(entry = previewHomePreparation(), isClosed = true))) {
            var images = 0
            render { Content(state, image = { _, _, _, _ -> images++ }) }.use {
                compose.onNodeWithText("Эфиопия").assertDoesNotExist()
                compose.onNodeWithText("Ягодная кислотность и сладкое послевкусие").assertDoesNotExist()
                assertEquals(0, images)
            }
        }
    }

    @Test fun deleteRequiresSeparateConfirmationAndCanBeCancelled() {
        val actions = mutableListOf<HomeCheckInDetailAction>()
        var state by mutableStateOf(HomeCheckInDetailState(entry = previewHomePreparation()))
        render { Content(state, actions::add) }.use {
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Удалить запись"))
            compose.onNodeWithText("Удалить запись").performClick()
            assertEquals(listOf(HomeCheckInDetailAction.RequestDelete), actions)
            compose.runOnIdle { state = state.copy(showDeleteConfirmation = true) }
            awaitText("Она исчезнет из истории чекинов.")
            compose.onNodeWithText("Она исчезнет из истории чекинов.").assertIsDisplayed()
            compose.onNodeWithText("Отмена").performClick()
            assertEquals(HomeCheckInDetailAction.CancelDelete, actions.last())
        }
    }

    @Test fun deletingDisablesConflictingActionsAndRetryNeverEmitsDelete() {
        val actions = mutableListOf<HomeCheckInDetailAction>()
        var state by mutableStateOf(HomeCheckInDetailState(entry = previewHomePreparation(), isDeleting = true))
        render { Content(state, actions::add) }.use {
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Редактировать"))
            compose.onNodeWithText("Редактировать").assertIsNotEnabled()
            compose.onNodeWithText("Удалить запись").assertIsNotEnabled()
            compose.runOnIdle { state = state.copy(isDeleting = false, deleteIssue = HomePreparationIssue.RequestFailed) }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Повторить"))
            compose.onNodeWithText("Повторить").performClick()
            assertEquals(listOf(HomeCheckInDetailAction.Reload), actions)
        }
    }

    @Test fun narrowWindowLargeFontKeepsActionsReachableWithoutClipping() {
        render { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.6f)) {
            Box(Modifier.width(320.dp).height(520.dp)) { Content() }
        } }.use {
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Редактировать"))
            compose.onNodeWithText("Редактировать").assertIsDisplayed()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Удалить запись"))
            compose.onNodeWithText("Удалить запись").assertIsDisplayed()
        }
    }

    @Test fun runtimeUsesLatestCallbacksAndNewGenerationDoesNotReuseOldAccountModel() {
        val repository = Repository()
        val entry = createHomeCheckInDetailEntry(repository, { true }) { _, _, modifier, _ -> Box(modifier) }
        var generation by mutableIntStateOf(1)
        var callback by mutableStateOf<(String) -> Unit>({ error("Old callback must not be invoked") })
        val edits = mutableListOf<String>()
        render { entry.Content("fixture-home", generation, true, { date, _ -> date }, { it }, {}, callback,
            { error("Unexpected delete") }, { _, _ -> error("Unexpected photo") }, {}) }.use {
            awaitText("Эфиопия")
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Редактировать"))
            compose.runOnIdle { callback = { edits += it } }
            compose.onNodeWithText("Редактировать").performClick()
            compose.waitUntil { edits.size == 1 }
            compose.runOnIdle { generation = 2 }
            compose.waitUntil { repository.detailCalls == 2 }
            assertEquals(listOf("fixture-home"), edits)
            assertEquals(0, repository.deleteCalls)
        }
    }

    @Test fun runtimeRevocationHidesContentBeforeInvokingPrivateCallbacks() {
        val repository = Repository()
        var allowed by mutableStateOf(true)
        val entry = createHomeCheckInDetailEntry(repository, { allowed }) { _, _, modifier, _ -> Box(modifier) }
        render { entry.Content("fixture-home", 1, allowed, { date, _ -> date }, { it }, {},
            { error("Private edit after revocation") }, { error("Unexpected delete") }, { _, _ -> error("Unexpected photo") }, {}) }.use {
            awaitText("Эфиопия")
            compose.onNodeWithText("Эфиопия").assertExists()
            compose.runOnIdle { allowed = false }
            compose.onNodeWithText("Эфиопия").assertDoesNotExist()
            assertEquals(0, repository.deleteCalls)
        }
    }

    @Test fun runtimeDeletesOnceOnlyAfterConfirmationAndBlocksOuterDismissalUntilOutcome() {
        val repository = Repository().apply { deleteGate = CompletableDeferred() }
        val entry = createHomeCheckInDetailEntry(repository, { true }) { _, _, modifier, _ -> Box(modifier) }
        val deleted = mutableListOf<String>()
        var dismissAllowed = true
        render { entry.Content("fixture-home", 1, true, { date, _ -> date }, { it }, {}, {}, deleted::add,
            { _, _ -> }, { dismissAllowed = it }) }.use {
            awaitText("Эфиопия")
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Удалить запись"))
            compose.onNodeWithText("Удалить запись").performClick()
            awaitText("Удалить домашнюю запись?")
            compose.onNodeWithText("Удалить домашнюю запись?").assertIsDisplayed()
            assertEquals(0, repository.deleteCalls)
            compose.onNode(hasText("Удалить") and hasAnyAncestor(isDialog())).performClick()
            compose.waitUntil { repository.deleteCalls == 1 && !dismissAllowed }
            assertTrue(deleted.isEmpty())
            compose.runOnIdle { repository.deleteGate!!.complete(Unit) }
            compose.waitUntil { deleted.size == 1 && dismissAllowed }
            assertEquals(listOf("fixture-home"), deleted)
            compose.onNodeWithText("Эфиопия").assertDoesNotExist()
        }
    }

    @Test fun initiallyDeniedSnapshotDoesNotStartEvenTheFirstDetailRequest() {
        val repository = Repository()
        val entry = createHomeCheckInDetailEntry(repository, { true }) { _, _, _, _ -> error("Denied image") }
        render { entry.Content("fixture-home", 1, false, { date, _ -> date }, { it }, {}, {}, {}, { _, _ -> }, {}) }.use {
            compose.waitForIdle()
            assertEquals(0, repository.detailCalls)
            compose.onNodeWithText("Эфиопия").assertDoesNotExist()
        }
    }

    @Test fun runtimePhotoViewerReceivesIssuedAlbumAndCutoutWithoutAnotherDetailRead() {
        val originals = (1..5).map { "https://fixture.invalid/issued/original-$it?signature=fixture" }
        val cutout = "https://fixture.invalid/issued/cutout?signature=fixture"
        val repository = Repository().apply { detail = previewHomePreparation().copy(
            photos = originals.mapIndexed { index, url -> HomePreparationPhoto("photo-$index", url) },
            drinkPhotoUrl = cutout,
        ) }
        val requests = java.util.concurrent.CopyOnWriteArraySet<Pair<String, Boolean>>()
        val entry = createHomeCheckInDetailEntry(repository, { true }) { url, _, modifier, isCutout ->
            requests += url to isCutout
            Box(modifier)
        }
        val albums = mutableListOf<Pair<List<String>, Int>>()
        render { entry.Content("fixture-home", 1, true, { date, _ -> date }, { it }, {}, {}, {},
            { urls, index -> albums += urls to index }, {}) }.use {
            awaitText("Эфиопия")
            compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Фотография 3 из 5"))
            compose.onNodeWithContentDescription("Фотография 3 из 5").performClick()
            compose.waitUntil { albums.size == 1 }
            assertEquals(originals to 2, albums.single())
            compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Фото напитка"))
            compose.onNodeWithContentDescription("Фото напитка").performClick()
            compose.waitUntil { albums.size == 2 }
            assertEquals(listOf(cutout) to 0, albums.last())
            assertTrue(requests.containsAll(setOf(originals[0] to false, originals[2] to false, cutout to true)))
            assertEquals(1, repository.detailCalls)
            assertEquals(0, repository.deleteCalls)
        }
    }

    @Test fun runtimeLoadFailureRetriesTheReadWithoutInventingAnEntryOrWriting() {
        val repository = Repository().apply { detailFailure = HomePreparationFailure(HomePreparationIssue.Offline) }
        val entry = createHomeCheckInDetailEntry(repository, { true }) { _, _, modifier, _ -> Box(modifier) }
        render { entry.Content("fixture-home", 1, true, { date, _ -> date }, { it }, {}, {}, {}, { _, _ -> }, {}) }.use {
            awaitText("Нет подключения к интернету. Попробуйте ещё раз.")
            compose.onNodeWithText("Эфиопия").assertDoesNotExist()
            assertEquals(1, repository.detailCalls)
            compose.runOnIdle { repository.detailFailure = null }
            compose.onNodeWithText("Повторить").performClick()
            awaitText("Эфиопия")
            assertEquals(2, repository.detailCalls)
            assertEquals(0, repository.deleteCalls)
            compose.onNodeWithText("Нет подключения к интернету. Попробуйте ещё раз.").assertDoesNotExist()
        }
    }

    @Test fun runtimeFailedDeleteRefreshesOnlyAndRequiresFreshConfirmationBeforeAnotherDelete() {
        val repository = Repository().apply { deleteFailure = HomePreparationFailure(HomePreparationIssue.RequestFailed) }
        val entry = createHomeCheckInDetailEntry(repository, { true }) { _, _, modifier, _ -> Box(modifier) }
        val deleted = mutableListOf<String>()
        render { entry.Content("fixture-home", 1, true, { date, _ -> date }, { it }, {}, {}, deleted::add,
            { _, _ -> }, {}) }.use {
            awaitText("Эфиопия")
            requestDelete()
            confirmDelete()
            compose.waitUntil { repository.deleteCalls == 1 }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Повторить"))
            compose.onNodeWithText("Не удалось подтвердить удаление. Обновите запись перед повторной попыткой.")
                .assertIsDisplayed()
            assertTrue(deleted.isEmpty())
            compose.runOnIdle { repository.deleteFailure = null }
            compose.onNodeWithText("Повторить").performClick()
            compose.waitUntil { repository.detailCalls == 2 }
            awaitText("Эфиопия")
            assertEquals(1, repository.deleteCalls)
            assertTrue(deleted.isEmpty())
            requestDelete()
            compose.onNodeWithText("Отмена").performClick()
            compose.onNodeWithText("Удалить домашнюю запись?").assertDoesNotExist()
            assertEquals(1, repository.deleteCalls)
            requestDelete()
            confirmDelete()
            compose.waitUntil { deleted.size == 1 }
            assertEquals(2, repository.deleteCalls)
            assertEquals(listOf("fixture-home"), deleted)
        }
    }

    @Test fun runtimeStoppedDestinationDeliversDeleteOnlyOnceAfterResumingWithLatestCallback() {
        val repository = Repository().apply { deleteGate = CompletableDeferred() }
        val entry = createHomeCheckInDetailEntry(repository, { true }) { _, _, modifier, _ -> Box(modifier) }
        val deleted = mutableListOf<String>()
        var callback by mutableStateOf<(String) -> Unit>({ error("Old delete callback") })
        render { entry.Content("fixture-home", 1, true, { date, _ -> date }, { it }, {}, {}, callback,
            { _, _ -> }, {}) }.use { scenario ->
            awaitText("Эфиопия")
            requestDelete()
            confirmDelete()
            compose.waitUntil { repository.deleteCalls == 1 }
            compose.runOnIdle { callback = deleted::add }
            compose.waitForIdle()
            scenario.moveToState(Lifecycle.State.CREATED)
            InstrumentationRegistry.getInstrumentation().runOnMainSync { repository.deleteGate!!.complete(Unit) }
            compose.waitUntil { repository.deleteReturned }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertTrue(deleted.isEmpty())
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitUntil { deleted.size == 1 }
            assertEquals(listOf("fixture-home"), deleted)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitForIdle()
            assertEquals(listOf("fixture-home"), deleted)
            assertEquals(1, repository.deleteCalls)
        }
    }

    private fun requestDelete() {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Удалить запись"))
        compose.onNodeWithText("Удалить запись").performClick()
        awaitText("Удалить домашнюю запись?")
    }

    private fun confirmDelete() = compose.onNode(hasText("Удалить") and hasAnyAncestor(isDialog())).performClick()

    @Test fun brandFontFixtureRendersInBothThemes() {
        for (dark in listOf(false, true)) render(dark) {
            CoffeePeekTheme(darkTheme = dark) { Surface { Content() } }
        }.use {
            compose.onNodeWithText("Эфиопия").assertIsDisplayed()
            val file = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "home-detail-${if (dark) "dark" else "light"}.png")
            file.outputStream().use { output ->
                check(compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output))
            }
        }
    }

    private class Repository : HomePreparationRepository {
        @Volatile var detailCalls = 0
        @Volatile var deleteCalls = 0
        @Volatile var deleteReturned = false
        var detail = previewHomePreparation()
        var detailFailure: HomePreparationFailure? = null
        var deleteFailure: HomePreparationFailure? = null
        var deleteGate: CompletableDeferred<Unit>? = null
        override suspend fun detail(id: String): Result<HomePreparation> {
            detailCalls++
            return detailFailure?.let { Result.failure(it) } ?: Result.success(detail)
        }
        override suspend fun delete(id: String): Result<Unit> {
            deleteCalls++
            deleteGate?.await()
            deleteReturned = true
            return deleteFailure?.let { Result.failure(it) } ?: Result.success(Unit)
        }
        override suspend fun create(input: HomePreparationInput): Result<HomePreparation> = error("Unexpected create")
        override suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation> = error("Unexpected update")
        override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> = error("Unexpected history")
        override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> = error("Unexpected calendar")
    }
}
