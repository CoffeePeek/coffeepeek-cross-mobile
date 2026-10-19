package com.coffeepeek.core.designsystem

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekBynIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekDrinkLabel
import com.coffeepeek.core.designsystem.component.CoffeePeekCheckmarkRow
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class DesignSystemTestActivity : ComponentActivity()

class ComponentContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun currencyGlyphNextToAnAmountKeepsItsSquareSize() {
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    CoffeePeekBynIcon(Modifier.size(16.dp).testTag("currency"))
                }
            } }
            compose.onNodeWithTag("currency").assertWidthIsEqualTo(16.dp).assertHeightIsEqualTo(16.dp)
                .assertHasNoClickAction()
        }
    }

    @Test fun savedDrinkLabelHasNoFictionalNavigationAction() {
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme(FontFamily.Default) { CoffeePeekDrinkLabel("Cappuccino") }
            } }
            compose.onNodeWithText("Cappuccino").assertIsDisplayed().assertHasNoClickAction()
        }
    }

    @Test fun buttonRendersAndInvokesCallback() {
        var clicks = 0
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = false) {
                    CoffeePeekButton("Continue", onClick = { clicks++ })
                }
            } }
            compose.onNodeWithText("Continue").assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(1, clicks) }
        }
    }

    @Test fun disabledButtonDoesNotInvokeCallback() {
        var clicks = 0
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = true) {
                    CoffeePeekButton("Disabled", onClick = { clicks++ }, enabled = false)
                }
            } }
            compose.onNodeWithText("Disabled").assertIsNotEnabled().performClick()
            compose.runOnIdle { assertEquals(0, clicks) }
        }
    }

    @Test fun checkmarkRowExposesSelectionAndCallback() {
        var toggles = 0
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    CoffeePeekCheckmarkRow("Option", checked = false, onToggle = { toggles++ })
                }
            } }
            compose.onNodeWithText("Option").assertIsOff().performClick()
            compose.runOnIdle { assertEquals(1, toggles) }
        }
    }

    @Test fun glassFallbackButtonHasAccessibleLabelAndCallback() {
        var clicks = 0
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    CoffeePeekGlassIconButton(onClick = { clicks++ }, contentDescription = "Close", hazeState = null) {
                        Icon(CoffeePeekIcons.Close, contentDescription = null)
                    }
                }
            } }
            compose.onNodeWithContentDescription("Close").assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(1, clicks) }
        }
    }
}
