package com.coffeepeek.admin.ui.screen.shopchange

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.ShopChangeRequestRepository
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import java.io.File

@RunWith(AndroidJUnit4::class)
class ShopMenuEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun photosComeFirstAndOptionalDrinkEditsSurviveCollapsing() {
        val vm = ShopChangeEditorViewModel("shop", ShopChangeSection.Menu, "", MenuShops(), MenuChanges())
        val overrides = module { factory { vm } }
        loadKoinModules(overrides)
        try {
            runBlocking { withTimeout(5_000) { vm.state.first { !it.isLoading } } }
            compose.setContent {
                CoffeePeekTheme(darkTheme = false) {
                    ShopChangeEditorScreen("shop", ShopChangeSection.Menu.name, "")
                }
            }
            compose.onNodeWithText("Новые фото меню").assertIsDisplayed()
            compose.onNodeWithText("Необязательно").assertIsDisplayed()
            compose.onNodeWithText("Латте").assertDoesNotExist()
            val photos = compose.onNodeWithText("Новые фото меню").fetchSemanticsNode().boundsInRoot
            val details = compose.onNodeWithText("Указать напитки и цены").fetchSemanticsNode().boundsInRoot
            assertTrue(photos.bottom < details.top)
            screenshot("menu-photos-first.png")

            compose.onNodeWithText("Указать напитки и цены").performClick()
            compose.onNodeWithText("Есть").performScrollTo().performClick()
            compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("7,5")
            compose.onNodeWithText("Объём", substring = true).assertDoesNotExist()
            compose.waitUntil { vm.state.value.menuRows.single().priceText == "7,5" }
            compose.onNodeWithText("Свернуть").performScrollTo().performClick()
            compose.onNodeWithText("Латте").assertDoesNotExist()
            compose.onNodeWithText("Указать напитки и цены").performClick()
            compose.onNodeWithText("7,5").performScrollTo().assertIsDisplayed()
            screenshot("menu-details-expanded.png")
        } finally {
            vm.close()
            unloadKoinModules(overrides)
            loadKoinModules(module {
                factory { (shopId: String, section: ShopChangeSection, requestId: String) ->
                    ShopChangeEditorViewModel(shopId, section, requestId, get(), get())
                }
            })
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: requireNotNull(instrumentation.targetContext.getExternalFilesDir(null))
        check(output.isDirectory || output.mkdirs())
        File(output, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}

private class MenuShops : ShopRepository {
    override suspend fun getCatalogs() = Result.success(ShopCatalogs())
    override suspend fun getMenuDrinks() = Result.success(listOf(CoffeeDrinkDefinition("latte", "Латте", category = "coffee", sortOrder = 0)))
    override suspend fun getShopDetails(id: String) = Result.success(CoffeeShopDetails(
        CoffeeShop(id, "Coffee Shop", null, cityName = "City", priceRange = null, photoUrl = null)))
    override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("Unused")
    override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("Unused")
    override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("Unused")
    override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("Unused")
    override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("Unused")
}

private class MenuChanges : ShopChangeRequestRepository {
    override suspend fun create(draft: ShopChangeDraft): Result<ShopChangeRequest> = error("Unused")
    override suspend fun update(id: String, draft: ShopChangeDraft): Result<ShopChangeRequest> = error("Unused")
    override suspend fun getById(id: String): Result<ShopChangeRequest> = error("Unused")
    override suspend fun getMine(page: Int, pageSize: Int, status: ModerationStatus?, shopId: String?, section: ShopChangeSection?): Result<PagedResult<ShopChangeRequest>> = error("Unused")
}
