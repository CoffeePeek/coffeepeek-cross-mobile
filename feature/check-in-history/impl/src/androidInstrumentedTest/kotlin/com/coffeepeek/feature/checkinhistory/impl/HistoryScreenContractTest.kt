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
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.previewCombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.previewHomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDay
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
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

    private fun state(): CombinedHistoryState {
        val base = previewCombinedHistoryState()
        val entry = previewHistoryCheckIn().copy(note = "Owner private note", rating = null, drink = null)
        val row = HistoryTimelineItem.Shop(entry, "2026-10-14")
        return base.copy(shopCalendar = CombinedHistoryLoadState(mapOf(row.date to listOf(row)), hasLoaded = true),
            shops = CombinedHistoryPageState(listOf(row), page = 1, hasLoaded = true))
    }

    @Test fun backWorksBeforeInitialLoadFinishes() {
        val actions = mutableListOf<CombinedHistoryAction>()
        render { CheckInHistoryScreenContent(state().copy(shopCalendar = CombinedHistoryLoadState(emptyMap(), isLoading = true)), actions::add) }.use {
            compose.onNodeWithContentDescription("Назад").performClick()
            assertEquals(listOf<CombinedHistoryAction>(CombinedHistoryAction.Back), actions)
        }
    }

    @Test fun personalAuthorAvatarUsesInjectedLoaderWithoutInventingProfileNavigation() {
        for (dark in listOf(false, true)) {
            val requested = java.util.concurrent.CopyOnWriteArraySet<String>()
            render(dark) {
                HistoryCheckInCard(previewHistoryCheckIn().copy(authorAvatarUrl = "issued-avatar"), "14 октября", {},
                    imageContent = { url, modifier, scale ->
                        assertEquals(androidx.compose.ui.layout.ContentScale.Crop, scale)
                        requested += url
                        Box(modifier)
                    })
            }.use {
                compose.waitForIdle()
                assertEquals(setOf("issued-avatar"), requested.toSet())
                compose.onNodeWithText("Анна").assertHasNoClickAction()
            }
        }
    }

    @Test fun guestNeverComposesPrivateNotesOrImagesEvenIfGivenOldState() {
        val actions = mutableListOf<CombinedHistoryAction>()
        var images = 0
        render { CheckInHistoryScreenContent(state().copy(isLoggedIn = false), actions::add,
            imageContent = { _, _, _ -> images++ }) }.use {
            compose.onNodeWithText("Owner private note").assertDoesNotExist()
            compose.onNodeWithText("Войти").performClick()
            assertEquals(listOf<CombinedHistoryAction>(CombinedHistoryAction.SignIn), actions)
            assertEquals(0, images)
        }
    }

    @Test fun calendarUsesInjectedMonthPhotosEvenWhenSelectedTimelineIsEmpty() {
        for (dark in listOf(false, true)) {
            val requested = java.util.concurrent.CopyOnWriteArraySet<String>()
            val base = state()
            val shop = base.shopCalendar.content.getValue("2026-10-14").single()
            val source = base.copy(
                selectedDate = "2026-10-01",
                shops = CombinedHistoryPageState(), homes = CombinedHistoryPageState(),
                shopCalendar = CombinedHistoryLoadState(mapOf(shop.date to listOf(shop.copy(
                    entry = shop.entry.copy(photoUrls = listOf("fixture-shop-photo"), photoCalendarUrls = listOf("fixture-shop-photo")),
                ))), hasLoaded = true),
                homeCalendar = CombinedHistoryLoadState(mapOf("2026-10-13" to 1, shop.date to 1), hasLoaded = true),
                homeCalendarPhotos = mapOf(
                    "2026-10-13" to listOf(
                        com.coffeepeek.feature.checkinhistory.domain.model.HistoryCalendarPhoto("fixture-home-photo", false)),
                    shop.date to listOf(
                        com.coffeepeek.feature.checkinhistory.domain.model.HistoryCalendarPhoto("fixture-second-photo", false)),
                ),
            )
            render(dark) { CheckInHistoryScreenContent(source, {}, imageContent = { url, modifier, _ ->
                requested += url
                Box(modifier)
            }) }.use {
                compose.waitForIdle()
                assertEquals(setOf("fixture-shop-photo", "fixture-home-photo"), requested.toSet())
                // Two visits use two dots, not a counter of the remaining photographs.
                compose.onNodeWithText("+1", useUnmergedTree = true).assertDoesNotExist()
                compose.onNodeWithText("Owner private note").assertDoesNotExist()
            }
        }
    }

    @Test fun calendarDayAndProductionTypeFilterEmitTypedActions() {
        val actions = mutableListOf<CombinedHistoryAction>()
        render { CheckInHistoryScreenContent(state().copy(selectedDate = "2026-10-14"), actions::add) }.use {
            compose.onNodeWithContentDescription("14 октября 2026, сегодня, 2 чекина, 1 дома").assertIsSelected().performClick()
            compose.onNodeWithContentDescription("Следующий месяц").assertIsNotEnabled()
            compose.onNodeWithText("Дома").performClick()
            assertEquals(listOf<CombinedHistoryAction>(CombinedHistoryAction.SelectDate("2026-10-14"),
                CombinedHistoryAction.SelectFilter(HistoryKindFilter.Home)), actions)
        }
    }

    @Test fun refreshFailureKeepsLoadedListAndShowsAnExplicitRetry() {
        val actions = mutableListOf<CombinedHistoryAction>()
        val source = state()
        render(dark = true) { CheckInHistoryScreenContent(source.copy(shops = source.shops.copy(hasError = true)), actions::add) }.use {
            scrollToText("Owner private note")
            compose.onNodeWithText("Owner private note").assertExists()
            clickRetry("Повторить: Не удалось загрузить посещения кофеен")
            assertEquals(listOf<CombinedHistoryAction>(CombinedHistoryAction.RetryTimeline(HistorySource.Shop)), actions)
        }
    }

    @Test fun paginationErrorDoesNotTriggerAutomaticLoopAndRetriesExplicitly() {
        val actions = mutableListOf<CombinedHistoryAction>()
        val source = state()
        render { CheckInHistoryScreenContent(source.copy(shops = source.shops.copy(hasMore = true, hasLoadMoreError = true)), actions::add) }.use {
            compose.waitForIdle()
            assertTrue(actions.isEmpty())
            clickRetry("Повторить: Не удалось загрузить следующие посещения кофеен")
            assertEquals(listOf<CombinedHistoryAction>(CombinedHistoryAction.RetryLoadMore(HistorySource.Shop)), actions)
        }
    }

    @Test fun photoActionDoesNotNavigateToShopAndUsesTheCompleteAlbumIndex() {
        val entry = previewHistoryCheckIn().copy(photoUrls = List(4) { "https://example.test/photo-$it" })
        var opened = -1
        render { HistoryCheckInCard(entry, "14 октября 2026", { opened = it },
            imageContent = { _, modifier, _ -> Box(modifier) }) }.use {
            compose.onNodeWithContentDescription("Фотография 3 из 4").performClick()
            assertEquals(2, opened)
            compose.onNodeWithText(entry.shopName).assertHasNoClickAction()
        }
    }

    @Test fun removedShopKeepsTheHistoryCardWithoutAFalseNavigationAction() {
        render { HistoryCheckInCard(previewHistoryCheckIn().copy(shop = null), "14 октября 2026", {}) }.use {
            compose.onNodeWithText("Кофейня").assertHasNoClickAction()
        }
    }

    @Test fun drinkLabelRemainsWithoutLoadingARetiredCutout() {
        var imageReads = 0
        render { HistoryCheckInCard(previewHistoryCheckIn(), "14 октября 2026", {},
            imageContent = { _, modifier, _ -> imageReads++; Box(modifier) }) }.use {
            compose.onNodeWithText("Капучино").assertExists()
            compose.waitForIdle()
            assertEquals(0, imageReads)
        }
    }

    @Test fun narrowLargeFontGuestStillOffersSignInAndBack() {
        val actions = mutableListOf<CombinedHistoryAction>()
        render(dark = true) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                Box(Modifier.width(320.dp)) { CheckInHistoryScreenContent(state().copy(isLoggedIn = false), actions::add) }
            }
        }.use {
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.onNodeWithText("Войти").performClick()
            assertEquals(listOf<CombinedHistoryAction>(CombinedHistoryAction.Back, CombinedHistoryAction.SignIn), actions)
        }
    }

    @Test fun actualEntryUsesCallerCallbacksAndDoesNotReadForGuest() {
        val repository = FakeRepository()
        val homes = Homes()
        var signedIn = 0
        val entry = createCheckInHistoryEntry(repository, homes, { FakeCalendar() }, { _, modifier, _ -> Box(modifier) })
        render { entry.Content(5, false, { it }, {}, { signedIn++ }, {}, { _, _, _ -> }, { "10:00" }, {}, {}, {}, 8) }.use {
            compose.onNodeWithText("Войти").performClick()
            compose.waitUntil(5_000) { signedIn == 1 }
            compose.runOnIdle { assertEquals(1, signedIn); assertEquals(0, repository.reads); assertEquals(0, homes.reads) }
        }
    }

    @Test fun actualEntryDoesNotDuplicateStartupForNonzeroRefreshGeneration() {
        val repository = FakeRepository()
        val entry = createCheckInHistoryEntry(repository, Homes(), { FakeCalendar() }, { _, modifier, _ -> Box(modifier) })
        render(dark = true) { entry.Content(5, true, { "14 октября 2026" }, {}, {}, {}, { _, _, _ -> }, { "10:00" }, {}, {}, {}, 8) }.use {
            compose.onNodeWithContentDescription("14 октября 2026, сегодня, 2 чекина, 1 дома").assertExists()
            compose.runOnIdle { assertEquals(2, repository.reads) }
        }
    }

    @Test fun mixedTimelineUsesHomeAndShopIntentsAndPreservesHomePublication() {
        val actions = mutableListOf<CombinedHistoryAction>()
        val base = state()
        val home = base.homes.items.single().let { it.copy(entry = it.entry.copy(
            visibility = com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility.Public,
            moderation = com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration.Pending)) }
        render { CheckInHistoryScreenContent(base.copy(homes = base.homes.copy(items = listOf(home))), actions::add) }.use {
            scrollToText("Кофейня")
            compose.onNodeWithText("Кофейня", substring = false).performClick()
            scrollToText("На модерации")
            compose.onNodeWithText("На модерации").assertExists()
            scrollToText("Эфиопия")
            compose.onNodeWithText("Эфиопия").performClick()
            assertEquals(listOf<CombinedHistoryAction>(CombinedHistoryAction.OpenEntry("shop:preview"),
                CombinedHistoryAction.OpenEntry("home:home")), actions)
            compose.onNodeWithContentDescription("Добавить чекин").performClick()
            assertEquals(CombinedHistoryAction.ShowCreate, actions.last())
            compose.onNodeWithText("Календарь").assertDoesNotExist()
            compose.onNodeWithText("Лента").assertDoesNotExist()
        }
    }

    @Test fun actualEntryDelegatesHomeAndCreateAfterExplicitChoice() {
        val homes = Homes()
        val opened = mutableListOf<String>()
        var created = 0
        var pickerOpened = 0
        var pickerCancelled = 0
        val entry = createCheckInHistoryEntry(FakeRepository(), homes, { FakeCalendar() }, { _, modifier, _ -> Box(modifier) })
        render {
            entry.Content(5, true, { it }, {}, {}, {}, { _, _, _ -> }, { "11:00" },
                opened::add, {}, { created++ }, 8,
                onCreatePickerOpened = { pickerOpened++ }, onCreatePickerCancelled = { pickerCancelled++ })
        }.use {
            scrollToText("Эфиопия")
            compose.onNodeWithText("Эфиопия").performClick()
            compose.waitUntil(5_000) { opened.isNotEmpty() }
            assertEquals(listOf("home"), opened)
            compose.onNodeWithContentDescription("Добавить чекин").performClick()
            compose.onNodeWithText("Записать приготовление").performClick()
            compose.onNodeWithText("Продолжить").performClick()
            compose.waitUntil(5_000) { created == 1 }
            assertEquals(1, pickerOpened)
            assertEquals(0, pickerCancelled)
            compose.onNodeWithText("Где вы пили кофе?").assertDoesNotExist()
            assertEquals(2, homes.reads)
        }
    }

    private fun clickRetry(description: String) {
        // Lazy items below the viewport are not composed until the list scrolls to them.
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription(description))
        compose.onNodeWithContentDescription(description).performClick()
    }

    @Test fun inlineAlbumUsesFeedVariantsButUnavailablePreviewStillOpensTheOriginalIndex() {
        for (dark in listOf(false, true)) {
            val requested = java.util.concurrent.CopyOnWriteArraySet<String>()
            val opened = mutableListOf<Int>()
            render(dark) {
                HistoryCheckInCard(previewHistoryCheckIn().copy(authorAvatarUrl = null,
                    photoUrls = listOf("original-a", "original-b"), photoThumbnailUrls = listOf("feed-a", "")), "14 октября", opened::add,
                    imageContent = { url, modifier, _ -> requested += url; Box(modifier) })
            }.use {
                compose.waitForIdle()
                assertEquals(setOf("feed-a"), requested.toSet())
                compose.onNodeWithContentDescription("Фотография 2 из 2").performClick()
                assertEquals(listOf(1), opened)
            }
        }
    }

    private fun scrollToText(text: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    }

    private class Homes : HomeHistoryReader {
        var reads = 0
        override suspend fun page(page: Int, pageSize: Int, range: HomeHistoryDateRange?): Result<HomeHistoryPage> {
            reads++
            return Result.success(HomeHistoryPage(listOf(previewHomeHistoryEntry()), 1, page, pageSize))
        }
        override suspend fun calendar(range: HomeHistoryDateRange): Result<List<HomeHistoryDay>> {
            reads++
            return Result.success(listOf(HomeHistoryDay("2026-10-14", 1)))
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
