package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitch
import com.coffeepeek.core.designsystem.component.coffeePeekSwitchPalette
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SwitchContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun actualThumbPixelsAndEnabledTouchCallbacksFollowTheTheme() {
        for (dark in listOf(false, true)) for (checked in listOf(false, true)) for (enabled in listOf(false, true)) {
            var clicks = 0
            ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    CoffeePeekTheme(FontFamily.Default, darkTheme = dark) {
                        Surface {
                            CoffeePeekSwitch(checked, { clicks++ }, enabled = enabled, modifier = Modifier.testTag("switch"))
                        }
                    }
                } }
                val control = compose.onNodeWithTag("switch")
                control.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
                if (checked) control.assertIsOn() else control.assertIsOff()
                if (enabled) control.assertIsEnabled() else control.assertIsNotEnabled()
                val palette = coffeePeekSwitchPalette(dark)
                val thumb = (if (checked) palette.checkedThumb else palette.uncheckedThumb).toArgb()
                val pixels = control.captureToImage().toPixelMap()
                var thumbPixels = 0
                for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                    if (pixels[x, y].toArgb() == thumb) thumbPixels++
                }
                assertTrue("Full-size themed thumb missing: dark=$dark checked=$checked enabled=$enabled",
                    thumbPixels > pixels.width * pixels.height * 0.03f)
                control.performTouchInput { click() }
                compose.runOnIdle { assertEquals(if (enabled) 1 else 0, clicks) }
            }
        }
    }
}
