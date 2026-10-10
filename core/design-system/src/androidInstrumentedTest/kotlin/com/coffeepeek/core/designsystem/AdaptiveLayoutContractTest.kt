package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AdaptiveLayoutContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    // Deterministic logical 320.dp viewport; real OS nonlinear scaling needs integration QA.
    private fun render(direction: LayoutDirection, scale: Float, dark: Boolean,
        content: @Composable () -> Unit): ActivityScenario<DesignSystemTestActivity> =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CompositionLocalProvider(LocalLayoutDirection provides direction,
                    LocalDensity provides Density(1f, fontScale = scale)) {
                    CoffeePeekTheme(darkTheme = dark) {
                        Column(Modifier.width(320.dp)) { content() }
                    }
                }
            } }
        }

    private fun textLayout(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val result = results.single()
        // Simple String Text reconstructs its semantic MultiParagraph with the original
        // parent maxWidth, not the measured compact width (ParagraphLayoutCache).
        // Re-layout with the actual width before asserting fit/ellipsis, also in RTL.
        return TextLayoutResult(result.layoutInput,
            MultiParagraph(result.multiParagraph.intrinsics,
                constraints = Constraints(maxWidth = result.size.width),
                maxLines = result.layoutInput.maxLines, overflow = result.layoutInput.overflow),
            result.size)
    }

    private fun assertFullText(text: String) {
        compose.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
        val layout = textLayout(text)
        assertFalse("Text overflow: $text, size=${layout.size}, lines=${layout.lineCount}, " +
            "paragraph=${layout.multiParagraph.width} x ${layout.multiParagraph.height}, " +
            "width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}", layout.hasVisualOverflow)
    }

    @Test fun groupedActionsHaveMinimumTargetsAndPreserveCheckboxState() {
        LayoutDirection.entries.forEach { direction ->
            val checked = mutableStateOf(false)
            var clicks = 0
            render(direction, 1f, dark = direction == LayoutDirection.Rtl) {
                CheckmarkRow("Option", checked.value, { checked.value = !checked.value })
                ActionRow("Action", { clicks++ })
            }.use {
                compose.onNodeWithText("Option").assertHeightIsAtLeast(48.dp)
                    .assertIsOff().performClick().assertIsOn()
                compose.onNodeWithText("Action").assertHeightIsAtLeast(48.dp)
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performClick()
                compose.runOnIdle { assertEquals(1, clicks) }
            }
        }
    }

    @Test fun stepperTargetsGrowAndActionsRemainLogicalInBothDirections() {
        LayoutDirection.entries.forEach { direction ->
            listOf(1f, 2f).forEach { scale ->
                var decreases = 0
                var increases = 0
                render(direction, scale, dark = direction == LayoutDirection.Rtl) {
                    StepperRow("Количество порций", "12", { decreases++ }, { increases++ },
                        "Decrease", "Increase")
                }.use {
                    val decrease = compose.onNodeWithContentDescription("Decrease")
                    val increase = compose.onNodeWithContentDescription("Increase")
                    listOf(decrease, increase).forEach {
                        it.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
                            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                    }
                    val left = decrease.fetchSemanticsNode().boundsInRoot
                    val right = increase.fetchSemanticsNode().boundsInRoot
                    if (direction == LayoutDirection.Ltr) assertTrue(left.right <= right.left)
                    else assertTrue(right.right <= left.left)
                    listOf("Количество порций", "12", "−", "+").forEach(::assertFullText)
                    decrease.performClick()
                    increase.performClick()
                    compose.runOnIdle { assertEquals(1, decreases); assertEquals(1, increases) }
                }
            }
        }
    }

    @Test fun multilineBrandButtonGrowsAtLargeScaleInBothDirectionsAndThemes() {
        val label = "Продолжить настройку приложения"
        LayoutDirection.entries.forEach { direction ->
            listOf(false, true).forEach { dark ->
                var clicks = 0
                render(direction, 2f, dark) { AppButton(label, { clicks++ }) }.use {
                    assertFullText(label)
                    assertTrue(textLayout(label).lineCount > 1)
                    compose.onNodeWithText(label).assertHeightIsAtLeast(48.dp).performClick()
                    compose.runOnIdle { assertEquals(1, clicks) }
                }
            }
        }
    }

    @Test fun longSwitchLabelWrapsWithoutLosingSelectionOrCallback() {
        val label = "Получать уведомления об изменениях"
        LayoutDirection.entries.forEach { direction ->
            val checked = mutableStateOf(false)
            render(direction, 2f, dark = direction == LayoutDirection.Rtl) {
                SwitchRow(label, checked.value, { checked.value = it })
            }.use {
                assertFullText(label)
                assertTrue(textLayout(label).lineCount > 1)
                compose.onNodeWithText(label).assertIsOff().performClick().assertIsOn()
            }
        }
    }

    @Test fun settingsKeepsFullSemanticLabelAndLogicalTrailingSlotWhenEllipsized() {
        val label = "Настройки оформления приложения"
        LayoutDirection.entries.forEach { direction ->
            var clicks = 0
            render(direction, 2f, dark = direction == LayoutDirection.Rtl) {
                SettingsRow(CpIcons.Settings, label, description = "Системная тема", onClick = { clicks++ },
                    trailing = { androidx.compose.material3.Text("End", Modifier.testTag("trailing")) })
            }.use {
                compose.onNodeWithText(label).assertIsDisplayed()
                val layout = textLayout(label)
                assertEquals(1, layout.lineCount)
                assertTrue(layout.isLineEllipsized(0))
                val text = compose.onNodeWithText(label, useUnmergedTree = true)
                    .fetchSemanticsNode().boundsInRoot
                val trailing = compose.onNodeWithTag("trailing", useUnmergedTree = true)
                    .fetchSemanticsNode().boundsInRoot
                if (direction == LayoutDirection.Ltr) assertTrue(text.right <= trailing.left)
                else assertTrue(trailing.right <= text.left)
                compose.onNodeWithText(label).performClick()
                compose.runOnIdle { assertEquals(1, clicks) }
            }
        }
    }

    @Test fun segmentsKeepOrderSelectionAndTargetsAtLargeScaleInRtl() {
        LayoutDirection.entries.forEach { direction ->
            val selected = mutableStateOf("First")
            render(direction, 2f, dark = direction == LayoutDirection.Rtl) {
                CapsuleSegmentedControl(listOf("First", "Second"), selected.value, { it }, { selected.value = it })
            }.use {
                val first = compose.onNodeWithText("First")
                val second = compose.onNodeWithText("Second")
                first.assertIsSelected().assertHeightIsAtLeast(48.dp)
                second.assertIsNotSelected().assertHeightIsAtLeast(48.dp)
                val firstBounds = first.fetchSemanticsNode().boundsInRoot
                val secondBounds = second.fetchSemanticsNode().boundsInRoot
                if (direction == LayoutDirection.Ltr) assertTrue(firstBounds.right <= secondBounds.left)
                else assertTrue(secondBounds.right <= firstBounds.left)
                second.performClick().assertIsSelected()
                first.assertIsNotSelected()
            }
        }
    }
}
