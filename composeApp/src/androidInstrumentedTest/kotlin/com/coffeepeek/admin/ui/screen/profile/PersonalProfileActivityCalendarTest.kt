package com.coffeepeek.admin.ui.screen.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInUiState
import com.coffeepeek.admin.feature.homecheckin.ui.HomeMonth
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.screen.checkins.CalendarMonth
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesUiState
import com.coffeepeek.admin.ui.screen.checkins.grantCheckInFixtureStartupPermissions
import com.coffeepeek.admin.utils.CustomUrl
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCalendarDay
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInInput
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPhoto
import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.ResourceConfig
import io.kamel.core.config.takeFrom
import io.kamel.core.fetcher.Fetcher
import io.kamel.image.config.Default
import io.kamel.image.config.LocalKamelConfig
import io.ktor.utils.io.ByteReadChannel
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Local photo fixtures exercise the personal-profile composition without accounts or HTTP. */
@RunWith(AndroidJUnit4::class)
class PersonalProfileActivityCalendarTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val density get() = instrumentation.targetContext.resources.displayMetrics.density

    @Before
    fun grantStartupPermissions() = grantCheckInFixtureStartupPermissions()

    @Test
    fun personalActivityKeepsCafeAndWholeMonthHomeCutoutsWithoutAFrameAndSupportsMonthNavigation() {
        val month = mutableStateOf(CalendarMonth(2026, 10))
        val selection = AtomicReference<String?>()
        val requestedPhotos = CopyOnWriteArraySet<String>()
        val fixtures = mapOf(
            CAFE_CUTOUT to photo(Color.MAGENTA, cutout = true),
            EXTRA_CUTOUT to photo(Color.YELLOW, cutout = true),
            HOME_CUTOUT to photo(Color.CYAN, cutout = true),
            FALLBACK_PHOTO to photo(Color.GREEN, cutout = false),
        )
        val cafeVisits = listOf(cafeVisit(), cafeVisit().copy(id = "cafe-extra", drinkPhotoUrl = EXTRA_CUTOUT))
        val visitWithoutPhoto = cafeVisit().copy(
            id = "no-photo", createdAt = "2026-10-13T10:00:00Z", visitedAt = "2026-10-13T10:00:00Z",
            drinkPhotoUrl = null, photoUrls = emptyList(), photoThumbnailUrls = emptyList(),
        )
        val imageConfig = KamelConfig {
            takeFrom(KamelConfig.Default)
            fetcher(object : Fetcher<CustomUrl> {
                override val inputDataKClass = CustomUrl::class
                override val source = DataSource.Disk
                override val CustomUrl.isSupported get() = url in fixtures
                override fun fetch(data: CustomUrl, resourceConfig: ResourceConfig) = flow {
                    requestedPhotos += data.url
                    emit(Resource.Success(ByteReadChannel(fixtures.getValue(data.url)), source))
                }
            })
        }
        val body = AtomicReference(ComposeRect.Zero)
        val calendar = AtomicReference(ComposeRect.Zero)
        val windowHeight = AtomicInteger()
        val bottomInset = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CompositionLocalProvider(LocalKamelConfig provides imageConfig) {
                    CoffeePeekTheme(darkTheme = true) {
                        // A prominent outline makes an accidentally restored card frame detectable.
                        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
                            outlineVariant = androidx.compose.ui.graphics.Color.Red,
                        )) {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                                SwipeDismissModalBottomSheet(onDismissRequest = {}, maxSheetHeightFraction = .9f) {
                                    val height = LocalWindowInfo.current.containerSize.height
                                    val inset = WindowInsets.navigationBars.getBottom(LocalDensity.current)
                                    Column(Modifier.fillMaxWidth().onGloballyPositioned {
                                        body.set(it.unclippedBoundsInWindow())
                                        windowHeight.set(height)
                                        bottomInset.set(inset)
                                    }) {
                                        Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp)) { Text("Активность") }
                                        Column(Modifier.weight(1f, fill = false).fillMaxWidth()
                                            .verticalScroll(rememberScrollState()).padding(16.dp)) {
                                            Box(Modifier.fillMaxWidth().onGloballyPositioned {
                                                calendar.set(it.unclippedBoundsInWindow())
                                            }) {
                                                val currentMonth = month.value
                                                val hasFixtures = currentMonth == CalendarMonth(2026, 10)
                                                PersonalProfileActivityCalendarContent(
                                                    shop = VisitedPlacesUiState(
                                                        calendarMonth = currentMonth,
                                                        selectedDate = "2026-10-12",
                                                        canGoNextMonth = !hasFixtures,
                                                        calendarCheckIns = if (hasFixtures) mapOf(
                                                            "2026-10-12" to cafeVisits,
                                                            "2026-10-13" to listOf(visitWithoutPhoto),
                                                        ) else emptyMap(),
                                                    ),
                                                    home = HomeCheckInUiState(
                                                        loading = false,
                                                        month = HomeMonth(currentMonth.year, currentMonth.month),
                                                        // The selected day's timeline has no home entries. The month still does.
                                                        items = emptyList(),
                                                        selectedDate = "2026-10-12",
                                                        calendar = if (hasFixtures) listOf(HomeCalendarDay("2026-10-14", 1)) else emptyList(),
                                                        calendarItems = if (hasFixtures) listOf(homeVisit()) else emptyList(),
                                                        draft = HomeCheckInInput("2026-10-16T12:00:00Z"),
                                                    ),
                                                    today = "2026-10-16",
                                                    onPreviousMonth = { month.value = currentMonth.plusMonths(-1) },
                                                    onNextMonth = { month.value = currentMonth.plusMonths(1) },
                                                    onSelectDate = selection::set,
                                                    onToday = { month.value = CalendarMonth(2026, 10); selection.set("2026-10-16") },
                                                    onRetry = {},
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } }
            waitUntil("Cafe and home cutouts must both appear in the personal calendar") {
                val cafe = day("12 октября 2026, 2 чекина") ?: return@waitUntil false
                val home = day("14 октября 2026, 1 чекин, 1 дома") ?: return@waitUntil false
                val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return@waitUntil false
                try {
                    hasCutoutPixels(bitmap, cafe, magenta = true) && hasCutoutPixels(bitmap, home, magenta = false)
                } finally { bitmap.recycle() }
            }
            assertTrue("Cutout URLs must be chosen for both kinds of check-in", requestedPhotos.containsAll(listOf(CAFE_CUTOUT, HOME_CUTOUT)))
            assertTrue("A regular photo must not replace an available cutout", FALLBACK_PHOTO !in requestedPhotos)
            assertTrue("Additional cutouts must be represented by a marker instead of fetching a stack", EXTRA_CUTOUT !in requestedPhotos)
            val cafeDay = requireNotNull(day("12 октября 2026, 2 чекина"))
            val homeDay = requireNotNull(day("14 октября 2026, 1 чекин, 1 дома"))
            val plainDay = requireNotNull(day("13 октября 2026, 1 чекин"))
            listOf(cafeDay, homeDay, plainDay).forEach { calendarDay ->
                val bounds = AndroidRect().also(calendarDay::getBoundsInScreen)
                assertTrue("Calendar date cells must be square", abs(bounds.width() - bounds.height()) <= 2)
            }
            compose.onNode(hasContentDescription("12 октября 2026, 2 чекина", substring = true))
                .assertTextEquals("+1")
            compose.onNode(hasContentDescription("14 октября 2026, 1 чекин, 1 дома", substring = true))
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
            compose.onNode(hasContentDescription("13 октября 2026, 1 чекин", substring = true))
                .assertTextEquals("13")

            waitUntil("The personal calendar sheet must wrap its content or scroll at its limit and stay bottom anchored") {
                val expectedBodyHeight = min(calendar.get().height + 80 * density,
                    windowHeight.get() * .9f - 44 * density - bottomInset.get())
                near(body.get().height, expectedBodyHeight) &&
                    near(body.get().bottom + bottomInset.get(), windowHeight.get().toFloat())
            }
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                assertNoCalendarFrame(bitmap, calendar.get())
                save(bitmap, "personal-calendar-cutouts.png")
            } finally { bitmap.recycle() }

            click(requireNotNull(day("14 октября 2026,")))
            waitUntil("A photographed home day must remain selectable") { selection.get() == "2026-10-14" }
            click(requireNotNull(node { it.contentDescription?.toString() == "Предыдущий месяц" }))
            waitUntil("The previous-month control must navigate") {
                node { it.text?.toString() == "Сентябрь 2026" } != null && day("14 сентября 2026, 0 чекинов") != null
            }
            click(requireNotNull(node { it.contentDescription?.toString() == "Следующий месяц" }))
            waitUntil("Returning to the month must restore both types of activity") {
                day("12 октября 2026, 2 чекина") != null && day("14 октября 2026, 1 чекин, 1 дома") != null
            }
            click(requireNotNull(node { it.contentDescription?.startsWith("Сегодня,") == true }))
            waitUntil("Today must remain available in the profile calendar") { selection.get() == "2026-10-16" }
            assertEquals("2026-10-16", selection.get())
        }
    }

    private fun hasCutoutPixels(bitmap: Bitmap, day: AccessibilityNodeInfo, magenta: Boolean): Boolean {
        val bounds = AndroidRect().also(day::getBoundsInScreen)
        var colored = 0
        var white = 0
        val top = (bounds.top + bounds.height() * .1f).toInt()
        val bottom = (bounds.bottom - bounds.height() * .1f).toInt()
        for (y in top.coerceAtLeast(0) until bottom.coerceAtMost(bitmap.height)) {
            for (x in bounds.left.coerceAtLeast(0) until bounds.right.coerceAtMost(bitmap.width)) {
                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                if (blue > 180 && if (magenta) red > 180 && green < 80 else green > 180 && red < 80) colored++
                if (red > 235 && green > 235 && blue > 235) white++
            }
        }
        return colored > 10 && white > 2
    }

    private fun assertNoCalendarFrame(bitmap: Bitmap, bounds: ComposeRect) {
        var outlinePixels = 0
        val left = (bounds.left + 24 * density).toInt().coerceAtLeast(0)
        val right = (bounds.right - 24 * density).toInt().coerceAtMost(bitmap.width)
        val top = bounds.top.toInt().coerceAtLeast(0)
        val bottom = (bounds.top + 3 * density).toInt().coerceAtMost(bitmap.height)
        for (y in top until bottom) for (x in left until right) {
            val pixel = bitmap.getPixel(x, y)
            if (Color.red(pixel) > 100 && Color.green(pixel) < 80 && Color.blue(pixel) < 80) outlinePixels++
        }
        assertTrue("The activity calendar must not gain an enclosing card outline", outlinePixels < 5)
    }

    private fun cafeVisit() = CheckIn(
        id = "cafe", shopId = "shop", shopName = "Coffee fixture", note = "", createdAt = "2026-10-12T10:00:00Z",
        visitedAt = "2026-10-12T10:00:00Z", drinkPhotoUrl = CAFE_CUTOUT, photoUrls = listOf(FALLBACK_PHOTO),
    )

    private fun homeVisit() = HomeCheckIn(
        id = "home", preparedAtUtc = "2026-10-14T10:00:00Z", preparedOn = "2026-10-14", createdAtUtc = "2026-10-14T10:00:00Z",
        text = null, brewMethodCode = null, coffeeProductId = null, beanName = null, doseGrams = null,
        brewDurationSeconds = null, tasteScore = null, drinkName = null, drinkSlug = null, customDrinkName = null,
        photos = listOf(HomeCheckInPhoto("photo", FALLBACK_PHOTO)), drinkPhotoUrl = HOME_CUTOUT,
    )

    private fun photo(color: Int, cutout: Boolean): ByteArray {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        Canvas(bitmap).apply {
            if (cutout) {
                drawRoundRect(16f, 12f, 43f, 51f, 7f, 7f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                drawCircle(45f, 28f, 9f, paint)
            } else drawColor(color)
        }
        return ByteArrayOutputStream().use { bytes ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
            bitmap.recycle()
            bytes.toByteArray()
        }
    }

    private fun LayoutCoordinates.unclippedBoundsInWindow(): ComposeRect {
        val origin = positionInWindow()
        return ComposeRect(origin.x, origin.y, origin.x + size.width, origin.y + size.height)
    }

    private fun day(description: String) = node { it.contentDescription?.startsWith(description) == true }

    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun find(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            root.refresh()
            if (predicate(root)) return root
            for (index in 0 until root.childCount) root.getChild(index)?.let { child -> find(child)?.let { return it } }
            return null
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::find)
    }

    private fun click(label: AccessibilityNodeInfo) {
        val target = generateSequence(label) { it.parent }.firstOrNull { it.isClickable }
        assertTrue("Calendar control must be clickable", target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
        compose.waitForIdle()
    }

    private fun near(actual: Float, expected: Float) = abs(actual - expected) <= 2

    private fun waitUntil(message: String, condition: () -> Boolean) {
        compose.waitForIdle()
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
            compose.waitForIdle()
        }
        instrumentation.waitForIdleSync()
        assertTrue(message, condition())
    }

    private fun save(bitmap: Bitmap, name: String) {
        val directory = File(instrumentation.targetContext.filesDir, "sheet-layout").apply { mkdirs() }
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val CAFE_CUTOUT = "https://profile-calendar-fixture.invalid/cafe-cutout.png"
        const val EXTRA_CUTOUT = "https://profile-calendar-fixture.invalid/extra-cutout.png"
        const val HOME_CUTOUT = "https://profile-calendar-fixture.invalid/home-cutout.png"
        const val FALLBACK_PHOTO = "https://profile-calendar-fixture.invalid/fallback.png"
    }
}
