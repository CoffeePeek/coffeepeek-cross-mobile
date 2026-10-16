package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.CoffeePeekFormStep
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThemeOwnershipContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun glassIconsUseTheCurrentSurfaceThemeRatherThanAnUnrelatedParentTint() {
        for (dark in listOf(false, true)) {
            var actual: Color? = null
            var expected: Color? = null
            ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    CoffeePeekTheme(FontFamily.Default, darkTheme = dark) {
                        val themed = MaterialTheme.colorScheme.onSurface
                        CompositionLocalProvider(LocalContentColor provides Color.Magenta) {
                            CoffeePeekGlassIconButton({}, "Back") {
                                val inherited = LocalContentColor.current
                                SideEffect { actual = inherited; expected = themed }
                                Icon(CoffeePeekIcons.Back, null)
                            }
                        }
                    }
                } }
                compose.runOnIdle { assertEquals(checkNotNull(expected), checkNotNull(actual)) }
            }
        }
    }

    @Test fun optionalRailUsesItsCallersLocalizedAccessibilityDescription() {
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    CoffeePeekFormStep(optional = true, last = true, optionalDescription = "Необязательный пункт") {
                        Text("Fixture field")
                    }
                }
            } }
            compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Необязательный пункт"))
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
        }
    }
}
