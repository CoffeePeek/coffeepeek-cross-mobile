package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeIcon
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Self-targeted library host: no production Application, account, DI or network. */
class CoffeePeekLikeContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun selectedHeartIsGoldAndUnselectedOutlineIsNeutralInBothThemes() {
        for (dark in listOf(false, true)) {
            val selected = mutableStateOf(false)
            render(dark) { CoffeePeekLikeIcon(selected.value, null, Modifier.testTag("heart")) }.use {
                val gold = if (dark) CoffeePeekColors.Like else CoffeePeekColors.LikeOnLight
                assertEquals(0, colorPixelCount("heart", gold))
                assertTrue("Outline must stay neutral in $dark", colorPixelCount("heart",
                    if (dark) CoffeePeekColors.DarkTextPrimary else CoffeePeekColors.LightTextPrimary) > 0)
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

    private fun render(dark: Boolean, content: @androidx.compose.runtime.Composable () -> Unit): ActivityScenario<DesignSystemTestActivity> =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { it.setContent { CoffeePeekTheme(FontFamily.Default, darkTheme = dark) { Surface(content = content) } } }
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
