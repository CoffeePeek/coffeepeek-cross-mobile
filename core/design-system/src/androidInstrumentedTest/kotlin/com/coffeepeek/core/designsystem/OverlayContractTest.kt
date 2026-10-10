package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ActivityScenario
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import android.view.View
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OverlayContractTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun render(content: @Composable () -> Unit): ActivityScenario<DesignSystemTestActivity> =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) { content() }
            } }
        }

    @Test fun fabActionsExposeLabelsAndEnabledCallbacks() {
        var clicks = 0
        render {
            FabMenu(listOf(FabMenuAction(CpIcons.Settings, "Settings", { clicks++ }),
                FabMenuAction(CpIcons.Close, "Unavailable", { clicks++ }, enabled = false)))
        }.use {
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithContentDescription("Unavailable").assertIsNotEnabled().performClick()
            compose.runOnIdle { assertEquals(1, clicks) }
        }
    }

    @Test fun sheetAccessibleDismissIsCallerOwned() {
        val shown = mutableStateOf(true)
        var dismissals = 0
        render {
            if (shown.value) SwipeDismissModalBottomSheet(
                { dismissals++; shown.value = false }, "Dismiss sheet",
            ) { Text("Sheet content") }
        }.use {
            compose.onNodeWithText("Sheet content").assertIsDisplayed()
            compose.onNodeWithContentDescription("Dismiss sheet").performClick()
            compose.onNodeWithText("Sheet content").assertDoesNotExist()
            compose.runOnIdle { assertEquals(1, dismissals) }
        }
    }

    @Test fun modalBackDispatcherRequestsSheetDismissal() {
        val shown = mutableStateOf(true)
        var sheetView: View? = null
        render {
            if (shown.value) SwipeDismissModalBottomSheet({ shown.value = false }, "Dismiss sheet") {
                sheetView = LocalView.current
                Text("Sheet content")
            }
        }.use {
            compose.onNodeWithText("Sheet content").assertIsDisplayed()
            compose.runOnIdle {
                checkNotNull(sheetView?.findViewTreeOnBackPressedDispatcherOwner())
                    .onBackPressedDispatcher.onBackPressed()
            }
            compose.waitUntil(timeoutMillis = 5000) {
                compose.onAllNodesWithText("Sheet content").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithText("Sheet content").assertDoesNotExist()
        }
    }

    @Test fun handleTouchDismissesExactlyOnce() {
        val shown = mutableStateOf(true)
        var dismissals = 0
        render {
            if (shown.value) SwipeDismissModalBottomSheet(
                { dismissals++; shown.value = false }, "Dismiss sheet",
            ) { Text("Sheet content") }
        }.use {
            compose.onNodeWithContentDescription("Dismiss sheet").performTouchInput { click() }
            compose.onNodeWithText("Sheet content").assertDoesNotExist()
            compose.runOnIdle { assertEquals(1, dismissals) }
        }
    }

    @Test fun longHandleDragDismissesSheet() {
        val shown = mutableStateOf(true)
        render {
            if (shown.value) SwipeDismissModalBottomSheet({ shown.value = false }, "Dismiss sheet") {
                Text("Sheet content")
            }
        }.use {
            compose.onNodeWithContentDescription("Dismiss sheet").performTouchInput {
                // Travel exceeds 72dp at any emulator density; end can lie outside the handle.
                swipe(Offset(center.x, 1f), Offset(center.x, height * 2.5f), durationMillis = 700)
            }
            compose.onNodeWithText("Sheet content").assertDoesNotExist()
        }
    }

    @Test fun shortSlowHandleDragRestoresSheet() {
        var dismissals = 0
        render { SwipeDismissModalBottomSheet({ dismissals++ }, "Dismiss sheet") { Text("Sheet content") } }.use {
            compose.onNodeWithContentDescription("Dismiss sheet").performTouchInput {
                swipe(center, center + Offset(0f, height * 0.2f), durationMillis = 700)
            }
            compose.onNodeWithText("Sheet content").assertIsDisplayed()
            compose.runOnIdle { assertEquals(0, dismissals) }
        }
    }

    @Test fun lockedSheetBlocksAccessibleDismissBackAndDragThenCanBeUnlocked() {
        val enabled = mutableStateOf(false)
        val shown = mutableStateOf(true)
        var dismissals = 0
        var sheetView: View? = null
        render {
            if (shown.value) SwipeDismissModalBottomSheet(
                { dismissals++; shown.value = false }, "Dismiss sheet", dismissEnabled = enabled.value,
            ) {
                sheetView = LocalView.current
                Text("Pending submission")
            }
        }.use {
            compose.onNodeWithContentDescription("Dismiss sheet")
                .assertIsNotEnabled()
                .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
                .performTouchInput {
                    click()
                    swipe(Offset(center.x, 1f), Offset(center.x, height * 2.5f), durationMillis = 700)
                }
            // The top of this tiny modal is outside its content: tapping the scrim
            // must not dismiss a write in progress either.
            compose.onNode(isDialog()).performTouchInput { click(Offset(center.x, 1f)) }
            compose.runOnIdle {
                checkNotNull(sheetView?.findViewTreeOnBackPressedDispatcherOwner())
                    .onBackPressedDispatcher.onBackPressed()
            }
            compose.onNodeWithText("Pending submission").assertIsDisplayed()
            compose.runOnIdle { assertEquals(0, dismissals); enabled.value = true }
            compose.onNodeWithContentDescription("Dismiss sheet").performClick()
            compose.onNodeWithText("Pending submission").assertDoesNotExist()
            compose.runOnIdle { assertEquals(1, dismissals) }
        }
    }
}
