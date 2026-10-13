package com.coffeepeek.admin.ui.screen.checkins

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.theme.PlatformSystemBars
import com.coffeepeek.admin.ui.component.CapsuleSegmentedControl
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.ReviewRating
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the production calendar composable with local fixtures, never a signed-in account. */
@RunWith(AndroidJUnit4::class)
class CheckInCalendarInteractionTest {
    @Test
    fun datesRemainSelectableWithMultipleVisitsAndTodaySelectsAnEmptyDay() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val selection = AtomicReference("2026-10-12")
        val visits = listOf(
            visit("one", "Территория кофе", "Капучино", "09:24"),
            visit("two", "Surf Coffee", "Айс-латте", "13:10"),
            visit("three", "Varka", "Фильтр · Эфиопия", "17:45"),
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    var date by remember { mutableStateOf("2026-10-12") }
                    CoffeePeekTheme(darkTheme = true) {
                        PlatformSystemBars(darkTheme = true)
                        Scaffold(topBar = { CpTopBar("Чекины", onBack = {}) }) { padding ->
                            Column(Modifier.fillMaxSize().padding(padding)) {
                                CapsuleSegmentedControl(CheckInViewMode.entries, CheckInViewMode.Calendar,
                                    { if (it == CheckInViewMode.Calendar) "Календарь" else "Лента" }, {},
                                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                CheckInCalendarContent(
                                    VisitedPlacesUiState(calendarMonth = CalendarMonth(2026, 10), selectedDate = date,
                                        calendarCheckIns = mapOf("2026-10-12" to visits, "2026-10-07" to visits.take(1))),
                                    onPreviousMonth = {}, onNextMonth = {},
                                    onSelectDate = { date = it; selection.set(it) },
                                    onToday = { date = "2026-10-13"; selection.set(date) },
                                    onRetry = {}, onCheckInClick = {}, today = "2026-10-13",
                                )
                            }
                        }
                    }
                }
            }
            waitUntil { node { it.contentDescription?.startsWith("12 октября 2026") == true } != null }
            val shot = instrumentation.uiAutomation.takeScreenshot()
            if (shot != null) {
                File(instrumentation.targetContext.getExternalFilesDir(null), "checkins-reference.png").outputStream().use {
                    shot.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                shot.recycle()
            }
            click(node { it.contentDescription?.startsWith("7 октября 2026") == true }!!)
            waitUntil { selection.get() == "2026-10-07" }
            click(node { it.contentDescription?.startsWith("Сегодня,") == true }!!)
            waitUntil { selection.get() == "2026-10-13" }
            assertEquals("2026-10-13", selection.get())
            waitUntil { node { it.text?.toString() == "В этот день чекинов не было" } != null }
        }
    }

    private fun visit(id: String, shop: String, drink: String, time: String) = CheckIn(
        id, id, shop, "", "2026-10-12T${time}:00Z", visitedAt = "2026-10-12T${time}:00Z",
        customDrinkName = drink, rating = ReviewRating(5, 4, 5),
    )

    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun find(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(root)) return root
            for (i in 0 until root.childCount) root.getChild(i)?.let { find(it)?.let { found -> return found } }
            return null
        }
        return InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let(::find)
    }

    private fun click(label: AccessibilityNodeInfo) {
        // Compose may expose the description as a child of the actionable node.
        var target: AccessibilityNodeInfo? = label
        while (target != null && !target.isClickable) target = target.parent
        assertTrue("No clickable control for ${label.contentDescription}",
            target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue("Calendar did not reach the expected state", condition())
    }
}
