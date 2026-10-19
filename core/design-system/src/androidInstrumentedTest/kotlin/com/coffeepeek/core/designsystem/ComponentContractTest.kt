package com.coffeepeek.core.designsystem

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertHeightIsEqualTo
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
import com.coffeepeek.core.designsystem.component.CoffeePeekGroupSection
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.component.CoffeePeekCheckmarkRow
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class DesignSystemTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // Foreground test window only; never change device settings or dismiss the keyguard.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

class ComponentContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun optionalGroupHeaderDoesNotInsertAnEmptyTitleGap() {
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    Box(Modifier.testTag("group")) {
                        CoffeePeekGroupSection { Box(Modifier.size(20.dp)) }
                    }
                }
            } }
            compose.onNodeWithTag("group").assertHeightIsEqualTo(20.dp)
        }
    }

    @Test fun outlinedContentCardKeepsCallerPaddingWithoutExtraInsets() {
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = true) {
                    CoffeePeekContentCard(Modifier.testTag("card"), contentPadding = PaddingValues(16.dp)) {
                        Box(Modifier.size(20.dp))
                    }
                }
            } }
            compose.onNodeWithTag("card").assertHeightIsEqualTo(52.dp)
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
