package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Self-targeted test APK with fake content; no application, DI, account or transport. */
class ModalBottomSheetPlacementTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(content: @Composable () -> Unit) =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) { content() }
            } }
        }

    @Test fun boundedSheetIsBottomAnchoredAndCanScroll() {
        var bottomInset = 0
        render {
            CoffeePeekSwipeDismissModalBottomSheet({}, "Dismiss", maxSheetHeightFraction = 0.76f) {
                bottomInset = WindowInsets.safeDrawing.getBottom(LocalDensity.current)
                Column(Modifier.fillMaxWidth().fillMaxHeight().testTag("sheet-body")
                    .verticalScroll(rememberScrollState())) {
                    repeat(60) { Text("Row $it", Modifier.testTag("row-$it")) }
                }
            }
        }.use {
            assertBottomAnchored { bottomInset }
            compose.onNodeWithTag("row-59").performScrollTo().assertIsDisplayed()
            assertBottomAnchored { bottomInset }
        }
    }

    @Test fun unboundedSheetKeepsItsWrapContentBottomAnchor() {
        var bottomInset = 0
        render {
            CoffeePeekSwipeDismissModalBottomSheet({}, "Dismiss") {
                bottomInset = WindowInsets.safeDrawing.getBottom(LocalDensity.current)
                Text("Short content", Modifier.testTag("sheet-body"))
            }
        }.use { assertBottomAnchored { bottomInset } }
    }

    @Test fun loadingCompletionAndHandleDismissThenReopenKeepTheBottomAnchor() {
        val shown = mutableStateOf(true)
        val loading = mutableStateOf(true)
        var bottomInset = 0
        render {
            if (shown.value) CoffeePeekSwipeDismissModalBottomSheet(
                { shown.value = false }, "Dismiss", maxSheetHeightFraction = 0.76f,
            ) {
                bottomInset = WindowInsets.safeDrawing.getBottom(LocalDensity.current)
                Column(Modifier.fillMaxWidth().fillMaxHeight().testTag("sheet-body")
                    .verticalScroll(rememberScrollState())) {
                    if (loading.value) Text("Loading")
                    else repeat(60) { Text("Row $it") }
                }
            }
        }.use {
            assertBottomAnchored { bottomInset }
            compose.runOnIdle { loading.value = false }
            assertBottomAnchored { bottomInset }
            compose.onNodeWithContentDescription("Dismiss").performClick()
            compose.onNodeWithTag("sheet-body").assertDoesNotExist()
            compose.runOnIdle { shown.value = true }
            assertBottomAnchored { bottomInset }
        }
    }

    private fun assertBottomAnchored(bottomInset: () -> Int) {
        compose.waitForIdle()
        val dialog = compose.onNode(isDialog()).fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag("sheet-body").fetchSemanticsNode().boundsInRoot
        assertTrue("Sheet must leave space above, not below: $body / $dialog", body.top > dialog.height * 0.15f)
        assertEquals("Sheet body must meet the bottom system inset", dialog.bottom - bottomInset(), body.bottom, 2f)
    }
}
