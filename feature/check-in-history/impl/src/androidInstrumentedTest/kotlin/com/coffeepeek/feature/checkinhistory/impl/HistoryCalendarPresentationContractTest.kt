package com.coffeepeek.feature.checkinhistory.impl

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryCalendarPhoto
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.HistoryCalendarCard
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.HistoryCalendarDay
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.previewCombinedHistoryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Production calendar geometry and bounded press feedback; no application DI or image requests. */
class HistoryCalendarPresentationContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(dark: Boolean, content: @Composable () -> Unit): ActivityScenario<HistoryTestActivity> =
        ActivityScenario.launch(HistoryTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(darkTheme = dark) {
                    Box(Modifier.size(96.dp).background(MaterialTheme.colorScheme.surface).testTag("calendar"),
                        contentAlignment = Alignment.Center) { content() }
                }
            } }
        }

    @Test fun selectedDayIsCompactAndFilledAcrossTheRoundedRectangleInBothThemes() {
        for (dark in listOf(false, true)) {
            var primary = androidx.compose.ui.graphics.Color.Unspecified
            var surface = androidx.compose.ui.graphics.Color.Unspecified
            render(dark) {
                primary = MaterialTheme.colorScheme.primary
                surface = MaterialTheme.colorScheme.surface
                HistoryCalendarDay(14, "2026-10-14", 0, 0, isSelected = true, isToday = true, isFuture = false,
                    onSelect = {}, modifier = Modifier.width(56.dp).testTag("day"))
            }.use {
                val day = compose.onNodeWithTag("day").assertIsSelected()
                    // Production keeps a 1 dp gutter on each side of the 56 dp slot.
                    .assertWidthIsEqualTo(54.dp).assertHeightIsEqualTo(54.dp)
                val pixels = day.captureToImage().toPixelMap()
                assertEquals(primary, pixels[pixels.width / 2, pixels.height / 6])
                assertEquals(surface, pixels[1, 1])
            }
        }
    }

    @Test fun photoUsesTheSameSquareCellAndFutureDatesCannotDispatchSelection() {
        for (dark in listOf(false, true)) {
            val requested = java.util.concurrent.CopyOnWriteArrayList<String>()
            var selections = 0
            render(dark) {
                HistoryCalendarDay(15, "2026-10-15", 2, 1, isSelected = false, isToday = false, isFuture = true,
                    onSelect = { selections++ }, modifier = Modifier.width(56.dp).testTag("day"),
                    photos = listOf(HistoryCalendarPhoto("first", true), HistoryCalendarPhoto("second", false)),
                    imageContent = { url, modifier, scale ->
                        assertEquals(androidx.compose.ui.layout.ContentScale.Fit, scale)
                        requested += url
                        Box(modifier)
                    })
            }.use {
                compose.onNodeWithTag("day").assertWidthIsEqualTo(54.dp).assertHeightIsEqualTo(54.dp)
                    .assertIsNotEnabled().performClick()
                compose.runOnIdle {
                    assertEquals(setOf("first"), requested.toSet())
                    assertEquals(0, selections)
                }
            }
        }
    }

    @Test fun rippleIsVisibleButDoesNotEscapeTheRoundedCellInEitherTheme() {
        for (dark in listOf(false, true)) {
            render(dark) {
                HistoryCalendarDay(14, "2026-10-14", 0, 0, isSelected = false, isToday = false, isFuture = false,
                    onSelect = {}, modifier = Modifier.width(56.dp).testTag("day"))
            }.use {
                val day = compose.onNodeWithTag("day")
                val parent = compose.onNodeWithTag("calendar")
                val before = parent.captureToImage().toPixelMap()
                val parentBounds = parent.fetchSemanticsNode().boundsInRoot
                val bounds = day.fetchSemanticsNode().boundsInRoot.translate(-parentBounds.left, -parentBounds.top)
                compose.mainClock.autoAdvance = false
                try {
                    day.performTouchInput { down(center) }
                    compose.mainClock.advanceTimeBy(200)
                    val pressed = parent.captureToImage().toPixelMap()
                    var changes = 0
                    for (x in 0 until before.width) for (y in 0 until before.height) {
                        if (before[x, y] != pressed[x, y]) {
                            changes++
                            assertTrue("Ripple escaped its day cell", bounds.contains(androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat())))
                        }
                    }
                    assertTrue("A pressed day must retain visible ripple feedback", changes > 0)
                    val cornerX = bounds.left.toInt() + 2
                    val cornerY = bounds.top.toInt() + 2
                    assertEquals("Ripple must respect rounded corners", before[cornerX, cornerY], pressed[cornerX, cornerY])
                } finally {
                    day.performTouchInput { cancel() }
                    compose.mainClock.autoAdvance = true
                    compose.waitForIdle()
                }
            }
        }
    }

    @Test fun calendarRowsKeepTheSameCompactSpacingAsColumnsInBothThemes() {
        for (dark in listOf(false, true)) {
            ActivityScenario.launch(HistoryTestActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    CoffeePeekTheme(darkTheme = dark) {
                        HistoryCalendarCard(previewCombinedHistoryState(), {}, Modifier.width(320.dp))
                    }
                } }
                val first = compose.onNodeWithContentDescription("1 октября 2026, 0 чекинов").fetchSemanticsNode().boundsInRoot
                val second = compose.onNodeWithContentDescription("2 октября 2026, 0 чекинов").fetchSemanticsNode().boundsInRoot
                val nextWeek = compose.onNodeWithContentDescription("8 октября 2026, 0 чекинов").fetchSemanticsNode().boundsInRoot
                assertEquals("Week spacing must follow square day cells", second.left - first.left, nextWeek.top - first.top, 1f)
                assertEquals("Day hit area must not grow vertically", first.width, first.height, 1f)
            }
        }
    }

    @Test fun visitMarkersStayInsideTheSquareWithAndWithoutPhotosAndAnnounceTheFullCount() {
        for (dark in listOf(false, true)) for (withPhoto in listOf(false, true)) {
            val total = androidx.compose.runtime.mutableStateOf(0L)
            var color = androidx.compose.ui.graphics.Color.Unspecified
            render(dark) {
                color = MaterialTheme.colorScheme.primary
                HistoryCalendarDay(14, "2026-10-14", total.value, 0,
                    isSelected = false, isToday = false, isFuture = false, onSelect = {},
                    modifier = Modifier.width(56.dp).testTag("day"),
                    photos = if (withPhoto) listOf(HistoryCalendarPhoto("photo", true)) else emptyList(),
                    imageContent = { _, modifier, _ -> Box(modifier) })
            }.use {
                for (count in listOf(0L, 1L, 2L, 3L, 5L)) {
                    compose.runOnIdle { total.value = count }
                    val day = compose.onNodeWithTag("day").assertWidthIsEqualTo(54.dp).assertHeightIsEqualTo(54.dp)
                    val label = when (count) { 1L -> "1 чекин"; 2L, 3L -> "$count чекина"; else -> "$count чекинов" }
                    compose.onNodeWithContentDescription("14 октября 2026, $label").assertExists()
                    if (count > 3) compose.onNodeWithText("+2", useUnmergedTree = true).assertExists()
                    else compose.onNodeWithText("+2", useUnmergedTree = true).assertDoesNotExist()
                    val pixels = day.captureToImage().toPixelMap()
                    val markerPixels = (0 until pixels.width).sumOf { x ->
                        (pixels.height * 4 / 5 until pixels.height).count { y -> pixels[x, y] == color }
                    }
                    if (count == 0L) assertEquals(0, markerPixels)
                    else assertTrue("A visit must remain visible even without a photo", markerPixels > 0)
                }
            }
        }
    }
}
