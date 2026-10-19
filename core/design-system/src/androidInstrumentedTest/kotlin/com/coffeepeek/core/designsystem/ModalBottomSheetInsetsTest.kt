package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.component.CoffeePeekTextField
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real modal window; synthetic safe areas are separate from the real IME check. */
class ModalBottomSheetInsetsTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(dark: Boolean = false, direction: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit) = ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
        scenario.onActivity { activity -> activity.setContent {
            CoffeePeekTheme(FontFamily.Default, darkTheme = dark) {
                CompositionLocalProvider(LocalLayoutDirection provides direction, content = content)
            }
        } }
        compose.waitUntil(5_000) {
            compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
    }

    @Test fun safeAreaIsConsumedOnceIncludingAsymmetricHorizontalCutouts() {
        LayoutDirection.entries.forEach { direction ->
            val insets = WindowInsets(left = 24, top = 96, right = 52, bottom = 40)
            render(dark = direction == LayoutDirection.Rtl, direction = direction) {
                CoffeePeekSwipeDismissModalBottomSheet({}, "Dismiss", Modifier.testTag("surface"),
                    maxSheetHeightFraction = 1f, contentWindowInsets = { insets }) {
                    Box(Modifier.fillMaxSize().testTag("body").windowInsetsPadding(insets)) {
                        Box(Modifier.fillMaxSize().testTag("inner"))
                    }
                }
            }.use {
                val surface = bounds("surface")
                val body = bounds("body")
                val inner = bounds("inner")
                assertEquals(24f, body.left - surface.left, 2f)
                assertEquals(52f, surface.right - body.right, 2f)
                assertEquals(40f, surface.bottom - body.bottom, 2f)
                assertTrue("Handle/content must be below the top safe area", body.top - surface.top >= 96f)
                assertEquals("Child must not reapply consumed insets", body, inner)
            }
        }
    }

    @Test fun insetChangesRemeasureWithoutAccumulatingBottomPadding() {
        val inset = mutableStateOf(WindowInsets(left = 16, top = 80, right = 20, bottom = 24))
        render {
            CoffeePeekSwipeDismissModalBottomSheet({}, "Dismiss", Modifier.testTag("surface"),
                maxSheetHeightFraction = 1f, contentWindowInsets = { inset.value }) {
                Box(Modifier.fillMaxSize().testTag("body").navigationBarsPadding())
            }
        }.use {
            listOf(24, 100, 24).forEach { bottom ->
                compose.runOnIdle { inset.value = WindowInsets(left = 16, top = 80, right = 20, bottom = bottom) }
                val surface = bounds("surface")
                val body = bounds("body")
                assertEquals(bottom.toFloat(), surface.bottom - body.bottom, 2f)
                assertEquals(16f, body.left - surface.left, 2f)
                assertEquals(20f, surface.right - body.right, 2f)
            }
        }
    }

    @Test fun realKeyboardKeepsFocusedFieldAndBottomActionVisible() {
        val value = mutableStateOf("")
        var imeBottom = 0
        render(dark = true) {
            CoffeePeekSwipeDismissModalBottomSheet({}, "Dismiss", maxSheetHeightFraction = 1f) {
                imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
                Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        repeat(15) { Text("Fixture row $it", Modifier.padding(vertical = 8.dp)) }
                        CoffeePeekTextField("Note", value.value, { value.value = it }, "Local note",
                            modifier = Modifier.testTag("field"))
                    }
                    CoffeePeekButton("Done", {}, modifier = Modifier.fillMaxWidth().testTag("action"))
                }
            }
        }.use {
            compose.onNodeWithTag("field").performScrollTo().performClick()
            compose.waitUntil(10_000) { imeBottom > 0 }
            compose.onNode(hasSetTextAction()).performTextInput("Fixture")
            compose.onNode(hasSetTextAction()).assertIsFocused()
            compose.onNodeWithTag("field").assertIsDisplayed()
            compose.onNodeWithTag("action").assertIsDisplayed()
            val dialog = compose.onNode(isDialog()).fetchSemanticsNode().boundsInRoot
            val action = bounds("action")
            assertTrue("Action must remain above the keyboard: $action / $dialog, IME=$imeBottom",
                action.bottom <= dialog.bottom - imeBottom + 2f)
        }
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
}
