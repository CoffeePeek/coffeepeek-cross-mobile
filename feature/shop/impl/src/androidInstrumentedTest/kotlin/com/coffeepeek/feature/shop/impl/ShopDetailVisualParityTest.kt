package com.coffeepeek.feature.shop.impl

import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.feature.shop.impl.ui.compose.ShopDetailScreenContent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Geometry uses real brand typography and fake state; no app DI, photos, account or HTTP. */
class ShopDetailVisualParityTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(dark: Boolean, forCheckIn: Boolean = false,
        onAction: (ShopDetailAction) -> Unit = {}): ActivityScenario<ShopFormTestActivity> =
        ActivityScenario.launch(ShopFormTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity ->
                activity.enableEdgeToEdge()
                activity.setContent {
                    CoffeePeekTheme(darkTheme = dark) {
                        Box(Modifier.fillMaxSize()) {
                            ShopDetailScreenContent(shopDetailParityState(),
                                onAction, distanceLabel = "248 м", forCheckIn = forCheckIn)
                        }
                    }
                }
            }
        }

    @Test fun brandPlaceholderKeepsFullHeroHeightInBothThemes() {
        listOf(false, true).forEach { dark -> render(dark).use {
            compose.onNodeWithContentDescription("Фото кофейни отсутствует").assertIsDisplayed()
            compose.onNodeWithContentDescription("Фото кофейни отсутствует").assertHeightIsEqualTo(300.dp)
        } }
    }

    @Test fun titleUsesProductionMetricsWhileBadgesKeepApprovedLargerText() {
        render(dark = true).use {
            assertTextSize("Happy Coffee", 28)
            assertTextSize("248 м", 14)
            assertTextSize("От вас", 12)
            assertTextSize("0 чекинов", 12)
            assertTextSize("Кофейня", 12)
        }
    }

    @Test fun allInformationCardIconsKeepBrandAccentInBothThemes() {
        listOf(false, true).forEach { dark -> render(dark).use {
            listOf("Нет оценок", "248 м", "8 BYN").forEach { title ->
                val pixels = compose.onNodeWithText(title).captureToImage().toPixelMap()
                val hasAccent = (0 until pixels.width).any { x ->
                    (0 until pixels.height).any { y -> pixels[x, y].toArgb() == CoffeePeekColors.Primary.toArgb() }
                }
                assertTrue("$title icon must keep the brand accent in both themes", hasAccent)
            }
        } }
    }

    @Test fun scrollContentContinuesBehindFloatingActionsAndCallbacksStaySeparate() {
        val actions = mutableListOf<ShopDetailAction>()
        render(dark = true, onAction = actions::add).use {
            val list = compose.onAllNodes(hasScrollToIndexAction())[0].fetchSemanticsNode().boundsInRoot
            val button = compose.onNodeWithText("Чекин").fetchSemanticsNode().boundsInRoot
            assertTrue("Content must extend behind, not stop above, the floating actions", list.bottom > button.bottom)
            compose.onNodeWithContentDescription("Маршрут").performClick()
            compose.onNodeWithText("Чекин").performClick()
            compose.runOnIdle { assertEquals(listOf(ShopDetailAction.OpenRoute, ShopDetailAction.OpenCheckIn), actions) }
        }
    }

    @Test fun pickerKeepsItsTwoActionsWithoutReplacingThemWithRoute() {
        val actions = mutableListOf<ShopDetailAction>()
        render(dark = false, forCheckIn = true, onAction = actions::add).use {
            compose.onNodeWithContentDescription("Маршрут").assertDoesNotExist()
            compose.onNodeWithText("Выбрать другую").performClick()
            compose.onNodeWithText("Продолжить").performClick()
            compose.runOnIdle { assertEquals(listOf(ShopDetailAction.ChooseAnotherShop, ShopDetailAction.OpenCheckIn), actions) }
        }
    }

    private fun assertTextSize(text: String, size: Int) {
        val results = mutableListOf<TextLayoutResult>()
        // Read the actual text, not the card's merged accessibility node (whose first layout is the value).
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(size.sp, results.single().layoutInput.style.fontSize)
    }
}
