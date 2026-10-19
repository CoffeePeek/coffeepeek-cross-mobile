package com.coffeepeek.admin.di.checkinhistory

import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDay
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryRecipe
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.domain.model.CheckInHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryAddress
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real application boundary with isolated auth/repository/images; never changes the CoffeePeek account. */
class HistoryCompositionTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val calendar = AndroidCheckInHistoryCalendar({ ZoneOffset.UTC },
        Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC))

    @Test fun guestEntryDoesNotReadAndDelegatesSignInAndBack() {
        val sessions = Sessions(null)
        val repository = Repository()
        var signIns = 0
        var backs = 0
        val renderer = renderer(sessions, repository)
        render { renderer.FixtureContent({ "Fixture date" }, { backs++ }, { signIns++ }, {}) }.use {
            compose.onNodeWithText("Войти").performClick()
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.waitUntil(5_000) { signIns == 1 && backs == 1 }
            assertEquals(0, repository.reads.get())
        }
    }

    @Test fun accountReplacementClosesPhotosAndRemovesPreviousPrivateHistory() {
        val sessions = Sessions()
        val renderer = renderer(sessions, Repository())
        render { renderer.FixtureContent({ "Fixture date" }, {}, {}, {}) }.use {
            compose.onNodeWithText("Owner fixture note").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Фотография 1 из 1").performScrollTo().performClick()
            compose.onNodeWithText("Fixture photo 1").assertIsDisplayed()
            compose.runOnIdle { sessions.applySession(null) }
            compose.onNodeWithText("Чтобы увидеть свои чекины, войдите в аккаунт").assertIsDisplayed()
            compose.onNodeWithText("Fixture photo 1").assertDoesNotExist()
            compose.onNodeWithText("Owner fixture note").assertDoesNotExist()
        }
    }

    @Test fun shopCallbackUsesPublicSlugAndTokenRenewalDoesNotReload() {
        val sessions = Sessions()
        val repository = Repository()
        val shops = mutableListOf<String>()
        val renderer = renderer(sessions, repository)
        render { renderer.FixtureContent({ "Fixture date" }, {}, {}, { shops += it }) }.use {
            // The merged card also contains a photo button: tapping its center can
            // open the photo instead of the shop. Exercise the visible title bounds.
            compose.onNodeWithText("Fixture shop", useUnmergedTree = true)
                .performScrollTo().performTouchInput { click() }
            compose.onNodeWithText("Открыть кофейню").performScrollTo().performClick()
            compose.waitUntil(5_000) { shops.isNotEmpty() }
            assertEquals(listOf("public-slug"), shops)
            compose.onNodeWithText("Fixture photo 1").assertDoesNotExist()
            val before = repository.reads.get()
            compose.runOnIdle { sessions.applySession(Session("renewed", userId = "owner")) }
            compose.waitForIdle()
            assertEquals(before, repository.reads.get())
        }
    }

    @Test fun hidingTheRouteDoesNotPreventLogoutFromCancelingItsRead() {
        val sessions = Sessions()
        val started = AtomicBoolean()
        val cancelled = AtomicBoolean()
        val repository = Repository().apply {
            load = { started.set(true); try { awaitCancellation() } finally { cancelled.set(true) } }
        }
        val shown = mutableStateOf(true)
        val renderer = renderer(sessions, repository)
        render { if (shown.value) renderer.FixtureContent({ "Fixture date" }, {}, {}, {}) }.use {
            compose.waitUntil(5_000) { started.get() }
            compose.runOnIdle { shown.value = false }
            compose.runOnIdle { sessions.applySession(null) }
            compose.waitUntil(5_000) { cancelled.get() }
            compose.runOnIdle { shown.value = true }
            compose.onNodeWithText("Чтобы увидеть свои чекины, войдите в аккаунт").assertIsDisplayed()
            assertTrue(cancelled.get())
            assertEquals(2, repository.reads.get())
        }
    }

    @Test fun combinedHomeActionsUseCallerAndAccountReplacementHidesPrivateHomeRows() {
        val sessions = Sessions()
        val homes = Homes().apply {
            items = listOf(HomeHistoryEntry("home-owner", "2026-10-14", "2026-10-14T08:00:00Z",
                "2026-10-14T08:00:00Z", "Home owner fixture",
                HomeHistoryRecipe("hario-v60", null, "Home beans", "18", 180, 4),
                null, null, null, emptyList(), HistoryVisibility.Private, HistoryModeration.NotSubmitted))
        }
        val opened = mutableListOf<String>()
        var creates = 0
        val renderer = renderer(sessions, Repository(), homes)
        render {
            renderer.Content({ "Fixture date" }, {}, {}, {}, { "11:00" }, opened::add, {}, { creates++ })
        }.use {
            compose.onNodeWithText("Home beans").performScrollTo().performClick()
            compose.waitUntil(5_000) { opened.isNotEmpty() }
            assertEquals(listOf("home-owner"), opened)
            compose.onNodeWithContentDescription("Добавить чекин").performClick()
            compose.onNodeWithText("Записать приготовление").performClick()
            compose.onNodeWithText("Продолжить").performClick()
            compose.waitUntil(5_000) { creates == 1 }
            val before = homes.reads.get()
            compose.runOnIdle { sessions.applySession(Session("renewed", userId = "owner")) }
            compose.waitForIdle()
            assertEquals(before, homes.reads.get())
            compose.runOnIdle { sessions.applySession(null) }
            compose.onNodeWithText("Чтобы увидеть свои чекины, войдите в аккаунт").assertIsDisplayed()
            compose.onNodeWithText("Home owner fixture").assertDoesNotExist()
            compose.onNodeWithText("Home beans").assertDoesNotExist()
            assertEquals(before, homes.reads.get())
        }
    }

    private fun renderer(sessions: Sessions, repository: Repository, homes: HomeHistoryReader = Homes()) = AndroidCheckInHistoryScreenRenderer(
        sessions, repository, homes, calendar, HistoryHomeEditorFactory { _, _, _ -> error("Callback seam must not construct editor") },
        imageContent = { _, modifier, _ -> Box(modifier) },
        photoContent = { _, _, index, dismiss ->
            Button(onClick = dismiss) { Text("Fixture photo ${index + 1}") }
        },
    )

    @Composable
    private fun AndroidCheckInHistoryScreenRenderer.FixtureContent(formatDate: (String) -> String, onBack: () -> Unit,
        onSignIn: () -> Unit, onOpenShop: (String) -> Unit) {
        Content(formatDate, onBack, onSignIn, onOpenShop, { "13:00" }, {}, {}, {})
    }

    private class Homes : HomeHistoryReader {
        val reads = AtomicInteger()
        var items: List<HomeHistoryEntry> = emptyList()
        override suspend fun page(page: Int, pageSize: Int, range: HomeHistoryDateRange?): Result<HomeHistoryPage> {
            reads.incrementAndGet()
            return Result.success(HomeHistoryPage(items, items.size, page, pageSize))
        }
        override suspend fun calendar(range: HomeHistoryDateRange): Result<List<HomeHistoryDay>> {
            reads.incrementAndGet()
            return Result.success(items.groupBy { it.preparedOn }.map { HomeHistoryDay(it.key, it.value.size) })
        }
    }

    // ui-test-manifest declares this debug-only host. Replacing MainActivity's App
    // before its onReady effect runs leaves the production splash gate closed.
    private fun render(content: @Composable () -> Unit) = ActivityScenario.launch(ComponentActivity::class.java).also { scenario ->
        scenario.onActivity { activity -> activity.setContent { CoffeePeekTheme { content() } } }
    }

    private class Repository : CheckInHistoryRepository {
        val reads = AtomicInteger()
        var load: suspend () -> Unit = {}
        private val item = PersonalCheckIn("fixture", HistoryAddress("public-slug", "/coffee-shop/public-slug"),
            "Fixture shop", null, "Fixture owner", "Owner fixture note", "2026-10-14T10:00:00Z",
            "2026-10-14T10:00:00Z", null, listOf("https://fixture.invalid/photo.jpg"), null,
            HistoryVisibility.Private, HistoryModeration.NotSubmitted, 0, null, 0, false)
        override suspend fun page(page: Int, pageSize: Int): Result<CheckInHistoryPage> {
            reads.incrementAndGet(); load()
            return Result.success(CheckInHistoryPage(listOf(item), 1, page, pageSize))
        }
        override suspend fun period(period: HistoryPeriod): Result<List<PersonalCheckIn>> {
            reads.incrementAndGet(); load()
            return Result.success(listOf(item))
        }
    }

    private class Sessions(initial: Session? = Session("token", userId = "owner")) : SessionRepository {
        private val state = MutableStateFlow(initial)
        override fun peekSession() = state.value
        override fun applySession(session: Session?) { state.value = session }
        override fun isActiveSession(session: Session?) = session != null && session.accessToken.isNotBlank() && !session.userId.isNullOrBlank()
        override suspend fun getSession() = state.value
        override suspend fun persistSession(session: Session?) = Unit
        override suspend fun saveSession(session: Session?) = applySession(session)
        override suspend fun warmCache() = Unit
        override fun observeSession() = state
        override suspend fun isLoggedIn() = isActiveSession(state.value)
    }
}
