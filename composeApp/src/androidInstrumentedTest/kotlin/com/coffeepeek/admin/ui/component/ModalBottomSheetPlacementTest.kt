package com.coffeepeek.admin.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coffeepeek.admin.theme.CoffeePeekTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Fake content only: no application screen, ViewModel, API, database or account writes. */
@RunWith(AndroidJUnit4::class)
class ModalBottomSheetPlacementTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun fractionalSheetIsBottomAnchoredAndLongContentCanScroll() {
        var bottomInset = 0
        compose.setContent {
            CoffeePeekTheme(darkTheme = true) {
                SwipeDismissModalBottomSheet({}, maxSheetHeightFraction = 0.76f) {
                    bottomInset = WindowInsets.safeDrawing.getBottom(LocalDensity.current)
                    Column(Modifier.fillMaxWidth().fillMaxHeight().testTag("sheet-body")
                        .verticalScroll(rememberScrollState())) {
                        repeat(60) { Text("Row $it", Modifier.testTag("row-$it")) }
                    }
                }
            }
        }
        assertBottomAnchored { bottomInset }
        compose.onNodeWithTag("row-59").performScrollTo().assertIsDisplayed()
        assertBottomAnchored { bottomInset }
    }

    @Test
    fun sheetWithoutHeightLimitStillWrapsShortContentAtTheBottom() {
        var bottomInset = 0
        compose.setContent {
            CoffeePeekTheme(darkTheme = false) {
                SwipeDismissModalBottomSheet({}) {
                    bottomInset = WindowInsets.safeDrawing.getBottom(LocalDensity.current)
                    Text("Short content", Modifier.testTag("sheet-body"))
                }
            }
        }
        assertBottomAnchored { bottomInset }
    }

    @Test
    fun loadingCompletionAndReopeningKeepTheSameBottomAnchor() {
        val shown = mutableStateOf(true)
        val loading = mutableStateOf(true)
        var bottomInset = 0
        compose.setContent {
            CoffeePeekTheme(darkTheme = false) {
                if (shown.value) SwipeDismissModalBottomSheet(
                    { shown.value = false }, maxSheetHeightFraction = 0.76f,
                ) {
                    bottomInset = WindowInsets.safeDrawing.getBottom(LocalDensity.current)
                    Column(Modifier.fillMaxWidth().fillMaxHeight().testTag("sheet-body")
                        .verticalScroll(rememberScrollState())) {
                        if (loading.value) Text("Loading")
                        else repeat(60) { Text("Row $it", Modifier.testTag("row-$it")) }
                    }
                }
            }
        }
        assertBottomAnchored { bottomInset }
        compose.runOnIdle { loading.value = false }
        assertBottomAnchored { bottomInset }
        compose.runOnIdle { shown.value = false }
        compose.onNodeWithTag("sheet-body").assertDoesNotExist()
        compose.runOnIdle { shown.value = true }
        assertBottomAnchored { bottomInset }
    }

    private fun assertBottomAnchored(bottomInset: () -> Int) {
        compose.waitForIdle()
        val dialog = compose.onNode(isDialog()).fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag("sheet-body").fetchSemanticsNode().boundsInRoot
        assertTrue("The fractional sheet must leave space above, not below: $body / $dialog",
            body.top > dialog.height * 0.15f)
        assertEquals("The sheet body must end just above the dialog's bottom system inset",
            dialog.bottom - bottomInset(), body.bottom, 2f)
    }
}
