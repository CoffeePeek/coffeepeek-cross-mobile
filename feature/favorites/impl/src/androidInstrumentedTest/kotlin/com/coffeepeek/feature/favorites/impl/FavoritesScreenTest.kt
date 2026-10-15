package com.coffeepeek.feature.favorites.impl

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
import com.coffeepeek.feature.favorites.impl.ui.compose.FavoritesScreenContent
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoritesTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // Explicit opt-in fake host: no DI, account, catalogue/photo reads or writes.
        if (intent.getBooleanExtra("roaster_parity_preview", false)) {
            val dark = intent.getBooleanExtra("parity_dark", false)
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            setContent { CoffeePeekTheme(darkTheme = dark) {
                FavoritesScreenContent(FavoritesUiState(isLoading = false, roasters = listOf(
                    FavoriteRoaster("sample", "Тестовый обжарщик", "sample", coffeeShopsCount = 7, coffeeProductsCount = 12),
                    FavoriteRoaster("unaddressed", "Без публичного адреса"),
                )), {})
            } }
        }
    }
}

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
            compose.onNodeWithText("Пока нет избранных кофеен и обжарщиков").assertIsDisplayed()
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

    @Test fun entryConstructsLifecycleOwnedVmAndDelegatesShopRouteToHost() {
        val values = MutableStateFlow(Result.success(listOf(FavoriteShop("id", "Кофейня"))))
        val repo = object : FavoritesRepository {
            override fun observe() = values
            override suspend fun read() = values.value
            override suspend fun save(shop: FavoriteShop) = Result.success(Unit)
            override suspend fun clear() = Result.success(Unit)
            override suspend fun remove(shopId: String) = Result.success(Unit)
        }
        val screen = createFavoritesEntry(repo)
        val selected = mutableStateOf<String?>(null)
        var backRequests = 0
        render {
            if (selected.value == null) screen.Content({ selected.value = it }, { backRequests++ })
            else Text("Shop ${selected.value}")
        }.use {
            compose.onNodeWithText("Кофейня").assertIsDisplayed().performClick()
            compose.onNodeWithText("Shop id").assertIsDisplayed()
            compose.runOnIdle { assertEquals("id", selected.value) }
            compose.runOnIdle { selected.value = null }
            compose.onNodeWithText("Кофейня").assertIsDisplayed()
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.runOnIdle { assertEquals(1, backRequests) }
        }
    }

    @Test fun onlyRoastersAreNotAnEmptyShopScreenInEitherTheme() {
        for (dark in listOf(false, true)) render {
            CoffeePeekTheme(darkTheme = dark) {
                FavoritesScreenContent(FavoritesUiState(isLoading = false, roasters = listOf(
                    FavoriteRoaster("catalog-id", "Любимый обжарщик", "public-slug", coffeeShopsCount = 7,
                        coffeeProductsCount = 12))), {})
            }
        }.use {
            compose.onNodeWithText("Обжарщики").assertIsDisplayed()
            compose.onNodeWithText("Любимый обжарщик").assertIsDisplayed()
            compose.onNodeWithText("7", substring = true).assertIsDisplayed()
            compose.onNodeWithText("12", substring = true).assertIsDisplayed()
            compose.onNodeWithText("Пока нет избранных кофеен и обжарщиков").assertDoesNotExist()
        }
    }

    @Test fun roasterRemoveAndNavigationAreSeparateAndPendingDisablesRemoval() {
        val actions = mutableListOf<FavoritesAction>()
        val state = mutableStateOf(FavoritesUiState(isLoading = false,
            roasters = listOf(FavoriteRoaster("catalog-id", "Обжарщик", "public-slug"))))
        render { FavoritesScreenContent(state.value, actions::add) }.use {
            compose.onNodeWithContentDescription("Убрать из избранного: Обжарщик").performClick()
            compose.onNodeWithText("Обжарщик").performClick()
            compose.runOnIdle {
                assertEquals(listOf(FavoritesAction.RemoveRoaster("catalog-id"),
                    FavoritesAction.OpenRoaster("catalog-id")), actions)
                state.value = state.value.copy(removingRoasters = setOf("catalog-id"))
            }
            compose.onNodeWithContentDescription("Убрать из избранного: Обжарщик").assertIsNotEnabled()
        }
    }

    @Test fun mixedShopAndRoasterIdsDoNotCollideAndOneSourceFailureDoesNotHideRows() {
        val state = FavoritesUiState(shops = listOf(FavoriteShop("same", "Кофейня")), isLoading = false,
            roasters = listOf(FavoriteRoaster("same", "Обжарщик", "public")), roastersLoadFailed = true)
        render { FavoritesScreenContent(state, {}) }.use {
            compose.onNodeWithText("Кофейня").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Обжарщик").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Пока нет избранных кофеен и обжарщиков").assertDoesNotExist()
        }
    }

    @Test fun missingPublicAddressDoesNotInventRoasterNavigationButCanStillRemove() {
        val actions = mutableListOf<FavoritesAction>()
        render { FavoritesScreenContent(FavoritesUiState(isLoading = false,
            roasters = listOf(FavoriteRoaster("saved", "Без адреса"))), actions::add) }.use {
            compose.onNodeWithText("Без адреса").assertIsNotEnabled()
            compose.onNodeWithContentDescription("Убрать из избранного: Без адреса").performClick()
            compose.runOnIdle { assertEquals(listOf(FavoritesAction.RemoveRoaster("saved")), actions) }
        }
    }

    @Test fun entryDelegatesRoasterPublicSlugToCaller() {
        val shops = object : FavoritesRepository {
            override fun observe() = MutableStateFlow(Result.success(emptyList<FavoriteShop>()))
            override suspend fun read() = Result.success(emptyList<FavoriteShop>())
            override suspend fun save(shop: FavoriteShop) = Result.success(Unit)
            override suspend fun remove(shopId: String) = Result.success(Unit)
            override suspend fun clear() = Result.success(Unit)
        }
        val roasters = object : FavoriteRoastersRepository {
            override fun observe() = MutableStateFlow(Result.success(listOf(FavoriteRoaster("saved-id", "Roaster", "public-slug"))))
            override suspend fun remove(roasterId: String) = Result.success(Unit)
        }
        val selected = mutableStateOf<String?>(null)
        val entry = createFavoritesEntry(shops, roasters)
        render {
            if (selected.value == null) entry.Content({}, {}, onOpenRoaster = { selected.value = it })
            else Text("Roaster ${selected.value}")
        }.use {
            compose.onNodeWithText("Roaster").performClick()
            compose.onNodeWithText("Roaster public-slug").assertIsDisplayed()
        }
    }
}
