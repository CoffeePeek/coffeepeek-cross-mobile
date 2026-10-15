package com.coffeepeek.feature.shopreport.impl

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import com.coffeepeek.feature.shopreport.impl.ui.compose.ShopReportScreenContent
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportAction
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShopReportSelectionTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(dark: Boolean = true, initial: ShopReportState = ShopReportState(),
        onAction: (ShopReportAction) -> Unit = {}): ActivityScenario<ShopReportTestActivity> =
        ActivityScenario.launch(ShopReportTestActivity::class.java).also { scenario ->
            var state by mutableStateOf(initial)
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(darkTheme = dark) {
                    ShopReportScreenContent(state, "Happy Coffee") { action ->
                        onAction(action)
                        state = when (action) {
                            is ShopReportAction.SelectCategory -> state.copy(selectedCategory = action.category)
                            is ShopReportAction.ChangeDescription -> state.copy(description = action.description)
                            else -> state
                        }
                    }
                }
            } }
        }

    @Test fun reasonsRemainOneAccessibleSingleChoiceGroupInBothThemes() {
        listOf(false, true).forEach { dark -> render(dark).use {
            compose.onAllNodes(isSelectable()).assertCountEquals(6)
            compose.onAllNodes(isSelected()).assertCountEquals(0)
            compose.onNodeWithText("Отправить").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithText("Кофейня закрылась").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithText("Указан неверный адрес").performScrollTo().performClick().assertIsSelected()
            compose.onAllNodes(isSelected()).assertCountEquals(1)
            compose.onNodeWithText("Отправить").performScrollTo().assertIsEnabled()
        } }
    }

    @Test fun rowsHaveOnlyDividerClearanceInsteadOfSeparateCardGaps() {
        render().use {
            val bounds = compose.onAllNodes(isSelectable()).fetchSemanticsNodes().map { it.boundsInRoot }.sortedBy { it.top }
            val maxDivider = with(compose.density) { 2.dp.toPx() }
            bounds.zipWithNext().forEach { (previous, next) ->
                assertTrue("Grouped rows must not have card-sized gaps", next.top - previous.bottom <= maxDivider)
            }
        }
    }

    @Test fun otherReasonKeepsDescriptionAndTypedSubmitIntent() {
        val actions = mutableListOf<ShopReportAction>()
        render(onAction = actions::add).use {
            compose.onNodeWithText("Другая проблема").performScrollTo().performClick()
            compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Описание")
            compose.onNodeWithText("Отправить").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(listOf(ShopReportAction.SelectCategory(ShopIssueCategory.Other),
                ShopReportAction.ChangeDescription("Описание"), ShopReportAction.Submit), actions) }
        }
    }

    @Test fun pendingReportDoesNotAllowChangingItsReason() {
        val actions = mutableListOf<ShopReportAction>()
        render(initial = ShopReportState(selectedCategory = ShopIssueCategory.ShopClosed, isSubmitting = true),
            onAction = actions::add).use {
            compose.onNodeWithText("Указан неверный адрес").performScrollTo().assertIsNotEnabled().performClick()
            compose.onNodeWithText("Кофейня закрылась").assertIsSelected()
            compose.runOnIdle { assertTrue(actions.isEmpty()) }
        }
    }
}
