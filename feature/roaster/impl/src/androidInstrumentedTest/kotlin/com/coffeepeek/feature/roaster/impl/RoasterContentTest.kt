package com.coffeepeek.feature.roaster.impl

import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.domain.model.RoasterAddress
import com.coffeepeek.feature.roaster.domain.model.RoasterContact
import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.domain.model.RoasterLocation
import com.coffeepeek.feature.roaster.domain.model.RoasterPhoto
import com.coffeepeek.feature.roaster.domain.model.RoasterShop
import com.coffeepeek.feature.roaster.domain.model.RoasterSummary
import com.coffeepeek.feature.roaster.domain.model.RoasterTag
import com.coffeepeek.feature.roaster.impl.ui.compose.RoasterDetailScreenContent
import com.coffeepeek.feature.roaster.impl.ui.compose.RoasterDiscoverySectionContent
import com.coffeepeek.feature.roaster.api.RoasterImage
import com.coffeepeek.feature.roaster.impl.ui.compose.RoasterListScreenContent
import com.coffeepeek.feature.roaster.impl.ui.compose.component.RoasterCard
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailState
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListItem
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListState
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** All cases use in-memory state and synthetic images, across both themes and type scales. */
@RunWith(Parameterized::class)
class RoasterContentTest(private val dark: Boolean, private val fontScale: Float) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0},font={1}")
        fun configurations() = listOf(arrayOf<Any>(false, 1f), arrayOf<Any>(true, 1f),
            arrayOf<Any>(false, 1.5f), arrayOf<Any>(true, 1.5f))
    }

    @get:Rule val compose = createEmptyComposeRule()

    private fun render(content: @Composable () -> Unit) = ActivityScenario.launch(RoasterTestActivity::class.java).also { scenario ->
        scenario.onActivity { it.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                CoffeePeekTheme(FontFamily.Default, darkTheme = dark) { Surface { content() } }
            }
        } }
    }

    private val image: RoasterImage = { _, _, modifier, _, _ -> Box(modifier.testTag("fake-image")) }
    private val retryError: @Composable (() -> Unit) -> Unit = { onRetry -> Text("Ошибка", Modifier.testTag("error")); androidx.compose.material3.TextButton(onRetry) { Text("Повторить список") } }

    private fun address(slug: String) = RoasterAddress(slug, "/roasters/$slug", 1, false)
    private fun item(slug: String?, name: String = "Roaster $slug", tags: List<RoasterTag> = emptyList()) =
        RoasterListItem(RoasterSummary(slug?.let(::address), name, null, tags, 7, 0, 3))
    private fun details(shops: List<RoasterShop> = emptyList(), contact: RoasterContact? = RoasterContact("@my.roast", "roast.example/x")) = RoasterDetails(
        "roast", address("roast"), "Roast Lab", "Про обжарщика", RoasterLocation("Минск, Улица 1", null, null), contact,
        listOf(RoasterPhoto("1", "https://x/1", 1), RoasterPhoto("2", "https://x/2", 2)), shops)

    @Test fun loadingEmptyAndErrorStatesAreDistinct() {
        var state by mutableStateOf(RoasterListState())
        val actions = mutableListOf<RoasterListAction>()
        render { RoasterListScreenContent(state, { actions += it }, image, retryError) }.use {
            compose.onAllNodesWithContentDescription("Загрузка").onFirst().assertExists()
            compose.onNodeWithText("Обжарщики не найдены").assertDoesNotExist()
            compose.runOnIdle { state = state.copy(isLoading = false) }
            compose.onNodeWithText("Обжарщики не найдены").assertIsDisplayed()
            compose.runOnIdle { state = state.copy(loadFailed = true) }
            compose.onNodeWithTag("error").assertIsDisplayed()
            compose.onNodeWithText("Повторить список").performClick()
            compose.runOnIdle { assertTrue(RoasterListAction.Refresh in actions) }
        }
    }

    @Test fun cardsFilterByStateAndDispatchOpenFavoriteAndTagActions() {
        val actions = mutableListOf<RoasterListAction>()
        val state = RoasterListState(items = listOf(item("a", "Alpha", listOf(RoasterTag("light", "Светлая", null, 1))), item("b", "Beta"), item(null, "No address")),
            isLoading = false, favoriteKeys = setOf("a"), query = "")
        render { RoasterListScreenContent(state, { actions += it }, image, retryError) }.use {
            compose.onNodeWithText("Alpha").assertIsDisplayed()
            compose.onNodeWithText("Beta").assertIsDisplayed()
            compose.onNodeWithText("Светлая").performClick()
            compose.onNodeWithText("Все").performClick()
            compose.onNodeWithText("Alpha").performClick()
            compose.onNodeWithContentDescription("Убрать из избранного").performClick()
            compose.runOnIdle {
                assertTrue(RoasterListAction.ToggleTag("light") in actions)
                assertTrue(RoasterListAction.ClearTags in actions)
                assertTrue(RoasterListAction.OpenRoaster("a") in actions)
                assertTrue(RoasterListAction.ToggleFavorite("a") in actions)
                assertTrue(actions.filterIsInstance<RoasterListAction.LoadDetails>().any { it.slug == "a" })
            }
        }
    }

    @Test fun cardWithoutAddressIsNotInteractiveAndFavoriteMessageIsShownThenDismissed() {
        val actions = mutableListOf<RoasterListAction>()
        val state = RoasterListState(items = listOf(item(null, "No address")), isLoading = false, message = RoasterMessage.FavoriteChangeFailed)
        render { RoasterListScreenContent(state, { actions += it }, image, retryError) }.use {
            compose.onNodeWithText("No address").performClick()
            compose.onNodeWithText("Не удалось изменить избранное").assertIsDisplayed()
            // The snackbar reports dismissal when its (short) duration ends.
            compose.waitUntil(timeoutMillis = 10_000) { RoasterListAction.DismissMessage in actions.toList() }
            compose.runOnIdle { assertTrue(actions.none { it is RoasterListAction.OpenRoaster || it is RoasterListAction.ToggleFavorite }) }
        }
    }

    @Test fun discoveryCarouselShowsAtMostEightCardsWithTagsAndKeepsCardWidthRule() {
        val items = (1..10).map { item("r$it", "Roaster $it", listOf(RoasterTag("t", "Тег", null, 1))) }
        render { RoasterDiscoverySectionContent(RoasterListState(items = items, isLoading = false), {}, image, retryError) }.use {
            compose.onNodeWithText("Тег").assertIsDisplayed()
            compose.onNodeWithText("Roaster 1").assertIsDisplayed()
            compose.onNodeWithTag("roaster-carousel").performScrollToIndex(7)
            compose.onNodeWithText("Roaster 8").assertExists()
            compose.onNodeWithText("Roaster 9").assertDoesNotExist()
        }
    }

    @Test fun cardKeepsItsStatsFooterWhenTheCarouselGivesAnExactSmallHeight() {
        render { Box(Modifier.fillMaxWidth().height(96.dp)) {
            RoasterCard("Roaster", null, "Длинное описание ".repeat(20), 7, 21, isFavorite = false, onOpen = {}, onToggleFavorite = {}, image = image,
                modifier = Modifier.height(96.dp).testTag("card"))
        } }.use {
            compose.onNodeWithTag("card").assertHeightIsEqualTo(96.dp)
            compose.onNodeWithText("Кофейни").assertIsDisplayed()
            compose.onNodeWithText("Каталог").assertIsDisplayed()
        }
    }

    @Test fun detailRendersSectionsAndDispatchesTypedActions() {
        val actions = mutableListOf<RoasterDetailAction>()
        val state = RoasterDetailState(isLoading = false, details = details(listOf(RoasterShop("cafe", "Sandbox Cafe", null, address("cafe")))), favoriteKeys = emptySet())
        render { RoasterDetailScreenContent(state, { actions += it }, image, { _, _ -> }) }.use {
            compose.onNodeWithText("Про обжарщика").assertExists()
            compose.onNodeWithText("Минск, Улица 1").assertExists()
            compose.onNodeWithContentDescription("Добавить в избранное").performClick()
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.onNodeWithText("@my.roast").performClick()
            compose.onNodeWithText("roast.example/x").performClick()
            compose.onNodeWithText("Sandbox Cafe").performClick()
            compose.runOnIdle {
                assertEquals(listOf(RoasterDetailAction.ToggleFavorite, RoasterDetailAction.Back, RoasterDetailAction.OpenInstagram,
                    RoasterDetailAction.OpenWebsite, RoasterDetailAction.OpenShop("cafe")), actions)
            }
        }
    }

    @Test fun detailWithoutContactsLocationOrShopsShowsTheEmptyShopsNoteOnly() {
        val state = RoasterDetailState(isLoading = false, details = details(contact = null).copy(location = null, about = null))
        render { RoasterDetailScreenContent(state, {}, image, { _, _ -> }) }.use {
            compose.onNodeWithText("Пока нет кофеен, которые указали этого обжарщика.").assertExists()
            compose.onNodeWithText("Адрес").assertDoesNotExist()
            compose.onNodeWithText("Ссылки").assertDoesNotExist()
            compose.onNodeWithText("Об обжарщике").assertDoesNotExist()
        }
    }

    @Test fun detailPhotoPreviewIsHostedByTheApplicationViewerAndClosesThroughAnAction() {
        val actions = mutableListOf<RoasterDetailAction>()
        val state = RoasterDetailState(isLoading = false, details = details(), previewPhotoUrl = "https://x/1")
        // The app's viewer is a dialog window; the fake mirrors that so it sits above the screen.
        render { RoasterDetailScreenContent(state, { actions += it }, image, { url, onDismiss ->
            Dialog(onDismissRequest = onDismiss) { Text("viewer:$url", Modifier.testTag("viewer").androidxClickable(onDismiss)) }
        }) }.use {
            compose.onNodeWithTag("viewer").assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(listOf<RoasterDetailAction>(RoasterDetailAction.ClosePhoto), actions) }
        }
    }

    @Test fun detailLoadingAndFailureStatesAreDistinct() {
        var state by mutableStateOf(RoasterDetailState())
        val actions = mutableListOf<RoasterDetailAction>()
        render { RoasterDetailScreenContent(state, { actions += it }, image, { _, _ -> }) }.use {
            compose.onNodeWithContentDescription("Загрузка").assertExists()
            compose.runOnIdle { state = RoasterDetailState(isLoading = false, loadFailed = true) }
            compose.onNodeWithText("Не удалось загрузить обжарщика").assertIsDisplayed()
            compose.onNodeWithText("Повторить").performClick()
            compose.onNodeWithText("Назад").performClick()
            compose.runOnIdle { assertEquals(listOf(RoasterDetailAction.Retry, RoasterDetailAction.Back), actions) }
        }
    }
}

private fun Modifier.androidxClickable(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
