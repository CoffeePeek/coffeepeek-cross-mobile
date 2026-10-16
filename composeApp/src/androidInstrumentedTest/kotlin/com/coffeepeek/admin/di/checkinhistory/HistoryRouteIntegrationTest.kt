package com.coffeepeek.admin.di.checkinhistory

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.BrewMethodOption
import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryDestination
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryScreenRenderer
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.homecheckin.domain.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/** Actual Android destination + existing Home UI/writer, all repositories/auth/catalog/images are fake. */
class HistoryRouteIntegrationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun applicationSelectsTheMigratedAndroidRenderer() {
        val graph = GlobalContext.get()
        assertSame(graph.get<AndroidCheckInHistoryScreenRenderer>(), graph.get<CheckInHistoryScreenRenderer>())
    }

    @Test fun platformDestinationForwardsInitialEditIdWithoutCreatingAnotherScreenContract() {
        val id = mutableStateOf<String?>(null)
        val seen = mutableListOf<String?>()
        val renderer = object : CheckInHistoryScreenRenderer {
            @Composable override fun Content(initialHomeId: String?) {
                SideEffect { seen += initialHomeId }
                Text(initialHomeId ?: "Combined route")
            }
        }
        render { CheckInHistoryDestination(id.value, renderer) }.use {
            compose.onNodeWithText("Combined route").assertExists()
            compose.runOnIdle { id.value = "home-one" }
            compose.onNodeWithText("home-one").assertExists()
            assertEquals(listOf(null, "home-one"), seen.distinct())
        }
    }

    @Test fun defaultDestinationKeepsExistingBackSignInAndShopPickerRoutes() {
        val fixture = Fixture(loggedIn = false)
        render { CheckInHistoryDestination(renderer = fixture.renderer) }.use {
            assertNavigation(Navigator.NavEvent.NavigateTo(Navigator.Screen.Auth)) {
                compose.onNodeWithText("Войти").performClick()
            }
            assertNavigation(Navigator.NavEvent.PopBack) {
                compose.onNodeWithContentDescription("Назад").performClick()
            }
            assertEquals(0, fixture.homes.pages.get())
            assertEquals(0, fixture.homes.details.get())
        }
        val owner = Fixture()
        render { CheckInHistoryDestination(renderer = owner.renderer) }.use {
            compose.onNodeWithContentDescription("Добавить чекин").performClick()
            assertNavigation(Navigator.NavEvent.NavigateTo(Navigator.Screen.CreateCheckIn)) {
                compose.onNodeWithText("Продолжить").performClick()
            }
            assertEquals(0, owner.homes.creates.get())
        }
    }

    @Test fun initialEditIdOpensTheRealEditorOnceAndCloseReturnsToDetailThenHistory() {
        val fixture = Fixture()
        val unrelated = mutableStateOf(0)
        render(dark = true) {
            Text("Generation ${unrelated.value}")
            fixture.renderer.RouteContent("home-one", {}, {}, {}, {})
        }.use {
            compose.onNodeWithText("Редактировать чекин").assertIsDisplayed()
            capture("history-home-edit-dark")
            compose.runOnIdle { unrelated.value++ }
            compose.waitForIdle()
            assertEquals(1, fixture.homes.details.get())
            assertEquals(0, fixture.homes.legacyLists.get())
            assertEquals(0, fixture.homes.legacyCalendars.get())
            compose.onNodeWithContentDescription("Закрыть форму").performClick()
            modalScrollTo("Редактировать")
            compose.onNodeWithText("Редактировать").assertExists()
            compose.onNodeWithContentDescription("Закрыть").performClick()
            compose.onNodeWithText("Редактировать").assertDoesNotExist()
            assertEquals(1, fixture.homes.details.get())
        }
    }

    @Test fun createHomeUsesExistingFormAndSuccessfulWriteRefreshesTheNewTimelineOnly() {
        val fixture = Fixture(empty = true)
        render { fixture.renderer.RouteContent(onBack = {}, onSignIn = {}, onOpenShop = {}, onCreateShop = {}) }.use {
            compose.onNodeWithContentDescription("Добавить чекин").performClick()
            compose.onNodeWithText("Записать приготовление").performClick()
            compose.onNodeWithText("Продолжить").performClick()
            compose.onNodeWithText("Кофе дома").assertIsDisplayed()
            capture("history-home-create-light")
            compose.onNodeWithText("Сохранить чекин").assertIsDisplayed().performClick()
            compose.waitUntil(5_000) { fixture.homes.creates.get() == 1 && fixture.homes.pages.get() >= 2 }
            modalScrollTo("Редактировать")
            compose.onNodeWithText("Редактировать").assertExists()
            compose.onNodeWithContentDescription("Закрыть").performClick()
            rootScrollTo("Создано дома")
            compose.onNodeWithText("Создано дома").assertExists()
            assertEquals(0, fixture.homes.legacyLists.get())
            assertEquals(0, fixture.homes.legacyCalendars.get())
            assertEquals(1, fixture.homes.creates.get())
        }
    }

    @Test fun ownedDetailEditAndConfirmedDeleteUseExistingWriterAndUpdateCombinedHistory() {
        val fixture = Fixture()
        render { fixture.renderer.RouteContent(onBack = {}, onSignIn = {}, onOpenShop = {}, onCreateShop = {}) }.use {
            rootScrollTo("Owned beans")
            compose.onNodeWithText("Owned beans").performClick()
            capture("history-home-detail-light")
            modalScrollTo("Редактировать")
            compose.onNodeWithText("Редактировать").performClick()
            compose.onNodeWithText("Редактировать чекин").assertIsDisplayed()
            compose.onNodeWithText("Сохранить чекин").assertIsDisplayed().performClick()
            compose.waitUntil(5_000) { fixture.homes.updates.get() == 1 }
            modalScrollTo("Удалить запись")
            compose.onNodeWithText("Удалить запись").performClick()
            compose.onNodeWithText("Удалить домашнюю запись?").assertIsDisplayed()
            capture("history-home-delete-light")
            compose.onNodeWithText("Отмена").performClick()
            assertEquals(0, fixture.homes.deletes.get())
            compose.onNodeWithText("Удалить запись").performClick()
            compose.onNodeWithText("Удалить").performClick()
            compose.waitUntil(5_000) { fixture.homes.deletes.get() == 1 && fixture.homes.records.isEmpty() }
            compose.onNodeWithContentDescription("Закрыть").assertDoesNotExist()
            compose.onNodeWithText("Owned beans").assertDoesNotExist()
            assertEquals(0, fixture.homes.legacyLists.get())
            assertEquals(0, fixture.homes.legacyCalendars.get())
        }
    }

    @Test fun pendingSaveLocksTheSheetAndLogoutRemovesDraftWithoutReplayingInitialId() {
        val fixture = Fixture()
        val gate = CompletableDeferred<Unit>()
        fixture.homes.writeGate = gate
        try {
            render { fixture.renderer.RouteContent("home-one", {}, {}, {}, {}) }.use {
                compose.onNodeWithText("Редактировать чекин").assertIsDisplayed()
                compose.onNodeWithText("Сохранить чекин").assertIsDisplayed().performClick()
                compose.waitUntil(5_000) { fixture.homes.updates.get() == 1 }
                compose.onNodeWithContentDescription("Закрыть форму").assertIsNotEnabled()
                compose.onNodeWithContentDescription("Закрыть").assertIsNotEnabled()
                compose.runOnIdle { fixture.sessions.applySession(null) }
                compose.onNodeWithText("Войти").assertExists()
                compose.onNodeWithText("Редактировать чекин").assertDoesNotExist()
                compose.onNodeWithText("Owned beans").assertDoesNotExist()
                compose.runOnIdle { fixture.sessions.applySession(Session("other", userId = "other")) }
                compose.waitForIdle()
                assertEquals(1, fixture.homes.details.get())
                gate.complete(Unit)
                compose.onNodeWithText("Редактировать чекин").assertDoesNotExist()
                compose.onNodeWithText("Owned beans").assertDoesNotExist()
            }
        } finally { gate.complete(Unit) }
    }

    @Test fun failedDetailShowsLocalRetryAndBackDoesNotStartAWrite() {
        val fixture = Fixture().apply { homes.detailFails = true }
        render { fixture.renderer.RouteContent("home-one", {}, {}, {}, {}) }.use {
            compose.onNodeWithText("Не удалось загрузить домашнюю запись").assertExists()
            compose.onNodeWithText("SQL private details").assertDoesNotExist()
            compose.runOnIdle { fixture.homes.detailFails = false }
            compose.onNodeWithText("Повторить").performClick()
            compose.onNodeWithText("Редактировать чекин").assertIsDisplayed()
            assertEquals(2, fixture.homes.details.get())
            compose.onNodeWithContentDescription("Закрыть форму").performClick()
            compose.onNodeWithContentDescription("Закрыть").performClick()
            assertEquals(0, fixture.homes.updates.get())
            assertEquals(0, fixture.homes.creates.get())
        }
    }

    private fun rootScrollTo(text: String) {
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText(text))
    }
    private fun modalScrollTo(text: String) {
        compose.onAllNodes(hasScrollToIndexAction()).onLast().performScrollToNode(hasText(text))
    }
    private fun render(dark: Boolean = false, content: @Composable () -> Unit) = ActivityScenario.launch(ComponentActivity::class.java).also {
        it.onActivity { activity ->
            activity.enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
            activity.setContent { CoffeePeekTheme(darkTheme = dark) { content() } }
        }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        FileOutputStream(File(instrumentation.targetContext.cacheDir, "$name.png")).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
    private fun assertNavigation(expected: Navigator.NavEvent, action: () -> Unit) = runBlocking {
        val event = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(5_000) { Navigator.navigationEvents.first() } }
        action()
        assertEquals(expected, event.await())
    }

    private class Fixture(loggedIn: Boolean = true, empty: Boolean = false) {
        val sessions = Sessions(if (loggedIn) Session("fake-token", userId = "owner") else null)
        val homes = Homes(empty) { sessions.peekSession()?.userId == "owner" }
        private val shops = object : CheckInHistoryRepository {
            private val entry = PersonalCheckIn("shop", HistoryAddress("public-shop", "/coffee-shop/public-shop"),
                "Fixture cafe", null, "Owner", "Shop note", "2026-10-16T10:00:00Z", "2026-10-16T10:00:00Z",
                null, emptyList(), null, null, HistoryVisibility.Private, HistoryModeration.NotSubmitted, 0, null, 0, false)
            override suspend fun page(page: Int, pageSize: Int) = Result.success(CheckInHistoryPage(listOf(entry), 1, page, pageSize))
            override suspend fun period(period: HistoryPeriod) = Result.success(listOf(entry))
        }
        val renderer = AndroidCheckInHistoryScreenRenderer(sessions, shops, homes,
            AndroidCheckInHistoryCalendar({ ZoneOffset.UTC }, Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC)),
            historyHomeEditorFactory(homes, object : CoffeePicker {
                override suspend fun search(query: String) = Result.success(emptyList<CoffeePickerItem>())
            }, BrewMethodCatalog { Result.success(listOf(BrewMethodOption("hario-v60", "Hario V60"))) }),
            imageContent = { _, _, _ -> error("Fixture contains no remote images") },
            photoContent = { _, _, _ -> error("Fixture contains no photos") })
    }

    private class Homes(empty: Boolean, private val owner: () -> Boolean) : HomeCheckInRepository, HomeHistoryReader {
        val records = ConcurrentHashMap<String, HomeCheckIn>()
        val pages = AtomicInteger()
        val details = AtomicInteger()
        val creates = AtomicInteger()
        val updates = AtomicInteger()
        val deletes = AtomicInteger()
        val legacyLists = AtomicInteger()
        val legacyCalendars = AtomicInteger()
        @Volatile var detailFails = false
        @Volatile var writeGate: CompletableDeferred<Unit>? = null
        init { if (!empty) records["home-one"] = home("home-one", "Owned beans") }
        override suspend fun page(page: Int, pageSize: Int, range: HomeHistoryDateRange?): Result<HomeHistoryPage> {
            pages.incrementAndGet()
            val entries = if (owner()) records.values.filter { range == null || it.preparedOn in range.from..range.to } else emptyList()
            return Result.success(HomeHistoryPage(entries.map { it.history() }, entries.size, page, pageSize))
        }
        override suspend fun calendar(range: HomeHistoryDateRange) = Result.success(
            if (owner() && records.isNotEmpty()) listOf(HomeHistoryDay("2026-10-16", records.size)) else emptyList())
        override suspend fun history(page: Int, pageSize: Int, from: String?, to: String?): Result<HomeCheckInPage> {
            legacyLists.incrementAndGet(); error("Legacy duplicate timeline request")
        }
        override suspend fun calendar(from: String, to: String): Result<List<HomeCalendarDay>> {
            legacyCalendars.incrementAndGet(); error("Legacy duplicate calendar request")
        }
        override suspend fun detail(id: String): Result<HomeCheckIn> {
            details.incrementAndGet()
            return if (detailFails) Result.failure(IllegalStateException("SQL private details"))
                else records[id]?.let { Result.success(it) } ?: Result.failure(IllegalStateException("Not found"))
        }
        override suspend fun create(input: HomeCheckInInput): Result<HomeCheckIn> {
            creates.incrementAndGet(); writeGate?.await()
            return home("created", "Создано дома").copy(text = input.text).also { records[it.id] = it }.let { Result.success(it) }
        }
        override suspend fun update(id: String, input: HomeCheckInInput): Result<HomeCheckIn> {
            updates.incrementAndGet(); writeGate?.await()
            return records.getValue(id).copy(text = input.text).also { records[id] = it }.let { Result.success(it) }
        }
        override suspend fun delete(id: String): Result<Unit> {
            deletes.incrementAndGet(); writeGate?.await(); records.remove(id)
            return Result.success(Unit)
        }
        override suspend fun beanStats(): Result<List<HomeBeanStats>> = error("No stats")
        override suspend fun methodStats(): Result<List<HomeMethodStats>> = error("No stats")
        override suspend fun photo(url: String): Result<ByteArray> = error("No images")

        private fun home(id: String, name: String) = HomeCheckIn(id, "2026-10-16T00:30:00Z", "2026-10-16",
            "2026-10-16T00:30:00Z", "Home owner note", "hario-v60", null, name, "18", 180, 4,
            null, null, null, emptyList(), null, HomeCheckInVisibility.Private, HomeCheckInModeration.NotSubmitted)
        private fun HomeCheckIn.history() = HomeHistoryEntry(id, preparedOn, preparedAtUtc, createdAtUtc, text,
            HomeHistoryRecipe(brewMethodCode, coffeeProductId, beanName, doseGrams, brewDurationSeconds, tasteScore),
            drinkName, drinkSlug, customDrinkName, emptyList(), null, HistoryVisibility.Private, HistoryModeration.NotSubmitted)
    }

    private class Sessions(initial: Session?) : SessionRepository {
        private val state = MutableStateFlow(initial)
        override fun peekSession() = state.value
        override fun applySession(session: Session?) { state.value = session }
        override fun isActiveSession(session: Session?) = session?.accessToken?.isNotBlank() == true && !session.userId.isNullOrBlank()
        override suspend fun getSession() = state.value
        override suspend fun persistSession(session: Session?) = Unit
        override suspend fun saveSession(session: Session?) = applySession(session)
        override suspend fun warmCache() = Unit
        override fun observeSession() = state
        override suspend fun isLoggedIn() = isActiveSession(state.value)
    }
}
