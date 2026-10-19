package com.coffeepeek.feature.homecheckin.impl

import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.compose.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Public entry with fake repository/catalogue/media/launcher/telemetry. No production application. */
class HomeCheckInEditorEntryTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun render(content: @Composable () -> Unit) =
        ActivityScenario.launch(HomeCheckInTestActivity::class.java).also { scenario ->
            scenario.onActivity { it.setContent { CoffeePeekTheme(FontFamily.Default) { Surface { content() } } } }
        }
    private fun awaitText(text: String) = compose.waitUntil(5_000) { compose.onNodeWithText(text).isDisplayed() }
    private fun addPhoto() {
        compose.onNodeWithContentDescription("Добавить фото").performScrollTo().performClick()
        awaitText("Выбрать из галереи")
    }
    private fun entry(repo: Repository = Repository(), picker: Picker = Picker(), catalog: Catalog = Catalog(),
        access: () -> Boolean = { true }, metrics: (HomeCheckInEditorMetric) -> Unit = {}) =
        createHomeCheckInEditorEntry(repo, catalog, access,
            HomeCheckInEditorDates({ "2026-10-17T07:30:00Z" }, { "2026-10-17" }, { "17 октября 2026" },
                { it + "T00:00:00Z" }, { it }),
            previewHomeEditorMedia(), picker, { HomeCheckInEditorAnalytics(metrics) })

    @Test fun closeAndReopenUsesFreshFormStoreUnderTheSameParentOwner() {
        val catalog = Catalog()
        val metrics = mutableListOf<HomeCheckInEditorMetric>()
        val entry = entry(catalog = catalog, metrics = metrics::add)
        var visible by mutableStateOf(true)
        var firstOwner: Any? = null
        render {
            val parent = LocalViewModelStoreOwner.current
            if (firstOwner == null) firstOwner = parent else check(firstOwner === parent)
            if (visible) entry.Content(null, 1, true, { visible = false }, { _, _ -> error("Unexpected save") }, {})
        }.use {
            awaitText("Сохранить чекин")
            compose.onNodeWithContentDescription("Заметка").performScrollTo().performTextInput("Private draft")
            compose.onNodeWithContentDescription("Закрыть форму").performClick()
            compose.onNodeWithText("Кофе дома").assertDoesNotExist()
            compose.runOnIdle { visible = true }
            awaitText("Сохранить чекин")
            compose.onNodeWithContentDescription("Заметка").performScrollTo()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            assertEquals(2, catalog.methodCalls)
            assertEquals(2, metrics.count { it == HomeCheckInEditorMetric.Opened })
            assertEquals(1, metrics.count { it == HomeCheckInEditorMetric.Cancelled })
        }
    }

    @Test fun exactEditIdAndHistoricalMethodArePreservedUntilUserChoosesAnotherMethod() {
        val repo = Repository().apply { detail = previewHomePreparation().copy(brewMethodCode = "pour_over") }
        val entry = entry(repo)
        val saved = mutableListOf<String>()
        render { entry.Content("fixture-home", 1, true, {}, { id, _ -> saved += id }, {}) }.use {
            awaitText("Редактировать чекин")
            compose.onNodeWithContentDescription("Выбрать способ заваривания").performScrollTo()
                .assert(hasText("Hario V60"))
            compose.onNodeWithText("Сохранить чекин").performClick()
            compose.waitUntil { saved.size == 1 }
            assertEquals(listOf("fixture-home"), repo.ids)
            assertEquals("fixture-home", repo.updates.single().first)
            assertEquals("pour_over", repo.updates.single().second.brewMethodCode)
            assertTrue(repo.creates.isEmpty())
        }
    }

    @Test fun initiallyDeniedEntryDoesNotInitializeCataloguePickerAnalyticsOrWriter() {
        val repo = Repository(); val picker = Picker(); val catalog = Catalog()
        val metrics = mutableListOf<HomeCheckInEditorMetric>()
        val entry = entry(repo, picker, catalog, metrics = metrics::add)
        render { entry.Content(null, 1, false, {}, { _, _ -> error("Denied save") }, {}) }.use {
            compose.waitForIdle()
            assertEquals(0, catalog.methodCalls); assertEquals(0, picker.compositions)
            assertTrue(repo.creates.isEmpty()); assertTrue(metrics.isEmpty())
            compose.onNodeWithText("Кофе дома").assertDoesNotExist()
        }
    }

    @Test fun galleryResultIsBoundedDuplicateCallbacksAreIgnoredAndSaveHappensOnce() {
        val repo = Repository(); val picker = Picker()
        val entry = entry(repo, picker)
        val saved = mutableListOf<String>()
        render { entry.Content(null, 1, true, {}, { id, _ -> saved += id }, {}) }.use {
            awaitText("Сохранить чекин"); addPhoto()
            compose.onNodeWithText("Выбрать из галереи").performClick()
            assertEquals(listOf(5), picker.limits)
            val first = picker.callbacks.single()
            compose.runOnIdle { first(listOf(upload("one"), upload("two"))) }
            compose.waitUntil { compose.onNodeWithText("Добавлено: 2/5").isDisplayed() }
            addPhoto(); compose.onNodeWithText("Выбрать из галереи").performClick()
            assertEquals(listOf(5, 3), picker.limits)
            compose.runOnIdle {
                first(listOf(upload("obsolete")))
                picker.callbacks.last()((1..5).map { upload("new" + it) })
            }
            compose.waitUntil { compose.onNodeWithText("Добавлено: 5/5").isDisplayed() }
            compose.onNodeWithContentDescription("Добавить фото").assertDoesNotExist()
            compose.onNodeWithText("Сохранить чекин").performClick()
            compose.waitUntil { saved.size == 1 }
            assertEquals(listOf("one", "two", "new1", "new2", "new3"), repo.creates.single().photos.map { it.fileName })
        }
    }

    @Test fun callbackFromDisposedFormCannotAddPhotosToReopenedForm() {
        val picker = Picker()
        val entry = entry(picker = picker)
        var visible by mutableStateOf(true)
        render { if (visible) entry.Content(null, 1, true, { visible = false }, { _, _ -> }, {}) }.use {
            awaitText("Сохранить чекин"); addPhoto()
            compose.onNodeWithText("Выбрать из галереи").performClick()
            val late = picker.callbacks.single()
            compose.onNodeWithContentDescription("Закрыть форму").performClick()
            compose.runOnIdle { visible = true }
            awaitText("Сохранить чекин")
            compose.runOnIdle { late(listOf(upload("private-old"))) }
            compose.onNodeWithContentDescription("Добавить фото").performScrollTo()
            compose.onNodeWithText("Добавлено: 0/5").assertIsDisplayed()
        }
    }

    @Test fun cancelSourceDoesNotLaunchCameraAndCameraRequestDeliversOneOriginal() {
        val picker = Picker(); val repo = Repository()
        val entry = entry(repo, picker)
        render { entry.Content(null, 1, true, {}, { _, _ -> }, {}) }.use {
            awaitText("Сохранить чекин"); addPhoto()
            compose.onNodeWithText("Отмена").performClick()
            assertEquals(0, picker.cameras); assertTrue(picker.callbacks.isEmpty())
            addPhoto(); compose.onNodeWithText("Сделать фото").performClick()
            assertEquals(1, picker.cameras)
            compose.runOnIdle { picker.callbacks.single()(listOf(upload("camera"), upload("extra"))) }
            compose.waitUntil { compose.onNodeWithText("Добавлено: 1/5").isDisplayed() }
            assertTrue(repo.creates.isEmpty())
        }
    }

    @Test fun accountGenerationReplacementClearsOldOwnerAndRejectsItsLateWriteAndMetrics() {
        var generation by mutableIntStateOf(1)
        val repo = Repository().apply { gate = CompletableDeferred() }
        val metrics = mutableListOf<HomeCheckInEditorMetric>()
        val old = entry(repo, access = { generation == 1 }, metrics = metrics::add)
        val fresh = entry(access = { generation == 2 })
        val saved = mutableListOf<String>()
        render {
            (if (generation == 1) old else fresh).Content(null, generation, true, {}, { id, _ -> saved += id }, {})
        }.use {
            awaitText("Сохранить чекин")
            compose.onNodeWithText("Сохранить чекин").performClick()
            compose.waitUntil { repo.creates.size == 1 }
            compose.runOnIdle { generation = 2; repo.gate!!.complete(Unit) }
            awaitText("Сохранить чекин")
            compose.runOnIdle {
                assertTrue(saved.isEmpty())
                assertFalse(metrics.any { it is HomeCheckInEditorMetric.Saved || it == HomeCheckInEditorMetric.Cancelled })
            }
        }
    }

    private class Catalog : HomeCheckInEditorCatalog {
        var methodCalls = 0
        override suspend fun brewMethods(): Result<List<HomeCheckInBrewMethod>> {
            methodCalls++; return Result.success(listOf(HomeCheckInBrewMethod("hario-v60", "Hario V60", setOf("pour_over"))))
        }
        override suspend fun searchBeans(query: String) = Result.success(emptyList<HomeCheckInBeanOption>())
    }
    private class Picker : HomeCheckInPhotoPicker {
        var compositions = 0
        var cameras = 0
        val limits = mutableListOf<Int>()
        val callbacks = mutableListOf<(List<HomePreparationUpload>) -> Unit>()
        @Composable override fun rememberController(): HomeCheckInPhotoPickerController {
            compositions++
            return HomeCheckInPhotoPickerController(false, { limit, deliver -> limits += limit; callbacks += deliver },
                { deliver -> cameras++; callbacks += deliver })
        }
    }
    private class Repository : HomePreparationRepository {
        var detail = previewHomePreparation()
        val ids = mutableListOf<String>()
        val creates = mutableListOf<HomePreparationInput>()
        val updates = mutableListOf<Pair<String, HomePreparationInput>>()
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun detail(id: String): Result<HomePreparation> { ids += id; return Result.success(detail) }
        override suspend fun create(input: HomePreparationInput): Result<HomePreparation> {
            creates += input
            withContext(NonCancellable) { gate?.await() }
            return Result.success(detail)
        }
        override suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation> {
            updates += id to input; return Result.success(detail)
        }
        override suspend fun delete(id: String): Result<Unit> = error("Unexpected delete")
        override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> = error("Unexpected history")
        override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> = error("Unexpected calendar")
    }
    companion object {
        private fun upload(name: String) = HomePreparationUpload(name, "image/png", name.encodeToByteArray())
    }
}
