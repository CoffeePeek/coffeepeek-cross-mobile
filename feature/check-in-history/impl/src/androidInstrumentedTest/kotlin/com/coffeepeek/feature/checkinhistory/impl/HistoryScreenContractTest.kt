package com.coffeepeek.feature.checkinhistory.impl

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.domain.model.CheckInHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryDateBounds
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.CheckInHistoryScreenContent
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.HistoryCheckInCard
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.previewHistoryCheckIn
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Self-targeted test APK and fake state only: no application DI/database/HTTP. */
class HistoryScreenContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(dark: Boolean = false, content: @Composable () -> Unit): ActivityScenario<HistoryTestActivity> =
        ActivityScenario.launch(HistoryTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = dark) { content() }
            } }
        }

    private fun state(mode: CheckInHistoryMode = CheckInHistoryMode.Calendar): CheckInHistoryState {
        val entry = previewHistoryCheckIn().copy(note = "Owner private note", rating = null, drink = null)
        return CheckInHistoryState("2026-10-14", true, 0,
            HistoryCalendarState(HistoryMonth(2026, 10), mapOf("2026-10-14" to listOf(entry)), "2026-10-14", true),
            HistoryListState(listOf(entry), page = 1, hasLoaded = true), mode)
    }

    @Test fun backWorksBeforeInitialLoadFinishes() {
        val actions = mutableListOf<CheckInHistoryAction>()
        render { CheckInHistoryScreenContent(state().copy(calendar = HistoryCalendarState(HistoryMonth(2026, 10), isLoading = true)), actions::add) }.use {
            compose.onNodeWithContentDescription("Назад").performClick()
            assertEquals(listOf<CheckInHistoryAction>(CheckInHistoryAction.Back), actions)
        }
    }

    @Test fun guestNeverComposesPrivateNotesOrImagesEvenIfGivenOldState() {
        val actions = mutableListOf<CheckInHistoryAction>()
        var images = 0
        render { CheckInHistoryScreenContent(state().copy(isLoggedIn = false), actions::add,
            imageContent = { _, _, _ -> images++ }) }.use {
            compose.onNodeWithText("Owner private note").assertDoesNotExist()
            compose.onNodeWithText("Войти").performClick()
            assertEquals(listOf<CheckInHistoryAction>(CheckInHistoryAction.SignIn), actions)
            assertEquals(0, images)
        }
    }

    @Test fun calendarDayHasSelectedLocalizedSemanticsAndModeActionsAreTyped() {
        val actions = mutableListOf<CheckInHistoryAction>()
        render { CheckInHistoryScreenContent(state(), actions::add) }.use {
            compose.onNodeWithContentDescription("14 октября 2026, 1 чекин").assertIsSelected().performClick()
            compose.onNodeWithContentDescription("Следующий месяц").assertIsNotEnabled()
            compose.onNodeWithText("Лента").performClick()
            assertEquals(listOf<CheckInHistoryAction>(CheckInHistoryAction.SelectDate("2026-10-14"),
                CheckInHistoryAction.SelectMode(CheckInHistoryMode.List)), actions)
        }
    }

    @Test fun refreshFailureKeepsLoadedListAndShowsAnExplicitRetry() {
        val actions = mutableListOf<CheckInHistoryAction>()
        val source = state(CheckInHistoryMode.List)
        render(dark = true) { CheckInHistoryScreenContent(source.copy(list = source.list.copy(hasError = true)), actions::add) }.use {
            compose.onNodeWithText("Owner private note").assertExists()
            compose.onNodeWithText("Повторить").performClick()
            assertEquals(listOf<CheckInHistoryAction>(CheckInHistoryAction.Retry), actions)
        }
    }

    @Test fun paginationErrorDoesNotTriggerAutomaticLoopAndRetriesExplicitly() {
        val actions = mutableListOf<CheckInHistoryAction>()
        val source = state(CheckInHistoryMode.List)
        render { CheckInHistoryScreenContent(source.copy(list = source.list.copy(hasMore = true, hasLoadMoreError = true)), actions::add) }.use {
            compose.waitForIdle()
            assertTrue(actions.isEmpty())
            compose.onNodeWithText("Повторить").performClick()
            assertEquals(listOf<CheckInHistoryAction>(CheckInHistoryAction.RetryLoadMore), actions)
        }
    }

    @Test fun photoActionDoesNotNavigateToShopAndUsesTheCompleteAlbumIndex() {
        val entry = previewHistoryCheckIn().copy(photoUrls = List(4) { "https://example.test/photo-$it" })
        var opened = -1
        var shopCalls = 0
        render { HistoryCheckInCard(entry, "14 октября 2026", { shopCalls++ }, { opened = it },
            imageContent = { _, modifier, _ -> Box(modifier) }) }.use {
            compose.onNodeWithContentDescription("Фотография 3 из 4").performClick()
            assertEquals(2, opened)
            assertEquals(0, shopCalls)
        }
    }

    @Test fun removedShopKeepsTheHistoryCardWithoutAFalseNavigationAction() {
        var shopCalls = 0
        render { HistoryCheckInCard(previewHistoryCheckIn().copy(shop = null), "14 октября 2026", { shopCalls++ }, {}) }.use {
            compose.onNodeWithText("Кофейня").assertHasNoClickAction()
            assertEquals(0, shopCalls)
        }
    }

    @Test fun drinkImageUsesFitThroughInjectedLoaderNotASeparateNetworkClient() {
        val scales = mutableListOf<androidx.compose.ui.layout.ContentScale>()
        val entry = previewHistoryCheckIn().copy(drinkPhotoUrl = "https://example.test/drink.png")
        render { HistoryCheckInCard(entry, "14 октября 2026", {}, {}, imageContent = { _, modifier, scale ->
            scales += scale
            Text("Fake protected image", modifier)
        }) }.use {
            compose.onNodeWithText("Fake protected image").assertExists()
            assertTrue(scales.isNotEmpty())
            assertTrue(scales.all { it == androidx.compose.ui.layout.ContentScale.Fit })
        }
    }

    @Test fun narrowLargeFontGuestStillOffersSignInAndBack() {
        val actions = mutableListOf<CheckInHistoryAction>()
        render(dark = true) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                Box(Modifier.width(320.dp)) { CheckInHistoryScreenContent(state().copy(isLoggedIn = false), actions::add) }
            }
        }.use {
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.onNodeWithText("Войти").performClick()
            assertEquals(listOf<CheckInHistoryAction>(CheckInHistoryAction.Back, CheckInHistoryAction.SignIn), actions)
        }
    }

    @Test fun actualEntryUsesCallerCallbacksAndDoesNotReadForGuest() {
        val repository = FakeRepository()
        var signedIn = 0
        val entry = createCheckInHistoryEntry(repository, { FakeCalendar() }, { _, modifier, _ -> Box(modifier) })
        render { entry.Content(5, false, { it }, {}, { signedIn++ }, {}, { _, _ -> }, 8) }.use {
            compose.onNodeWithText("Войти").performClick()
            compose.waitUntil(5_000) { signedIn == 1 }
            compose.runOnIdle { assertEquals(1, signedIn); assertEquals(0, repository.reads) }
        }
    }

    @Test fun actualEntryDoesNotDuplicateStartupForNonzeroRefreshGeneration() {
        val repository = FakeRepository()
        val entry = createCheckInHistoryEntry(repository, { FakeCalendar() }, { _, modifier, _ -> Box(modifier) })
        render(dark = true) { entry.Content(5, true, { "14 октября 2026" }, {}, {}, {}, { _, _ -> }, 8) }.use {
            compose.onNodeWithContentDescription("14 октября 2026, 1 чекин").assertExists()
            compose.runOnIdle { assertEquals(1, repository.reads) }
        }
    }

    private class FakeCalendar : CheckInHistoryCalendarSnapshot {
        override val today = "2026-10-14"
        override val zoneId = "UTC"
        override fun monthPeriod(year: Int, month: Int) = CheckInHistoryDateBounds(
            "${HistoryMonth(year, month).date(1)}T00:00:00Z", "${HistoryMonth(year, month).plusMonths(1).date(1)}T00:00:00Z")
        override fun localDate(instant: String) = instant.substringBefore('T')
    }

    private class FakeRepository : CheckInHistoryRepository {
        var reads = 0
        override suspend fun page(page: Int, pageSize: Int): Result<CheckInHistoryPage> {
            reads++
            return Result.success(CheckInHistoryPage(listOf(previewHistoryCheckIn()), 1, page, pageSize))
        }
        override suspend fun period(period: HistoryPeriod): Result<List<PersonalCheckIn>> {
            reads++
            return Result.success(listOf(previewHistoryCheckIn()))
        }
    }
}
