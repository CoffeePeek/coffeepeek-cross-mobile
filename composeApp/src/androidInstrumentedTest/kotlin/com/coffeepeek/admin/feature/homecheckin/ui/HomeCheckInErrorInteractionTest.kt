package com.coffeepeek.admin.feature.homecheckin.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.feature.catalog.api.BrewMethodOption
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInField
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInInput
import com.coffeepeek.feature.homecheckin.domain.homeCheckInFieldErrors
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeCheckInErrorInteractionTest {
    @Test fun inlineErrorsAndSummaryStayInSyncInLightTheme() = verifyErrors(false)
    @Test fun inlineErrorsAndSummaryStayInSyncInDarkTheme() = verifyErrors(true)

    private fun verifyErrors(dark: Boolean) {
        val draft = HomeCheckInInput("2026-10-13T08:30:00Z", preparedOn = "2026-10-13",
            brewMethodCode = "INVALID", customBeanName = "Руанда Ньякибанда", doseGrams = "0", brewDurationSeconds = 0)
        val current = AtomicReference<HomeCheckInUiState>()
        val editorState = mutableStateOf(HomeCheckInUiState(
            loading = false, month = HomeMonth(2026, 10), editing = true, draft = draft,
            brewMethods = listOf(BrewMethodOption("hario-v60", "Hario V60")),
        ))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                var state by editorState
                SideEffect { current.set(state) }
                CoffeePeekTheme(darkTheme = dark) {
                    Scaffold(topBar = { CpTopBar("Чекины", onBack = {}) }) { contentPadding ->
                        Box(Modifier.padding(contentPadding))
                    }
                    SwipeDismissModalBottomSheet(onDismissRequest = {}) {
                        HomeEditorContent(state,
                            onDraftChange = { next -> state = state.withDraft(next); current.set(state) }, onQueryChange = {}, onSearch = {},
                            onProductSelected = {}, onClose = {},
                            onSave = { state = state.copy(fieldErrors = homeCheckInFieldErrors(state.draft)) },
                            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp),
                        )
                    }
                }
            } }
            waitUntil { text("Сохранить чекин") != null }
            click(requireNotNull(text("Сохранить чекин")))
            waitUntil { text("Проверьте заполненные поля (3 ошибки).") != null }
            scrollTo("Выберите способ заваривания из списка")
            scrollTo("Количество кофе должно быть больше 0")
            screenshot("home-inline-errors-${if (dark) "dark" else "light"}.png")
            val control = requireNotNull(node { it.contentDescription?.toString() == "Количество кофе, г" })
            val input = find(control) { it.actionList.any { action -> action.id == AccessibilityNodeInfo.ACTION_SET_TEXT } }
                ?: generateSequence(control.parent) { it.parent }.first { it.actionList.any { a -> a.id == AccessibilityNodeInfo.ACTION_SET_TEXT } }
            assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "18")
            }))
            waitUntil { current.get()?.draft?.doseGrams == "18" }
            assertEquals(2, current.get().fieldErrors.size)
            assertNull(current.get().fieldErrors[HomeCheckInField.Dose])
            assertTrue(current.get().fieldErrors.containsKey(HomeCheckInField.Method))
            waitUntil { text("Проверьте заполненные поля (2 ошибки).") != null }
            scrollTo("Время заваривания должно быть больше 0")
            assertTrue("Draft is retained after a failed save", current.get().draft.customBeanName == draft.customBeanName)
            click(requireNotNull(text("Сохранить чекин")))
            waitUntil { current.get()?.fieldErrors?.size == 2 }
        }
    }

    private fun scrollTo(value: String) {
        repeat(24) {
            val target = node { it.isVisibleToUser && (it.text?.toString()?.contains(value) == true || it.error?.toString() == value) }
            val viewport = node { it.isScrollable }
            if (target != null && viewport != null) {
                val targetBounds = Rect().also(target::getBoundsInScreen)
                val viewportBounds = Rect().also(viewport::getBoundsInScreen)
                if (targetBounds.top >= viewportBounds.top && targetBounds.bottom < viewportBounds.bottom - 12) return
            }
            val scroll = node { it.isScrollable && it.actionList.any { a -> a.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD } }
                ?: breakScroll(value)
            // A full accessibility page can jump over a compact field. Swipe a small
            // part of the visible viewport, as a user would when finding an error.
            val bounds = Rect().also(scroll::getBoundsInScreen)
            val x = bounds.centerX().toFloat()
            val startY = bounds.bottom - bounds.height() * 0.15f
            val endY = startY - bounds.height() * 0.2f
            val start = SystemClock.uptimeMillis()
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            repeat(11) { step ->
                val action = when (step) { 0 -> MotionEvent.ACTION_DOWN; 10 -> MotionEvent.ACTION_UP; else -> MotionEvent.ACTION_MOVE }
                val event = MotionEvent.obtain(start, SystemClock.uptimeMillis(), action, x, startY + (endY - startY) * step / 10, 0)
                automation.injectInputEvent(event, true)
                event.recycle()
                SystemClock.sleep(20)
            }
            Thread.sleep(200)
        }
        breakScroll(value)
    }

    private fun breakScroll(value: String): Nothing {
        screenshot("home-errors-scroll-debug.png")
        val texts = mutableListOf<String>()
        node { texts.add("${it.text} / ${it.contentDescription} / ${it.error} / visible=${it.isVisibleToUser}"); false }
        error("Text not visible: $value; nodes: ${texts.joinToString("\n")}")
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        Thread.sleep(500)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    private fun text(value: String) = node { it.text?.toString()?.contains(value) == true }
    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? =
        InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let { find(it, predicate) }

    private fun find(node: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for (index in 0 until node.childCount) node.getChild(index)?.let { find(it, predicate)?.let { match -> return match } }
        return null
    }

    private fun click(node: AccessibilityNodeInfo) {
        val target = generateSequence(node) { it.parent }.first { it.isClickable }
        assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun waitUntil(predicate: () -> Boolean) {
        val until = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < until) {
            if (predicate()) return
            Thread.sleep(100)
        }
        error("UI condition timed out")
    }
}
