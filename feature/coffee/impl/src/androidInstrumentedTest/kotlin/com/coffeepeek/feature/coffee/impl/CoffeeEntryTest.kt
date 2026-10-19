package com.coffeepeek.feature.coffee.impl

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.*
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import com.coffeepeek.feature.coffee.impl.ui.compose.component.previewCoffee
import com.coffeepeek.feature.coffee.impl.ui.compose.component.previewCoffeeDetails
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Actual public entries/lifecycle ViewModels with manual fake ports, no app composition. */
class CoffeeEntryTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun render(content: @Composable () -> Unit) = ActivityScenario.launch(CoffeeTestActivity::class.java).also { scenario ->
        scenario.onActivity { it.setContent { CoffeePeekTheme(FontFamily.Default) { Surface { content() } } } }
    }
    private fun awaitName(name: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }

    @Test fun listEntryLoadsThroughOwnRepositoryAndDeliversCurrentCallerCallback() {
        val repository = Repository("Fixture coffee")
        val entry = createCoffeeListEntry(repository) { _, _, modifier, _ -> Box(modifier) }
        val opened = mutableListOf<String>()
        var callbackVersion by mutableIntStateOf(1)
        render { val version = callbackVersion; entry.Content({ opened += "$version:$it" }) }.use {
            awaitName("Fixture coffee")
            compose.runOnIdle { callbackVersion = 2 }
            compose.onNodeWithText("Fixture coffee").performClick()
            compose.waitUntil { opened.isNotEmpty() }
            compose.runOnIdle {
                assertEquals(listOf("2:fixture-coffee"), opened)
                assertEquals(1, repository.searches)
                assertEquals(1, repository.filters)
            }
        }
    }
    @Test fun replacingEntryUnderSameOwnerDoesNotReusePreviousRepositoryViewModel() {
        val first = Repository("First coffee")
        val second = Repository("Second coffee")
        val a = createCoffeeListEntry(first) { _, _, modifier, _ -> Box(modifier) }
        val b = createCoffeeListEntry(second) { _, _, modifier, _ -> Box(modifier) }
        var replacement by mutableStateOf(false)
        render { (if (replacement) b else a).Content({}) }.use {
            awaitName("First coffee")
            compose.runOnIdle { replacement = true }
            awaitName("Second coffee")
            compose.onNodeWithText("First coffee").assertDoesNotExist()
            compose.runOnIdle { assertEquals(1, first.searches); assertEquals(1, second.searches) }
        }
    }
    @Test fun sheetBackDiscardsDraftWhileDoneAppliesAndSearchesOnce() {
        val repository = Repository("Fixture coffee")
        val entry = createCoffeeListEntry(repository) { _, _, modifier, _ -> Box(modifier) }
        render { entry.Content({}) }.use {
            awaitName("Fixture coffee")
            compose.onNodeWithContentDescription("Фильтры").performClick()
            compose.onNodeWithText("Только в наличии").assertIsOff().performClick()
            compose.onNodeWithText("Только в наличии").assertIsOn()
            androidx.test.espresso.Espresso.pressBack()
            compose.waitUntil { compose.onAllNodesWithText("Только в наличии").fetchSemanticsNodes().isEmpty() }
            compose.runOnIdle { assertEquals(1, repository.searches) }
            compose.onNodeWithContentDescription("Фильтры").performClick()
            compose.onNodeWithText("Только в наличии").assertIsOff().performClick()
            compose.onNodeWithText("Готово").performClick()
            compose.waitUntil { repository.searches == 2 }
            compose.onNodeWithContentDescription("Фильтры: 1").assertIsDisplayed()
            compose.onNodeWithText("Только в наличии").assertDoesNotExist()
        }
    }
    @Test fun detailEntryMapsExactShareSellerAndRoasterWithoutOpeningExternalApps() {
        val repository = Repository("Fixture coffee")
        val entry = createCoffeeDetailEntry(repository, { _, _, modifier, _ -> Box(modifier) }, { _, _, _ -> }, { "local:$it" })
        val shares = mutableListOf<Pair<String, String>>()
        val sellers = mutableListOf<String>()
        val roasters = mutableListOf<String>()
        render { entry.Content("requested-slug", {}, {}, roasters::add, sellers::add, { name, path -> shares += name to path }) }.use {
            awaitName("Fixture coffee")
            compose.onNodeWithContentDescription("Поделиться кофе").performClick()
            compose.waitUntil { shares.isNotEmpty() }
            compose.onNodeWithTag("coffee-detail-list").performScrollToNode(hasText("CoffeePeek"))
            compose.onNodeWithText("CoffeePeek", substring = false).performClick()
            compose.waitUntil { roasters.isNotEmpty() }
            compose.onNodeWithTag("coffee-detail-list").performScrollToNode(hasText("Перейти к продавцу"))
            compose.onNodeWithText("Перейти к продавцу").performClick()
            compose.waitUntil { sellers.isNotEmpty() }
            compose.runOnIdle {
                assertEquals(listOf("requested-slug"), repository.detailSlugs)
                assertEquals(listOf("Fixture coffee" to "/coffees/fixture-coffee"), shares)
                assertEquals(listOf("fixture-roaster"), roasters)
                assertEquals(listOf("https://example.invalid/seller"), sellers)
            }
        }
    }

    private class Repository(private val name: String) : CoffeeRepository {
        var searches = 0
        var filters = 0
        val detailSlugs = mutableListOf<String>()
        private val coffee get() = previewCoffee().copy(name = name, countries = emptyList(), tasteCodes = emptyList(),
            offers = listOf(CoffeeOffer(24.5, "BYN", 250, sourceUrl = "https://example.invalid/seller")))
        override suspend fun search(query: String, filters: CoffeeFilters, page: Int): Result<CoffeePage> {
            searches++
            return Result.success(CoffeePage(listOf(coffee), 1, 1))
        }
        override suspend fun getFilterGroups(): Result<List<CoffeeFilterGroup>> { filters++; return Result.success(emptyList()) }
        override suspend fun getDetails(slug: String): Result<CoffeeDetails> {
            detailSlugs += slug
            return Result.success(previewCoffeeDetails().copy(coffee = coffee))
        }
    }
}
