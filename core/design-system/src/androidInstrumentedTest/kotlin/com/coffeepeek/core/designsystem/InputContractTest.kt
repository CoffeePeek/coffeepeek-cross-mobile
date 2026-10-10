package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.AppTextField
import com.coffeepeek.core.designsystem.component.CompactOutlinedTextField
import com.coffeepeek.core.designsystem.component.CpSearchField
import com.coffeepeek.core.designsystem.component.CpTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InputContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(content: @Composable () -> Unit): ActivityScenario<DesignSystemTestActivity> =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) { content() }
            } }
        }

    @Test fun fieldEditsHoistedValueAndExposesError() {
        val value = mutableStateOf("")
        render {
            AppTextField("Name", value.value, { value.value = it }, "Enter name", errorText = "Required")
        }.use {
            compose.onNodeWithContentDescription("Name")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Required"))
                .performTextInput("Coffee")
            compose.runOnIdle { assertEquals("Coffee", value.value) }
        }
    }

    @Test fun compactFieldExposesLocalizedErrorSemanticsDirectly() {
        render {
            CompactOutlinedTextField(
                value = "",
                onValueChange = {},
                isError = true,
                errorDescription = "Required",
            )
        }.use {
            compose.onNode(hasSetTextAction())
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Required"))
        }
    }

    @Test fun disabledFieldExposesDisabledState() {
        render { AppTextField("Name", "", {}, "Enter name", enabled = false) }.use {
            compose.onNodeWithContentDescription("Name").assertIsNotEnabled()
        }
    }

    @Test fun passwordToggleUsesCallerOwnedStateAndLabels() {
        val visible = mutableStateOf(false)
        render {
            AppTextField("Password", "secret", {}, "Password", isPassword = true,
                passwordVisible = visible.value, onPasswordVisibilityChange = { visible.value = it },
                passwordToggleDescription = if (visible.value) "Hide password" else "Show password")
        }.use {
            compose.onNodeWithContentDescription("Show password").performClick()
            compose.onNodeWithContentDescription("Hide password").assertIsDisplayed()
            compose.runOnIdle { assertEquals(true, visible.value) }
        }
    }

    @Test fun searchEmitsImeAndClearActions() {
        val value = mutableStateOf("Coffee")
        var searches = 0
        render {
            CpSearchField(value.value, { value.value = it }, "Search", "Clear search",
                onSearch = { searches++ })
        }.use {
            compose.onNode(hasSetTextAction()).performImeAction()
            compose.onNodeWithContentDescription("Clear search").performClick()
            compose.runOnIdle {
                assertEquals(1, searches)
                assertEquals("", value.value)
            }
            compose.onNodeWithContentDescription("Clear search").assertDoesNotExist()
        }
    }

    @Test fun readOnlySearchDoesNotClear() {
        var changes = 0
        render {
            CpSearchField("Coffee", { changes++ }, "Search", "Clear search", readOnly = true)
        }.use {
            compose.onNodeWithContentDescription("Clear search").assertIsNotEnabled().performClick()
            compose.runOnIdle { assertEquals(0, changes) }
        }
    }

    @Test fun topBarOnlyEmitsExplicitBackCallback() {
        var backs = 0
        render { CpTopBar("Coffee", "Back", onBack = { backs++ }) }.use {
            compose.onNodeWithText("Coffee").assertIsDisplayed()
            compose.onNodeWithContentDescription("Back").performClick()
            compose.runOnIdle { assertEquals(1, backs) }
        }
    }

    @Test fun topBarWithoutCallbackHasNoBackButton() {
        render { CpTopBar("Coffee", "Back") }.use {
            compose.onNodeWithContentDescription("Back").assertDoesNotExist()
        }
    }
}
