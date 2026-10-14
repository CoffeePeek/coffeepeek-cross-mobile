package com.coffeepeek.admin.ui.screen.checkins

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.feature.homecheckin.ui.HomeDetailContent
import com.coffeepeek.admin.feature.homecheckin.ui.HomeTimelineCard
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.theme.PlatformSystemBars
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPhoto
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInVisibility
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Production UI with local fixtures: no account or real check-ins are changed. */
@RunWith(AndroidJUnit4::class)
class CheckInPresentationTest {
    @Test
    fun timelineConnectsThroughCardGapsOnTheRightInBothThemes() {
        for (dark in listOf(false, true)) {
            val first = AtomicReference<Rect>()
            val second = AtomicReference<Rect>()
            val density = AtomicReference(1f)
            val lineColor = AtomicInteger()
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    CoffeePeekTheme(darkTheme = dark) {
                        PlatformSystemBars(darkTheme = dark)
                        density.set(LocalDensity.current.density)
                        lineColor.set(MaterialTheme.colorScheme.outlineVariant.toArgb())
                        Scaffold(topBar = { CpTopBar("Чекины", onBack = {}) }) { padding ->
                            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                                Text("14 октября", style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(bottom = 12.dp))
                                repeat(3) { index ->
                                    Box(Modifier.onGloballyPositioned {
                                        if (index == 0) first.set(it.boundsInWindow())
                                        if (index == 1) second.set(it.boundsInWindow())
                                    }) {
                                        TimelineRail(isHome = index != 1, firstInDay = index == 0, lastInDay = index == 2) {
                                            HomeTimelineCard(fixture.copy(id = "$index", text = null)) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                } }
                waitUntil { first.get() != null && second.get() != null && node { it.text?.toString() == "Руанда Ньякибанда" } != null }
                val shot = screenshot("checkins-timeline-${if (dark) "dark" else "light"}.png")
                val x = (first.get().right - 8 * density.get()).roundToInt()
                val gapY = (first.get().bottom - 6 * density.get()).roundToInt()
                val nextY = (second.get().top + 6 * density.get()).roundToInt()
                assertEquals("The rail must span the gap below a card", lineColor.get(), shot.getPixel(x, gapY))
                assertEquals("The next card must continue the same rail", lineColor.get(), shot.getPixel(x, nextY))
                assertEquals(first.get().bottom, second.get().top, 1f)
                shot.recycle()
            }
        }
    }

    @Test
    fun calendarRemainsSelectableWithLargeText() {
        val selection = AtomicReference<String>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.4f)) {
                    CoffeePeekTheme(darkTheme = false) {
                        PlatformSystemBars(darkTheme = false)
                        Scaffold(topBar = { CpTopBar("Чекины", onBack = {}) }) { padding ->
                            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                                CalendarCard(
                                    VisitedPlacesUiState(calendarMonth = CalendarMonth(2026, 10), selectedDate = "2026-10-14"),
                                    today = "2026-10-14", onPreviousMonth = {}, onNextMonth = {},
                                    onSelectDate = { selection.set(it) }, onToday = { selection.set("2026-10-14") },
                                    homeCounts = mapOf("2026-10-07" to 1, "2026-10-12" to 7, "2026-10-14" to 2),
                                )
                            }
                        }
                    }
                }
            } }
            waitUntil { node { it.contentDescription?.startsWith("12 октября 2026, 7 чекинов") == true } != null }
            screenshot("checkins-calendar-large-text.png").recycle()
            click(node { it.contentDescription?.startsWith("12 октября 2026,") == true }!!)
            waitUntil { selection.get() == "2026-10-12" }
            click(node { it.contentDescription?.startsWith("Сегодня,") == true }!!)
            waitUntil { selection.get() == "2026-10-14" }
        }
    }

    @Test
    fun homeDetailShowsRecipeAndKeepsPhotosAndActionsAccessible() {
        val edits = AtomicInteger()
        val deletes = AtomicInteger()
        val opened = AtomicReference<Pair<List<String>, Int>>()
        // The unavailable local URL keeps the photo tile deterministic without any external request.
        val entry = fixture.copy(photos = listOf(HomeCheckInPhoto("photo", "http://127.0.0.1:1/checkin.jpg")))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(darkTheme = false) {
                    PlatformSystemBars(darkTheme = false)
                    Scaffold(topBar = { CpTopBar("Чекины", onBack = {}) }) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            SwipeDismissModalBottomSheet(onDismissRequest = {}) {
                                HomeDetailContent(entry, Modifier.fillMaxWidth().heightIn(max = 680.dp),
                                    onEdit = { edits.incrementAndGet() }, onDelete = { deletes.incrementAndGet() },
                                    onPhotoClick = { urls, index -> opened.set(urls to index) })
                            }
                        }
                    }
                }
            } }
            waitUntil { node { it.text?.toString() == "4 мин 20 с" } != null }
            assertTrue(node { it.text?.toString() == "15 г" } != null)
            assertTrue(node { it.text?.toString() == "Только мне" } != null)
            screenshot("checkins-home-detail.png").recycle()
            click(node { it.contentDescription?.toString() == "Фотография 1 из 1" }!!)
            waitUntil { opened.get() != null }
            assertEquals(entry.photos.map { it.url } to 0, opened.get())
            scrollToText("Удалить запись")
            click(node { it.text?.toString() == "Редактировать" }!!)
            click(node { it.text?.toString() == "Удалить запись" }!!)
            waitUntil { edits.get() == 1 && deletes.get() == 1 }
        }
    }

    private fun scrollToText(text: String) {
        repeat(8) {
            if (node { it.text?.toString() == text && it.isVisibleToUser } != null) return
            node { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(200)
        }
        assertTrue("Missing action: $text", node { it.text?.toString() == text } != null)
    }

    private fun screenshot(name: String): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val shot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            shot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return shot
    }

    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun find(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            root.refresh()
            if (predicate(root)) return root
            for (index in 0 until root.childCount) root.getChild(index)?.let { find(it)?.let { found -> return found } }
            return null
        }
        return InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let(::find)
    }

    private fun click(label: AccessibilityNodeInfo) {
        var target: AccessibilityNodeInfo? = label
        while (target != null && !target.isClickable) target = target.parent
        assertTrue(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue("Check-in UI did not reach the expected state", condition())
    }

    private val fixture = HomeCheckIn(
        id = "home", preparedAtUtc = "2026-10-14T11:47:00Z", preparedOn = "2026-10-14", createdAtUtc = "2026-10-14T11:47:00Z",
        text = "Яркий и насыщенный ягодный вкус", brewMethodCode = "hario-v60", coffeeProductId = null,
        beanName = "Руанда Ньякибанда", doseGrams = "15.0", brewDurationSeconds = 260, tasteScore = 5,
        drinkName = "Фильтр", drinkSlug = "filter", customDrinkName = null,
        photos = emptyList(), drinkPhotoUrl = null, visibility = HomeCheckInVisibility.Private,
    )
}
