package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButtonStyle
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Self-targeted library host: no production Application, account, DI or network. */
class CoffeePeekLikeContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun defaultPhotoGlassIsTranslucentAndPlainStyleHasNoPlate() {
        for (dark in listOf(false, true)) {
            val style = mutableStateOf(CoffeePeekLikeButtonStyle.Glass)
            render(dark) {
                Box(Modifier.background(Color.White)) {
                    CoffeePeekLikeButton(true, "Saved", {}, Modifier.size(48.dp).testTag("glass"),
                        overImage = true, style = style.value)
                }
            }.use {
                val pixels = compose.onNodeWithTag("glass").captureToImage().toPixelMap()
                val background = pixels[pixels.width / 2, (pixels.height * 0.22f).toInt()]
                assertTrue("Glass must show the image, not solid black", background.red in 0.35f..0.45f)
                assertTrue(colorPixelCount("glass", CoffeePeekColors.Like) > 0)
                compose.runOnIdle { style.value = CoffeePeekLikeButtonStyle.Plain }
                val plain = compose.onNodeWithTag("glass").captureToImage().toPixelMap()
                assertEquals(Color.White.toArgb(), plain[plain.width / 2, (plain.height * 0.22f).toInt()].toArgb())
            }
        }
    }

    @Test fun photoBadgeIsReadOnlyNotADisabledToggle() {
        render(false) { CoffeePeekLikeBadge(true, "Saved") }.use {
            compose.onNodeWithContentDescription("Saved").assertHasNoClickAction()
            compose.onAllNodes(hasClickAction()).assertCountEquals(0)
        }
    }

    @Test fun selectedHeartIsGoldAndUnselectedOutlineIsNeutralInBothThemes() {
        for (dark in listOf(false, true)) {
            val selected = mutableStateOf(false)
            render(dark) { CoffeePeekLikeIcon(selected.value, null, Modifier.testTag("heart")) }.use {
                val gold = if (dark) CoffeePeekColors.Like else CoffeePeekColors.LikeOnLight
                assertEquals(0, colorPixelCount("heart", gold))
                assertTrue("Outline must stay neutral in $dark", colorPixelCount("heart",
                    if (dark) CoffeePeekColors.DarkTextSecondary else CoffeePeekColors.LightTextSecondary) > 0)
                compose.runOnIdle { selected.value = true }
                assertTrue("Selected heart must use gold in $dark", colorPixelCount("heart", gold) > 0)
            }
        }
    }

    @Test fun photoButtonPreservesGoldAndReadableNeutralOutline() {
        val selected = mutableStateOf(true)
        for (dark in listOf(false, true)) render(dark) {
            CoffeePeekLikeButton(selected.value, "Remove", {}, Modifier.testTag("photo"), containerColor = Color.Black, overImage = true)
        }.use {
            compose.runOnIdle { selected.value = true }
            compose.onNodeWithContentDescription("Remove").assertIsOn().assertIsEnabled()
            assertTrue(colorPixelCount("photo", CoffeePeekColors.Like) > 0)
            compose.runOnIdle { selected.value = false }
            compose.onNodeWithContentDescription("Remove").assertIsOff()
            assertTrue(colorPixelCount("photo", Color.White) > 0)
        }
    }

    @Test fun callerOwnsSelectionAndOneClickEmitsOneAction() {
        val selected = mutableStateOf(false)
        var clicks = 0
        render(false) { CoffeePeekLikeButton(selected.value, "Favorite", { clicks++ }) }.use {
            compose.onNodeWithContentDescription("Favorite").assertIsOff().performClick().assertIsOff()
            compose.runOnIdle { assertEquals(1, clicks); selected.value = true }
            compose.onNodeWithContentDescription("Favorite").assertIsOn().performClick()
            compose.runOnIdle { assertEquals(2, clicks) }
        }
    }

    @Test fun disabledToggleKeepsSelectionButNeverDispatches() {
        for (selected in listOf(false, true)) {
            var clicks = 0
            render(true) { CoffeePeekLikeButton(selected, "Disabled", { clicks++ }, enabled = false) }.use {
                val button = compose.onNodeWithContentDescription("Disabled").assertIsNotEnabled()
                if (selected) button.assertIsOn() else button.assertIsOff()
                button.performTouchInput { click() }
                compose.runOnIdle { assertEquals(0, clicks) }
            }
        }
    }

    @Test fun disabledHeartIsVisuallyMutedInsteadOfLookingEnabled() {
        val enabled = mutableStateOf(true)
        render(false) { CoffeePeekLikeIcon(true, null, Modifier.testTag("heart"), enabled = enabled.value) }.use {
            assertTrue(colorPixelCount("heart", CoffeePeekColors.LikeOnLight) > 0)
            compose.runOnIdle { enabled.value = false }
            assertEquals(0, colorPixelCount("heart", CoffeePeekColors.LikeOnLight))
        }
    }

    @Test fun nestedToggleDoesNotOpenParentAndHasOneLabelAndTouchTarget() {
        var parentClicks = 0
        var likeClicks = 0
        render(false) {
            Row(Modifier.clickable { parentClicks++ }) {
                Text("Card")
                CoffeePeekLikeButton(true, "Remove", { likeClicks++ }, Modifier.size(48.dp))
            }
        }.use {
            compose.onAllNodesWithContentDescription("Remove").assertCountEquals(1)
            compose.onNodeWithContentDescription("Remove").assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp).performClick()
            compose.runOnIdle { assertEquals(1, likeClicks); assertEquals(0, parentClicks) }
        }
    }

    @Test fun readOnlyCountRemainsTextAndDoesNotBecomeAnAction() {
        render(true) {
            Row(Modifier.semantics(mergeDescendants = true) { contentDescription = "Likes: 3" }) {
                CoffeePeekLikeIcon(true, null, size = 18.dp)
                Text("3")
            }
        }.use {
            compose.onNodeWithContentDescription("Likes: 3").assertHasNoClickAction()
            compose.onNodeWithText("3").assertIsDisplayed()
            compose.onAllNodes(hasClickAction()).assertCountEquals(0)
        }
    }

    @Test fun compactPlateDoesNotGrowWithTheTouchTargetOrDrawAPressRipple() {
        var expectedPlatePixels = 0f
        render(false) {
            expectedPlatePixels = with(LocalDensity.current) { 32.dp.toPx() }
            Box(Modifier.background(Color.White)) {
                CoffeePeekLikeButton(false, "Favorite", {}, Modifier.size(64.dp).testTag("compact"),
                    overImage = true)
            }
        }.use {
            val node = compose.onNodeWithTag("compact")
            val before = node.captureToImage().toPixelMap()
            val changedColumns = (0 until before.width).filter { x ->
                (0 until before.height).any { y -> before[x, y].toArgb() != Color.White.toArgb() }
            }
            assertEquals("The 64dp target must still draw a 32dp plate", expectedPlatePixels,
                (changedColumns.last() - changedColumns.first() + 1).toFloat(), 2f)
            node.performTouchInput { down(center) }
            val pressed = node.captureToImage().toPixelMap()
            for (y in 0 until before.height) for (x in 0 until before.width) {
                assertEquals("Press must not draw a ripple at $x,$y", before[x, y].toArgb(), pressed[x, y].toArgb())
            }
            node.performTouchInput { up() }
        }
    }

    @Test fun loadingToggleIsDisabledAndExposesOneProgressLabel() {
        render(false) { CoffeePeekLikeButton(true, "Remove", { error("No loading writes") },
            isLoading = true, loadingDescription = "Updating") }.use {
            compose.onAllNodesWithContentDescription("Updating").assertCountEquals(1)
            compose.onNodeWithContentDescription("Updating").assertIsOn().assertIsNotEnabled().performClick()
        }
    }

    private fun render(dark: Boolean, content: @androidx.compose.runtime.Composable () -> Unit): ActivityScenario<DesignSystemTestActivity> =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { it.setContent { CoffeePeekTheme(FontFamily.Default, darkTheme = dark) { Surface(content = content) } } }
            compose.waitUntil(5_000) {
                compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
            }
        }

    private fun colorPixelCount(tag: String, color: Color): Int {
        val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
        var count = 0
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            if (pixels[x, y].toArgb() == color.toArgb()) count++
        }
        return count
    }
}
