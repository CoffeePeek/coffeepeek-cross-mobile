package com.coffeepeek.feature.favorites.impl

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.api.FavoritesRoute
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.impl.navigation.favoritesEntry
import com.coffeepeek.feature.favorites.impl.ui.compose.FavoritesScreenContent
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoritesTestActivity : ComponentActivity()

class FavoritesScreenTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun render(content: @Composable () -> Unit) =
        ActivityScenario.launch(FavoritesTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent { CoffeePeekTheme { content() } } }
        }

    @Test fun loadingAndEmptyAreDistinctAndHaveNoNetworkDependencies() {
        val state = mutableStateOf(FavoritesUiState())
        render { FavoritesScreenContent(state.value, {}) }.use {
            compose.onNodeWithContentDescription("Загрузка избранного").assertIsDisplayed()
            compose.runOnIdle { state.value = FavoritesUiState(isLoading = false) }
            compose.onNodeWithText("Пока нет избранных кофеен").assertIsDisplayed()
            compose.onNodeWithContentDescription("Загрузка избранного").assertDoesNotExist()
        }
    }

    @Test fun errorRetryAndBackEmitCallerActions() {
        val actions = mutableListOf<FavoritesAction>()
        render { FavoritesScreenContent(FavoritesUiState(isLoading = false, loadFailed = true),
            actions::add) }.use {
            compose.onNodeWithText("Не удалось загрузить избранное").assertIsDisplayed()
            compose.onNodeWithText("Повторить").performClick()
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.runOnIdle { assertEquals(listOf(FavoritesAction.Retry, FavoritesAction.Back), actions) }
        }
    }

    @Test fun removeIsSeparateFromCardNavigationAndPendingDisablesIt() {
        val actions = mutableListOf<FavoritesAction>()
        val state = mutableStateOf(FavoritesUiState(listOf(FavoriteShop("id", "Кофейня")), isLoading = false))
        render { FavoritesScreenContent(state.value, actions::add) }.use {
            compose.onNodeWithContentDescription("Удалить из избранного: Кофейня").performClick()
            compose.runOnIdle { assertEquals(listOf(FavoritesAction.Remove("id")), actions) }
            compose.onNodeWithText("Кофейня").performClick()
            compose.runOnIdle {
                assertEquals(listOf(FavoritesAction.Remove("id"), FavoritesAction.OpenShop("id")), actions)
                state.value = state.value.copy(removing = setOf("id"))
            }
            compose.onNodeWithContentDescription("Удалить из избранного: Кофейня").assertIsNotEnabled()
        }
    }

    @Test fun distanceUsesSavedCoordinatesAndSkipsIncompleteLocations() {
        val requested = mutableListOf<Pair<Double, Double>>()
        val shops = listOf(
            FavoriteShop("located", "Кофейня рядом", latitude = 53.9, longitude = 27.56),
            FavoriteShop("missing", "Кофейня без координат", latitude = 53.9),
        )
        render {
            FavoritesScreenContent(
                state = FavoritesUiState(shops = shops, isLoading = false),
                onAction = {},
                distanceForCoordinates = { latitude, longitude ->
                    requested += latitude to longitude
                    "950 м"
                },
            )
        }.use {
            compose.onNodeWithText("950 м", substring = true).assertIsDisplayed()
            compose.runOnIdle { assertEquals(setOf(53.9 to 27.56), requested.toSet()) }
        }
    }

    @Test fun cardShowsAvailableSavedMetadataWithoutFetchingCatalog() {
        val saved = FavoriteShop(
            id = "id", title = "Кофейня", rating = 4.8, reviewCount = 12,
            address = "Кофейная, 1", priceRange = "\$\$", isOpen = true,
            brewMethods = listOf("Эспрессо", "Фильтр"), tags = listOf("Спешелти"),
        )
        render {
            FavoritesScreenContent(FavoritesUiState(shops = listOf(saved), isLoading = false), {})
        }.use {
            compose.onNodeWithText("COFFEEPEEK").assertIsDisplayed()
            compose.onNodeWithText("4.8").assertIsDisplayed()
            compose.onNodeWithText("(12)").assertIsDisplayed()
            compose.onNodeWithText("ОТКРЫТО").assertIsDisplayed()
            compose.onNodeWithText("Эспрессо").assertIsDisplayed()
            compose.onNodeWithText("Кофейная, 1").assertIsDisplayed()
            compose.onNodeWithText("\$\$").assertIsDisplayed()
            compose.onNodeWithText("Спешелти").assertIsDisplayed()
        }
    }

    @Test fun longClosedCardRendersInLightAndDarkThemes() {
        val title = "Кофейня с длинным названием и разными способами заваривания"
        val state = FavoritesUiState(
            shops = listOf(FavoriteShop("closed", title, cityName = "Минск", isOpen = false,
                brewMethods = listOf("Пуровер", "Эспрессо", "Фильтр"))),
            isLoading = false,
        )
        for (dark in listOf(false, true)) {
            render {
                CoffeePeekTheme(darkTheme = dark) {
                    FavoritesScreenContent(state, {})
                }
            }.use {
                compose.onNodeWithText(title).assertIsDisplayed()
                compose.onNodeWithText("ЗАКРЫТО").assertIsDisplayed()
                compose.onNodeWithText("+1").assertIsDisplayed()
            }
        }
    }

    private data class ShopKey(val id: String) : NavKey

    @Test fun navigation3EntryConstructsManualVmAndDelegatesShopRouteToHost() {
        val values = MutableStateFlow(Result.success(listOf(FavoriteShop("id", "Кофейня"))))
        val repo = object : FavoritesRepository {
            override fun observe() = values
            override suspend fun read() = values.value
            override suspend fun save(shop: FavoriteShop) = Result.success(Unit)
            override suspend fun clear() = Result.success(Unit)
            override suspend fun remove(shopId: String) = Result.success(Unit)
        }
        val screen = createFavoritesEntry(repo)
        val stack = mutableStateListOf<NavKey>(FavoritesRoute)
        var backRequests = 0
        render {
            NavDisplay(backStack = stack, onBack = { if (stack.size > 1) stack.removeLast() },
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                entryProvider = entryProvider {
                    favoritesEntry(screen, { stack.add(ShopKey(it)) }, { backRequests++ })
                    entry<ShopKey> { Text("Shop ${it.id}") }
                })
        }.use {
            compose.onNodeWithText("Кофейня").assertIsDisplayed().performClick()
            compose.onNodeWithText("Shop id").assertIsDisplayed()
            compose.runOnIdle { assertEquals(ShopKey("id"), stack.last()) }
            compose.runOnIdle { stack.removeLast() }
            compose.onNodeWithText("Кофейня").assertIsDisplayed()
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.runOnIdle { assertEquals(1, backRequests) }
        }
    }
}
