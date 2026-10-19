package com.coffeepeek.feature.coffee.impl

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.*
import com.coffeepeek.feature.coffee.impl.ui.compose.*
import com.coffeepeek.feature.coffee.impl.ui.compose.component.*
import com.coffeepeek.feature.coffee.impl.ui.compose.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** All UI cases use in-memory state and synthetic images, across both themes/type scales. */
@RunWith(Parameterized::class)
class CoffeeContentTest(private val dark: Boolean, private val fontScale: Float) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0},font={1}")
        fun configurations() = listOf(arrayOf<Any>(false, 1f), arrayOf<Any>(true, 1f),
            arrayOf<Any>(false, 1.5f), arrayOf<Any>(true, 1.5f))
    }
    @get:Rule val compose = createEmptyComposeRule()
    private fun render(content: @Composable () -> Unit) = ActivityScenario.launch(CoffeeTestActivity::class.java).also { scenario ->
        scenario.onActivity { it.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                CoffeePeekTheme(FontFamily.Default, darkTheme = dark) { Surface { content() } }
            }
        } }
    }
    private val images: @Composable (String, String, Modifier, androidx.compose.ui.layout.ContentScale) -> Unit =
        { _, _, modifier, _ -> Box(modifier.testTag("fake-image")) }

    @Test fun emptyAndLoadingAreDistinct() {
        var state by mutableStateOf(CoffeeListState())
        render { CoffeeListScreenContent(state, {}, images) }.use {
            compose.onNodeWithContentDescription("Загрузка").assertExists()
            compose.onNodeWithText("Кофе не найден").assertDoesNotExist()
            compose.runOnIdle { state = state.copy(isLoading = false) }
            compose.onNodeWithText("Кофе не найден").assertIsDisplayed()
            compose.onNodeWithContentDescription("Загрузка").assertDoesNotExist()
        }
    }
    @Test fun searchAndFiltersDispatchTypedActions() {
        val actions = mutableListOf<CoffeeListAction>()
        var state by mutableStateOf(CoffeeListState(isLoading = false))
        render { CoffeeListScreenContent(state, { action ->
            actions += action
            if (action is CoffeeListAction.QueryChanged) state = state.copy(query = action.query)
        }, images) }.use {
            compose.onNode(hasSetTextAction()).performTextInput("Эфиопия")
            compose.onNodeWithContentDescription("Фильтры").performClick()
            compose.runOnIdle {
                assertEquals(listOf<CoffeeListAction>(CoffeeListAction.QueryChanged("Эфиопия"), CoffeeListAction.OpenFilters), actions)
            }
        }
    }
    @Test fun loadFailureRetriesFirstPageRatherThanLoadMore() {
        val actions = mutableListOf<CoffeeListAction>()
        render { CoffeeListScreenContent(CoffeeListState(isLoading = false, loadFailed = true), actions::add, images) }.use {
            compose.onNodeWithText("Попробовать снова").performClick()
            compose.runOnIdle { assertEquals(listOf<CoffeeListAction>(CoffeeListAction.Refresh), actions) }
        }
    }
    @Test fun appendFailureRetainsCardsAndRetriesTheAppend() {
        val actions = mutableListOf<CoffeeListAction>()
        render { CoffeeListScreenContent(CoffeeListState(items = listOf(previewCoffee()), isLoading = false, loadMoreFailed = true), actions::add, images) }.use {
            compose.onNodeWithText("Эфиопия", substring = false).assertExists()
            compose.onNodeWithTag("coffee-list").performScrollToNode(hasText("Попробовать снова"))
            compose.onNodeWithText("Попробовать снова").performClick()
            compose.runOnIdle { assertEquals(listOf<CoffeeListAction>(CoffeeListAction.LoadMore), actions) }
        }
    }
    @Test fun weightChangesPriceWithoutOpeningTheCard() {
        var opens = 0
        render { CoffeeCard(previewCoffee(), emptyMap(), { opens++ }, images, Modifier.width(320.dp)) }.use {
            compose.onNodeWithText("24.5").assertIsDisplayed()
            compose.onNodeWithText("500 г").performClick()
            compose.onNodeWithText("48").assertIsDisplayed()
            compose.runOnIdle { assertEquals(0, opens) }
            compose.onNodeWithText("Эфиопия", substring = false).performClick()
            compose.runOnIdle { assertEquals(1, opens) }
        }
    }
    @Test fun cardKeeps216DpProductPhotoAndInjectedImagePort() {
        render { CoffeeCard(previewCoffee().copy(photoUrl = "fixture://coffee"), emptyMap(), {}, images, Modifier.width(320.dp)) }.use {
            compose.onNodeWithTag("fake-image", useUnmergedTree = true).assertHeightIsEqualTo(216.dp)
        }
    }
    @Test fun filterContentReportsDraftActionsWithoutApplyingThemImplicitly() {
        val actions = mutableListOf<CoffeeListAction>()
        val state = CoffeeListState(filterDraft = CoffeeFilters(), filtersLoading = false,
            filterGroups = listOf(CoffeeFilterGroup("taste", "Вкус", listOf(CoffeeFilterOption("berries", "Ягоды")))))
        render { CoffeeFiltersContent(state, actions::add) }.use {
            compose.onNodeWithText("Ягоды").assertIsOff().performClick()
            compose.onNodeWithText("Только в наличии").performClick()
            compose.onNodeWithText("Готово").performClick()
            compose.runOnIdle { assertEquals(listOf<CoffeeListAction>(CoffeeListAction.FilterToggled("taste", "berries"),
                CoffeeListAction.AvailableOnlyChanged(true), CoffeeListAction.ApplyFilters), actions) }
        }
    }
    @Test fun metadataRetryDoesNotTriggerCatalogueRefresh() {
        val actions = mutableListOf<CoffeeListAction>()
        render { CoffeeFiltersContent(CoffeeListState(filterDraft = CoffeeFilters(), filtersLoading = false, filtersFailed = true), actions::add) }.use {
            compose.onNodeWithText("Попробовать снова").performClick()
            compose.runOnIdle { assertEquals(listOf<CoffeeListAction>(CoffeeListAction.RetryFilters), actions) }
        }
    }
    @Test fun detailFailureOffersRetryBackButNotSharing() {
        val actions = mutableListOf<CoffeeDetailAction>()
        render { CoffeeDetailScreenContent(CoffeeDetailState(isLoading = false, loadFailed = true), actions::add, images, { _, _, _ -> }, { it }) }.use {
            compose.onNodeWithContentDescription("Поделиться кофе").assertIsNotEnabled()
            compose.onNodeWithText("Попробовать снова").performClick()
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.runOnIdle { assertEquals(listOf<CoffeeDetailAction>(CoffeeDetailAction.Retry, CoffeeDetailAction.Back), actions) }
        }
    }
    @Test fun photoGalleryUsesExactUrlsAndCanCloseWithoutPlatformAccess() {
        val urls = listOf("fixture://first", "fixture://second")
        var state by mutableStateOf(CoffeeDetailState(details = previewCoffeeDetails().copy(photos = urls), isLoading = false))
        var renderedUrls: List<String>? = null
        render { CoffeeDetailScreenContent(state, { action ->
            state = state.copy(arePhotosOpen = action == CoffeeDetailAction.OpenPhotos)
        }, images, { actualUrls, index, close ->
            renderedUrls = actualUrls
            assertEquals(0, index)
            androidx.compose.ui.window.Dialog(close) { TextButton(close) { Text("Закрыть галерею") } }
        }, { it }) }.use {
            compose.onNodeWithContentDescription("Открыть фото кофе").performClick()
            compose.onNodeWithText("Закрыть галерею").performClick()
            compose.onNodeWithText("Закрыть галерею").assertDoesNotExist()
            compose.runOnIdle { assertEquals(urls, renderedUrls); assertFalse(state.arePhotosOpen) }
        }
    }
    @Test fun missingSellerIsDisabledAndUnknownAvailabilityIsExplicit() {
        var opens = 0
        render { CoffeeOfferCard(CoffeeOffer(null, null, null), emptyMap(), { it }, { opens++ }) }.use {
            compose.onNodeWithText("Наличие не подтверждено").assertIsDisplayed()
            compose.onNodeWithText("Цена не указана").assertIsDisplayed()
            compose.onNodeWithText("Перейти к продавцу").assertIsNotEnabled()
            compose.runOnIdle { assertEquals(0, opens) }
        }
    }
    @Test fun detailSellerUsesOfferIndexAndShareRemainsAppOwned() {
        val actions = mutableListOf<CoffeeDetailAction>()
        val details = previewCoffeeDetails().copy(coffee = previewCoffee().copy(offers = listOf(CoffeeOffer(20.0, "BYN", 250, sourceUrl = "https://example.invalid"))))
        render { CoffeeDetailScreenContent(CoffeeDetailState(details = details, isLoading = false), actions::add, images, { _, _, _ -> }, { it }) }.use {
            compose.onNodeWithContentDescription("Поделиться кофе").performClick()
            compose.onNodeWithTag("coffee-detail-list").performScrollToNode(hasText("Перейти к продавцу"))
            compose.onNodeWithText("Перейти к продавцу").performClick()
            compose.runOnIdle { assertEquals(listOf<CoffeeDetailAction>(CoffeeDetailAction.Share, CoffeeDetailAction.OpenSeller(0)), actions) }
        }
    }
}
