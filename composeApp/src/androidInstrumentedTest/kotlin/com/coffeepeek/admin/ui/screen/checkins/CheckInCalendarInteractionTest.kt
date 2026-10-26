package com.coffeepeek.admin.ui.screen.checkins

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
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
import com.coffeepeek.core.media.ImageUrl
import com.coffeepeek.admin.ui.component.CapsuleSegmentedControl
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.ReviewRating
import java.io.File
import java.io.ByteArrayOutputStream
import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.ResourceConfig
import io.kamel.core.config.takeFrom
import io.kamel.core.fetcher.Fetcher
import io.kamel.image.config.Default
import io.kamel.image.config.LocalKamelConfig
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the production calendar composable with local fixtures, never a signed-in account. */
@RunWith(AndroidJUnit4::class)
class CheckInCalendarInteractionTest {
    @Before
    fun grantStartupPermissions() = grantCheckInFixtureStartupPermissions()

    @Test
    fun calendarRendersPhotosAndRemainsSelectable() {
        val selection = AtomicReference<String?>(null)
        val pixels = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        pixels.eraseColor(Color.MAGENTA)
        val png = ByteArrayOutputStream().use { bytes ->
            pixels.compress(Bitmap.CompressFormat.PNG, 100, bytes)
            bytes.toByteArray()
        }
        pixels.recycle()
        val fixtureUrl = "https://calendar-fixture.invalid/photo.png"
        val imageConfig = KamelConfig {
            takeFrom(KamelConfig.Default)
            fetcher(object : Fetcher<ImageUrl> {
                override val inputDataKClass = ImageUrl::class
                override val source = DataSource.Disk
                override val ImageUrl.isSupported get() = url == fixtureUrl
                override fun fetch(data: ImageUrl, resourceConfig: ResourceConfig) = flow {
                    emit(Resource.Success(ByteReadChannel(png), source))
                }
            })
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CompositionLocalProvider(LocalKamelConfig provides imageConfig) {
                    CoffeePeekTheme(darkTheme = true) {
                        Scaffold(topBar = { CpTopBar("Чекины", onBack = {}) }) { padding ->
                            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                                CalendarCard(
                                    VisitedPlacesUiState(calendarMonth = CalendarMonth(2026, 10)),
                                    today = "2026-10-13", onPreviousMonth = {}, onNextMonth = {},
                                    onSelectDate = { selection.set(it) }, onToday = {},
                                    homeCounts = mapOf("2026-10-12" to 1),
                                    homePhotos = mapOf("2026-10-12" to listOf(CalendarPhoto(fixtureUrl))),
                                )
                            }
                        }
                    }
                }
            } }
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            waitUntil {
                val day = node { it.contentDescription?.startsWith("12 октября 2026, 1 чекин") == true }
                    ?: return@waitUntil false
                val bounds = Rect().also(day::getBoundsInScreen)
                val shot = instrumentation.uiAutomation.takeScreenshot() ?: return@waitUntil false
                var magenta = 0
                for (y in bounds.top.coerceAtLeast(0) until bounds.bottom.coerceAtMost(shot.height)) {
                    for (x in bounds.left.coerceAtLeast(0) until bounds.right.coerceAtMost(shot.width)) {
                        val color = shot.getPixel(x, y)
                        if (Color.red(color) > 180 && Color.green(color) < 80 && Color.blue(color) > 180) magenta++
                    }
                }
                val rendered = magenta > 10
                if (rendered) File(instrumentation.targetContext.getExternalFilesDir(null), "checkins-calendar-photo.png")
                    .outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                shot.recycle()
                rendered
            }
            click(node { it.contentDescription?.startsWith("12 октября 2026,") == true }!!)
            waitUntil { selection.get() == "2026-10-12" }
        }
    }

    @Test
    fun calendarCombinesHomeAndCafeCountsAndSupportsMonthNavigation() {
        val selection = AtomicReference<String?>(null)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                var date by remember { mutableStateOf<String?>(null) }
                var month by remember { mutableStateOf(CalendarMonth(2026, 10)) }
                var homesOnly by remember { mutableStateOf(false) }
                CoffeePeekTheme(darkTheme = true) {
                    Scaffold(topBar = { CpTopBar("Чекины", onBack = {}) }) { padding ->
                        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CapsuleSegmentedControl(listOf(false, true), homesOnly, { if (it) "Дома" else "Все" }, { homesOnly = it })
                            CalendarCard(
                                VisitedPlacesUiState(calendarMonth = month, selectedDate = date, canGoNextMonth = month.month < 10,
                                    calendarCheckIns = if (homesOnly || month.month != 10) emptyMap() else mapOf(
                                        "2026-10-12" to listOf(visit("one", "Территория кофе", "Капучино", "09:24")))),
                                today = "2026-10-13",
                                onPreviousMonth = { month = month.plusMonths(-1); date = null },
                                onNextMonth = { month = month.plusMonths(1); date = null },
                                onSelectDate = { date = it; selection.set(it) },
                                onToday = { month = CalendarMonth(2026, 10); date = "2026-10-13"; selection.set(date) },
                                homeCounts = if (month.month == 10) mapOf("2026-10-12" to 2, "2026-10-07" to 1) else emptyMap(),
                            )
                            if (date != null) TextButton(onClick = { date = null; selection.set(null) }) { Text("Показать все даты") }
                        }
                    }
                }
            } }
            waitUntil { node { it.contentDescription?.contains("12 октября 2026, 3 чекина, 2 дома") == true } != null }
            click(node { it.text?.toString() == "Дома" }!!)
            waitUntil { node { it.contentDescription?.contains("12 октября 2026, 2 чекина, 2 дома") == true } != null }
            click(node { it.contentDescription?.startsWith("7 октября 2026") == true }!!)
            waitUntil { selection.get() == "2026-10-07" }
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.waitForIdleSync()
            instrumentation.uiAutomation.takeScreenshot()?.let { shot ->
                File(instrumentation.targetContext.getExternalFilesDir(null), "checkins-home-calendar.png").outputStream().use {
                    shot.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                shot.recycle()
            }
            click(node { it.contentDescription?.toString() == "Предыдущий месяц" }!!)
            waitUntil { node { it.text?.toString() == "Сентябрь 2026" } != null }
            click(node { it.contentDescription?.toString() == "Следующий месяц" }!!)
            waitUntil { node { it.text?.toString() == "Октябрь 2026" } != null }
            click(node { it.contentDescription?.startsWith("Сегодня,") == true }!!)
            waitUntil { selection.get() == "2026-10-13" }
            waitUntil { node { it.text?.toString() == "Показать все даты" } != null }
            click(node { it.text?.toString() == "Показать все даты" }!!)
            waitUntil { selection.get() == null }
        }
    }

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
            // Count descriptions change on the same day node after switching filters.
            root.refresh()
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
