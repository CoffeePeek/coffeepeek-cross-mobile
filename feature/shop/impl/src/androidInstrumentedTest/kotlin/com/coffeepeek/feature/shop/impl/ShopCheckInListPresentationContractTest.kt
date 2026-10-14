package com.coffeepeek.feature.shop.impl

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.ui.compose.ShopCheckInListScreenContent
import com.coffeepeek.feature.shop.impl.ui.compose.component.previewCheckIn
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Fake-data UI scenarios, without application DI/network. */
class ShopCheckInListPresentationContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(dark: Boolean = false, content: @Composable () -> Unit): ActivityScenario<ShopFormTestActivity> =
        ActivityScenario.launch(ShopFormTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = dark) { content() }
            } }
        }

    @Test fun backIsAvailableBeforeTheFirstRequestCompletes() {
        val actions = mutableListOf<ShopCheckInListAction>()
        render { ShopCheckInListScreenContent(ShopCheckInListState(), actions::add) }.use {
            compose.onNodeWithContentDescription("Назад").performClick()
            assertEquals(listOf(ShopCheckInListAction.Back), actions)
        }
    }

    @Test fun refreshFailureKeepsAlreadyLoadedCardsAndOffersRetry() {
        val actions = mutableListOf<ShopCheckInListAction>()
        val state = ShopCheckInListState(isLoading = false, hasLoaded = true, hasLoadError = true,
            shopTitle = "Coffee", isLoggedIn = true,
            checkIns = listOf(previewCheckIn().copy(username = "Visible author", rating = null)))
        render(dark = true) { ShopCheckInListScreenContent(state, actions::add) }.use {
            compose.onNodeWithText("Повторить").performClick()
            assertEquals(listOf(ShopCheckInListAction.Retry), actions)
            compose.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(3)
            compose.onNodeWithText("Visible author").assertExists()
        }
    }

    @Test fun guestPromptOffersSignInAndRegistration() {
        val actions = mutableListOf<ShopCheckInListAction>()
        val state = ShopCheckInListState(isLoading = false, hasLoaded = true, shopTitle = "Coffee",
            checkIns = listOf(previewCheckIn().copy(username = "Visible author", rating = null)),
            hiddenCheckInCount = 2)
        render { ShopCheckInListScreenContent(state, actions::add) }.use {
            compose.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(3)
            compose.onNodeWithText("Войти").performClick()
            compose.onNodeWithText("Зарегистрироваться").performClick()
            assertEquals(listOf(ShopCheckInListAction.SignIn, ShopCheckInListAction.Register), actions)
        }
    }
}
