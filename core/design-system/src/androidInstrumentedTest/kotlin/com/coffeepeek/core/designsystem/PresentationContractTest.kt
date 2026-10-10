package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ActivityScenario
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PresentationContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(content: @Composable () -> Unit): ActivityScenario<DesignSystemTestActivity> =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) { content() }
            } }
        }

    @Test fun segmentedControlEmitsSelectionWithoutOwningIt() {
        val selected = mutableStateOf("First")
        render {
            CapsuleSegmentedControl(listOf("First", "Second"), selected.value, { it }, { selected.value = it })
        }.use {
            compose.onNodeWithText("First").assertIsSelected()
            compose.onNodeWithText("Second").assertIsNotSelected().performClick().assertIsSelected()
            compose.runOnIdle { assertEquals("Second", selected.value) }
        }
    }

    @Test fun disabledSegmentsDoNotEmitSelection() {
        var changes = 0
        render {
            CapsuleSegmentedControl(listOf("First", "Second"), "First", { it }, { changes++ }, enabled = false)
        }.use {
            compose.onNodeWithText("Second").assertIsNotEnabled().performClick()
            compose.runOnIdle { assertEquals(0, changes) }
        }
    }

    @Test fun settingsRowEmitsClickAndBadgeIsDecorative() {
        var clicks = 0
        render { SettingsRow(CpIcons.Settings, "Settings", onClick = { clicks++ }) }.use {
            compose.onNodeWithText("Settings").performClick()
            compose.runOnIdle { assertEquals(1, clicks) }
        }
    }

    @Test fun disabledSettingsRowDoesNotEmitClick() {
        var clicks = 0
        render { SettingsRow(CpIcons.Settings, "Settings", onClick = { clicks++ }, enabled = false) }.use {
            compose.onNodeWithText("Settings").assertIsNotEnabled().performClick()
            compose.runOnIdle { assertEquals(0, clicks) }
        }
    }

    @Test fun loaderExposesLocalizedIndeterminateProgress() {
        render { CoffeePeekLoader("Fetching") }.use {
            compose.onNodeWithContentDescription("Fetching").assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate))
        }
    }

    @Test fun errorDialogEmitsDismissAndCallerHidesIt() {
        val shown = mutableStateOf(true)
        render { ErrorDialog(shown.value, "Try again", "Failed", "Dismiss", { shown.value = false }) }.use {
            compose.onNodeWithText("Try again").assertIsDisplayed()
            compose.onNodeWithText("Dismiss").performClick()
            compose.onNodeWithText("Failed").assertDoesNotExist()
        }
    }

    @Test fun loadingDialogBlocksBackAndCallerControlsVisibility() {
        val shown = mutableStateOf(true)
        render { LoadingDialog(shown.value, "Loading data", "Please wait") }.use {
            compose.onNodeWithContentDescription("Loading data").assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.onNodeWithText("Please wait").assertIsDisplayed()
            compose.runOnIdle { shown.value = false }
            compose.onNodeWithText("Please wait").assertDoesNotExist()
        }
    }
}
