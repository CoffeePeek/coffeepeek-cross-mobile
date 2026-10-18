package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingDisplay
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Self-targeted library UI; no application DI, account, images or HTTP. */
class CoffeePeekRatingContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun actualFractionalPixelsIncreaseWithoutRoundingToWholeStarsInEverySizeAndTheme() {
        for (dark in listOf(false, true)) for (size in CoffeePeekRatingSize.entries) {
            val value = mutableStateOf(4.0)
            var accent = Color.Unspecified
            ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
                scenario.onActivity { it.setContent {
                    CoffeePeekTheme(FontFamily.Default, darkTheme = dark) {
                        val color = MaterialTheme.colorScheme.primary
                        SideEffect { accent = color }
                        Surface { CoffeePeekRating(value.value, Modifier.testTag("rating"), size = size) }
                    }
                } }
                var previous = -1
                for (rating in listOf(4.0, 4.3, 4.7, 5.0)) {
                    compose.runOnIdle { value.value = rating }
                    val pixels = compose.onNodeWithTag("rating").captureToImage().toPixelMap()
                    var colored = 0
                    for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                        if (pixels[x, y].toArgb() == accent.toArgb()) colored++
                    }
                    assertTrue("Missing fractional fill: $dark $size $rating ($colored <= $previous)", colored > previous)
                    previous = colored
                }
            }
        }
    }

    @Test fun fractionalFillMirrorsWithinTheLastStarInRtl() {
        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
                scenario.onActivity { it.setContent {
                    CoffeePeekTheme(FontFamily.Default) {
                        CompositionLocalProvider(LocalLayoutDirection provides direction) {
                            Surface { CoffeePeekRating(4.3, Modifier.testTag("rating"), activeColor = Color.Red) }
                        }
                    }
                } }
                val pixels = compose.onNodeWithTag("rating").captureToImage().toPixelMap()
                val starWidth = pixels.height
                val start = if (direction == LayoutDirection.Ltr) pixels.width - starWidth else 0
                var colored = 0
                for (y in 0 until pixels.height) for (x in start until start + starWidth) {
                    if (pixels[x, y].toArgb() == Color.Red.toArgb()) {
                        colored++
                        val fractionX = (x - start).toFloat() / starWidth
                        assertTrue("Fill leaked beyond 30% in $direction", if (direction == LayoutDirection.Ltr)
                            fractionX <= 0.31f else fractionX >= 0.69f)
                    }
                }
                assertTrue("Last star has no partial fill in $direction", colored > 0)
            }
        }
    }

    @Test fun readOnlyRatingHasOneExactAccessibleValueAndNoClickTargets() {
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    CoffeePeekRating(13.0 / 3, Modifier.testTag("rating"), valueText = "4.3", contentDescription = "Taste 4.3 of 5")
                }
            } }
            compose.onNodeWithTag("rating").assertContentDescriptionEquals("Taste 4.3 of 5")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(4.3f, 0f..5f)))
                .assertHasNoClickAction()
            compose.onAllNodes(hasClickAction()).assertCountEquals(0)
            compose.onAllNodesWithContentDescription("Taste 4.3 of 5").assertCountEquals(1)
            compose.onNodeWithText("4.3").assertIsDisplayed()
        }
    }

    @Test fun inputEmitsWholeValuesIncludingRepeatedSelectionButNeverOwnsClearPolicy() {
        val rating = mutableStateOf(4.0)
        val actions = mutableListOf<Int>()
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = true) {
                    CoffeePeekRating(rating.value, size = CoffeePeekRatingSize.Large,
                        onRatingChange = { value -> actions += value; rating.value = value.toDouble() },
                        starDescription = { "Choose $it" })
                }
            } }
            compose.onNodeWithContentDescription("Choose 4").assertIsSelected()
            compose.onNodeWithContentDescription("Choose 5").performClick().assertIsSelected().performClick()
            compose.runOnIdle { assertEquals(listOf(5, 5), actions) }
        }
    }

    @Test fun disabledInputDoesNotDispatchAndFitsNarrowLargeFontWindow() {
        var actions = 0
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                        Box(Modifier.width(180.dp).testTag("container")) {
                            CoffeePeekRating(0.0, size = CoffeePeekRatingSize.Large, onRatingChange = { actions++ },
                                enabled = false, starDescription = { "Choose $it" })
                        }
                    }
                }
            } }
            val bounds = compose.onNodeWithTag("container").fetchSemanticsNode().boundsInRoot
            for (value in 1..5) {
                val star = compose.onNodeWithContentDescription("Choose $value")
                star.assertIsDisplayed().assertIsNotEnabled().performTouchInput { click() }
                val target = star.fetchSemanticsNode().boundsInRoot
                assertTrue(target.left >= bounds.left && target.right <= bounds.right)
            }
            compose.runOnIdle { assertEquals(0, actions) }
        }
    }

    @Test fun compactDisplayKeepsValueAccessibleAndUsesOneSymbolRatherThanFiveStars() {
        ActivityScenario.launch(DesignSystemTestActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    CoffeePeekRating(4.3, Modifier.testTag("rating"), display = CoffeePeekRatingDisplay.Compact,
                        contentDescription = "Rating 4.3 of 5")
                }
            } }
            compose.onNodeWithTag("rating").assertContentDescriptionEquals("Rating 4.3 of 5")
                .assertHasNoClickAction().assertIsDisplayed()
            compose.onNodeWithText("4.3").assertIsDisplayed()
        }
    }
}
