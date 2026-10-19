package com.coffeepeek.feature.shop.impl

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.ui.compose.ShopCheckInListScreenContent
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopPublicCheckInsSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.previewCheckIn
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListState
import com.coffeepeek.feature.shop.api.ShopCheckInAuthor
import com.coffeepeek.feature.shop.api.ShopDetailEffect
import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.feature.shop.domain.repository.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Only fake state and recording callbacks; no application, account, HTTP or block writes. */
class ShopAuthorBlockMenuContractTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val row = previewCheckIn().copy(authorId = "public-id", authorSlug = "public-author", rating = null)
    private val verticalList = hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    private fun state() = ShopCheckInListState(isLoading = false, hasLoaded = true, shopTitle = "Coffee",
        checkIns = listOf(row), isLoggedIn = true, currentUserId = "viewer")

    private fun render(dark: Boolean, content: @Composable () -> Unit) =
        ActivityScenario.launch(ShopFormTestActivity::class.java).also { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = dark) { content() }
            } }
        }

    private fun openListMenu() {
        compose.onNode(verticalList).performScrollToNode(hasContentDescription("Действия с чекином"))
        compose.onNodeWithContentDescription("Действия с чекином").performClick()
        compose.waitUntil { compose.onAllNodesWithText("Пожаловаться").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun listMenuSeparatesBlockFromReportAndDismissesAfterEitherActionInBothThemes() {
        for (dark in listOf(false, true)) {
            val actions = mutableListOf<ShopCheckInListAction>()
            render(dark) { ShopCheckInListScreenContent(state(), actions::add) }.use {
                openListMenu()
                compose.onNodeWithText("Заблокировать автора").assertIsDisplayed().performClick()
                compose.onNodeWithText("Заблокировать автора").assertDoesNotExist()
                assertEquals(listOf(ShopCheckInListAction.BlockAuthor(row.id)), actions)
                openListMenu()
                compose.onNodeWithText("Пожаловаться").performClick()
                compose.onNodeWithText("Пожаловаться").assertDoesNotExist()
                assertEquals(listOf(ShopCheckInListAction.BlockAuthor(row.id), ShopCheckInListAction.Report(row.id)), actions)
            }
        }
    }

    @Test fun ownRowsHaveNoMenuAndUnknownSignedInProfileNeverOffersBlocking() {
        render(false) { ShopCheckInListScreenContent(state().copy(currentUserId = row.userId), {}) }.use {
            compose.onNode(verticalList).performScrollToNode(hasText(row.username))
            compose.onNodeWithContentDescription("Действия с чекином").assertDoesNotExist()
        }
        for (slug in listOf(null, row.authorSlug)) render(true) {
            ShopCheckInListScreenContent(state().copy(currentUserId = slug), {})
        }.use {
            openListMenu()
            compose.onNodeWithText("Заблокировать автора").assertDoesNotExist()
            compose.onNodeWithText("Пожаловаться").assertIsDisplayed()
        }
    }

    @Test fun guestDetailPreviewHasBlockOnlyForTheVisibleAuthorInBothThemes() {
        for (dark in listOf(false, true)) {
            val requested = mutableListOf<String>()
            render(dark) { Column {
                ShopPublicCheckInsSection(listOf(row, row.copy(id = "hidden", username = "Hidden author")),
                    "Coffee", false, null, emptySet(), null, { _, _ -> }, {}, {}, {}, {}, {},
                    onBlockAuthor = requested::add)
            } }.use {
                compose.onNodeWithContentDescription("Действия с чекином").performClick()
                compose.waitUntil { compose.onAllNodesWithText("Заблокировать автора").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Пожаловаться").assertDoesNotExist()
                compose.onNodeWithText("Заблокировать автора").performClick()
                assertEquals(listOf(row.id), requested)
                compose.onNodeWithText("Hidden author").assertDoesNotExist()
                assertTrue(requested.none { it == "hidden" })
            }
        }
    }

    @Test fun publicEntriesDeliverOnlyTheIssuedAuthorAndLocalizedNameWithoutAnotherReadOrWrite() {
        for (detail in listOf(false, true)) {
            var reads = 0
            val repository = object : ShopDetailsRepository {
                override suspend fun getDetails(shopId: String): Result<ShopDetails> {
                    reads++
                    return Result.success(ShopDetails(
                        ShopOverview("shop", "Coffee", null, null, null, null, null, 1, true, emptyList()),
                        null, emptyList(), ShopCoffeeDetails(), null, emptyList(), emptyList(), emptyList(),
                        listOf(row.copy(username = ""))))
                }
            }
            val votes = object : ShopCheckInVoteRepository {
                override suspend fun setHelpful(checkInId: String, helpful: Boolean): Result<ShopHelpfulVote> = error("No writes")
            }
            val favorites = object : ShopFavorites {
                override fun observeIds() = flowOf(Result.success(emptySet<String>()))
                override suspend fun save(shop: ShopDetails): Result<Unit> = error("No writes")
                override suspend fun remove(shopId: String): Result<Unit> = error("No writes")
            }
            val requested = mutableListOf<ShopCheckInAuthor>()
            val listEntry = createShopCheckInListEntry(repository, votes, { Result.success(ShopViewer(true, "viewer")) })
            val detailEntry = createShopDetailEntry(repository, votes, favorites,
                { Result.success(ShopViewer(true, "viewer")) }, { 1 }, { 600 })
            render(detail) {
                if (detail) detailEntry.Content("shop", 1, false, { it }, { it }, { _, _ -> null }, { effect ->
                    if (effect is ShopDetailEffect.BlockAuthor) requested += effect.author
                    else error("Unexpected effect $effect")
                })
                else listEntry.Content("shop", 1, { it }, {}, {}, {}, {}, { _, _, _ -> }, { _, _, _ -> }, requested::add)
            }.use {
                compose.waitUntil { compose.onAllNodesWithText("Coffee").fetchSemanticsNodes().isNotEmpty() }
                compose.onNode(verticalList).performScrollToNode(hasContentDescription("Действия с чекином"))
                compose.onNodeWithContentDescription("Действия с чекином").performClick()
                compose.waitUntil { compose.onAllNodesWithText("Заблокировать автора").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Заблокировать автора").performClick()
                compose.waitUntil { requested.size == 1 }
                assertEquals(listOf(ShopCheckInAuthor("public-id", "public-author", "Пользователь", null)), requested)
                assertEquals(1, reads)
            }
        }
    }
}
