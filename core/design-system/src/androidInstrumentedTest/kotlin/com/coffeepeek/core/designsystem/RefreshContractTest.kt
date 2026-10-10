package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RefreshContractTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun render(content: @Composable () -> Unit): ActivityScenario<DesignSystemTestActivity> =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) { content() }
            } }
        }

    @Composable private fun Sample(
        refreshing: Boolean = false, enabled: Boolean = true, firstItem: Int = 0, onRefresh: () -> Unit,
    ) {
        val list = rememberLazyListState(firstItem)
        CoffeePeekPullToRefresh(list, refreshing, onRefresh, "Refresh", "Refreshing",
            modifier = Modifier.fillMaxWidth().height(480.dp).testTag("Refresh container"),
            enabled = enabled, cooldown = 10.seconds) { scrollModifier ->
            LazyColumn(state = list, modifier = scrollModifier.fillMaxSize().testTag("List")) {
                items(40) { Text("Item $it", Modifier.fillMaxWidth().height(64.dp)) }
            }
        }
    }

    private fun pull(fraction: Float = 0.65f) {
        compose.onNodeWithTag("List").performTouchInput {
            swipe(Offset(center.x, height * 0.15f), Offset(center.x, height * (0.15f + fraction)), 1000)
        }
    }

    @Test fun downwardPullAtTopEmitsOneRefresh() {
        var calls = 0
        render { Sample(onRefresh = { calls++ }) }.use {
            pull()
            compose.runOnIdle { assertEquals(1, calls) }
            pull()
            compose.runOnIdle { assertEquals(1, calls) }
        }
    }
    @Test fun shortPullDoesNotRefresh() {
        var calls = 0
        render { Sample(onRefresh = { calls++ }) }.use {
            pull(0.1f)
            compose.runOnIdle { assertEquals(0, calls) }
        }
    }
    @Test fun awayFromTopScrollDoesNotRefresh() {
        var calls = 0
        render { Sample(firstItem = 20, onRefresh = { calls++ }) }.use {
            pull()
            compose.runOnIdle { assertEquals(0, calls) }
        }
    }
    @Test fun disabledPullHasNoActionOrCallback() {
        var calls = 0
        render { Sample(enabled = false, onRefresh = { calls++ }) }.use {
            compose.onNodeWithTag("Refresh container")
                .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
            pull()
            compose.runOnIdle { assertEquals(0, calls) }
        }
    }
    @Test fun refreshingStateBlocksRequestsAndCallerCanFinish() {
        val refreshing = mutableStateOf(true)
        var calls = 0
        render { Sample(refreshing = refreshing.value, onRefresh = { calls++ }) }.use {
            compose.onNodeWithContentDescription("Refreshing").assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate))
            pull()
            compose.runOnIdle { assertEquals(0, calls); refreshing.value = false }
            compose.onNodeWithContentDescription("Refreshing").assertDoesNotExist()
        }
    }
    @Test fun accessibleActionSharesCooldown() {
        var calls = 0
        render { Sample(onRefresh = { calls++ }) }.use {
            repeat(2) {
                val actions = compose.onNodeWithTag("Refresh container").fetchSemanticsNode()
                    .config[SemanticsActions.CustomActions]
                compose.runOnIdle { actions.single { it.label == "Refresh" }.action() }
            }
            compose.runOnIdle { assertEquals(1, calls) }
        }
    }
}
