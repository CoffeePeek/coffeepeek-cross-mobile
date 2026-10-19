package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CardBuilderContractTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun render(dark: Boolean, content: @Composable () -> Unit) =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { it.setContent { CoffeePeekTheme(FontFamily.Default, darkTheme = dark) { Surface { content() } } } }
        }

    @Test fun slotsKeepFullBleedEdgesAndOnlyPadTheBodyInBothThemes() {
        for (dark in listOf(false, true)) render(dark) {
            CoffeePeekCard(
                modifier = Modifier.width(240.dp).testTag("card"),
                header = { Box(Modifier.fillMaxWidth().height(40.dp).testTag("header")) },
                footer = { Box(Modifier.fillMaxWidth().height(30.dp).testTag("footer")) },
            ) { Box(Modifier.fillMaxWidth().height(20.dp).testTag("body")) }
        }.use {
            compose.onNodeWithTag("header").assertWidthIsEqualTo(240.dp)
            compose.onNodeWithTag("footer").assertWidthIsEqualTo(240.dp)
            compose.onNodeWithTag("body").assertWidthIsEqualTo(208.dp)
            compose.onNodeWithTag("card").assertHeightIsEqualTo(122.dp)
        }
    }

    @Test fun footerActionDoesNotTriggerCardClick() {
        var opens = 0
        var actions = 0
        render(false) {
            CoffeePeekCard(onClick = { opens++ }, footer = { TextButton({ actions++ }) { Text("Action") } }) {
                Text("Card body")
            }
        }.use {
            compose.onNodeWithText("Action").performClick()
            compose.runOnIdle { assertEquals(1, actions); assertEquals(0, opens) }
            compose.onNodeWithText("Card body").performClick()
            compose.runOnIdle { assertEquals(1, opens) }
        }
    }

    @Test fun readOnlyCardAndInfoItemExposeNoFabricatedAction() {
        render(true) {
            CoffeePeekCard(Modifier.testTag("card")) {
                CoffeePeekInfoItem("Medium", "Roast", Modifier.fillMaxWidth())
            }
        }.use {
            compose.onNodeWithTag("card").assertHasNoClickAction()
            compose.onAllNodes(hasClickAction()).assertCountEquals(0)
            compose.onNodeWithText("Medium").assertIsDisplayed()
            compose.onNodeWithText("Roast").assertIsDisplayed()
        }
    }

    @Test fun expandableTextHasControlledStateAndHidesActionForShortReplacement() {
        var expanded by mutableStateOf(false)
        var text by mutableStateOf("CoffeePeek description. ".repeat(25))
        render(false) {
            CoffeePeekExpandableText(text, expanded, { expanded = !expanded }, "Expand", "Collapse",
                Modifier.width(180.dp), collapsedLines = 1)
        }.use {
            compose.onNodeWithText("Expand").performClick()
            compose.onNodeWithText("Collapse").performClick()
            compose.onNodeWithText("Expand").assertIsDisplayed()
            compose.runOnIdle { text = "Short" }
            compose.onNodeWithText("Expand").assertDoesNotExist()
            compose.onNodeWithText("Short").assertIsDisplayed()
        }
    }
}
