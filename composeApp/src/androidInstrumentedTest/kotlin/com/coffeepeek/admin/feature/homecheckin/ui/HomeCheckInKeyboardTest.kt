package com.coffeepeek.admin.feature.homecheckin.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInInput
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeCheckInKeyboardTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation
    private val imeHeight = AtomicInteger()
    private val saved = AtomicReference<HomeCheckInInput>()

    @Test fun numericKeyboardKeepsFocusedFieldAndSaveVisible() = withEditor {
        val initialButtonTop = bounds(clickable(text("Сохранить чекин"))).top
        scrollToField("Количество кофе, г")
        tap(field("Количество кофе, г"))
        waitUntil { imeHeight.get() > 0 && keyboardBounds() != null }
        setText("Количество кофе, г", "18")
        assertAboveKeyboard("Сохранить чекин")
        val inputBounds = bounds(field("Количество кофе, г"))
        val saveBounds = bounds(clickable(text("Сохранить чекин")))
        assertTrue("Focused dose must remain above the footer", inputBounds.bottom <= saveBounds.top)
        assertTrue("Footer must move up with the keyboard", saveBounds.top < initialButtonTop)
        screenshot("home-keyboard-dose.png")

        scrollToField("Секунды заваривания")
        tap(field("Секунды заваривания"))
        setText("Секунды заваривания", "30")
        assertAboveKeyboard("Сохранить чекин")
        tap(clickable(text("Сохранить чекин")))
        waitUntil { saved.get() != null && imeHeight.get() == 0 }
        assertEquals("18", saved.get().doseGrams)
        assertEquals(30, saved.get().brewDurationSeconds)
    }

    @Test fun beanNameAndActionRemainVisibleWithTextKeyboard() = withEditor {
        scrollToField("Выбрать зерно")
        tap(requireNotNull(node { it.contentDescription?.toString() == "Выбрать зерно" }))
        waitUntil { node { it.contentDescription?.toString() == "Название своего зерна" } != null }
        tap(field("Название своего зерна"))
        waitUntil { imeHeight.get() > 0 && keyboardBounds() != null }
        setText("Название своего зерна", "Эфиопия Иргачеффе")
        assertAboveKeyboard("Использовать своё зерно")
        assertTrue(bounds(field("Название своего зерна")).bottom <= bounds(clickable(text("Использовать своё зерно"))).top)
        screenshot("home-keyboard-bean.png")
        tap(clickable(text("Использовать своё зерно")))
        waitUntil { imeHeight.get() == 0 && node { it.text?.toString() == "Кофе дома" } != null }
        tap(clickable(text("Сохранить чекин")))
        waitUntil { saved.get() != null }
        assertEquals("Эфиопия Иргачеффе", saved.get().customBeanName)
    }

    private fun withEditor(block: () -> Unit) {
        val originalFlags = automation.serviceInfo.flags
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        val state = mutableStateOf(HomeCheckInUiState(loading = false, month = HomeMonth(2026, 10), editing = true,
            draft = HomeCheckInInput("2026-10-13T08:30:00Z", preparedOn = "2026-10-13")))
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    val keyboard = WindowInsets.ime.getBottom(LocalDensity.current)
                    SideEffect { imeHeight.set(keyboard) }
                    CoffeePeekTheme(darkTheme = true) {
                        Surface(Modifier.fillMaxSize()) {
                            // Like the native iOS sheet host: safe areas are consumed,
                            // but the host does not resize the form for the keyboard.
                            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(top = 24.dp)) {
                                HomeEditorContent(state.value, onDraftChange = { state.value = state.value.withDraft(it) },
                                    onQueryChange = {}, onSearch = {}, onProductSelected = {}, onClose = {},
                                    onSave = { saved.set(state.value.draft) },
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp))
                            }
                        }
                    }
                } }
                waitUntil { node { it.text?.toString() == "Сохранить чекин" } != null }
                block()
            }
        } finally {
            automation.serviceInfo = automation.serviceInfo.apply { flags = originalFlags }
        }
    }

    private fun assertAboveKeyboard(button: String) {
        // Wait for IME animation/bring-into-view before checking geometry.
        waitUntil {
            val keyboard = keyboardBounds() ?: return@waitUntil false
            val action = node { it.text?.toString() == button } ?: return@waitUntil false
            bounds(clickable(action)).bottom <= keyboard.top
        }
        val keyboard = requireNotNull(keyboardBounds())
        val buttonBounds = bounds(clickable(text(button)))
        val density = instrumentation.targetContext.resources.displayMetrics.density
        assertTrue("No duplicated keyboard-sized gap below the footer", keyboard.top - buttonBounds.bottom < 48 * density)
        assertTrue("Header stays visible", bounds(text(if (button == "Сохранить чекин") "Кофе дома" else "Зерно")).top >= 0)
    }

    private fun keyboardBounds(): Rect? = automation.windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        ?.let { Rect().also(it::getBoundsInScreen) }

    private fun scrollToField(label: String) {
        repeat(24) {
            val target = node { it.contentDescription?.toString() == label }
            val scroll = node { it.isScrollable }
            if (target != null && scroll != null) {
                val targetBounds = bounds(target)
                val viewport = bounds(scroll)
                if (targetBounds.top >= viewport.top && targetBounds.bottom < viewport.bottom - 12) return
            }
            val viewport = bounds(requireNotNull(scroll) { "No form scroll viewport" })
            swipe(viewport.centerX().toFloat(), viewport.bottom - viewport.height() * .15f, viewport.height() * .2f)
        }
        error("Field not visible: $label")
    }

    private fun setText(label: String, value: String) {
        assertTrue(field(label).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }))
        instrumentation.waitForIdleSync()
    }

    private fun field(label: String): AccessibilityNodeInfo {
        val control = requireNotNull(node { it.contentDescription?.toString() == label })
        val editable: (AccessibilityNodeInfo) -> Boolean = { it.actionList.any { a -> a.id == AccessibilityNodeInfo.ACTION_SET_TEXT } }
        return find(control, editable) ?: generateSequence(control.parent) { it.parent }.first(editable)
    }

    private fun tap(target: AccessibilityNodeInfo) {
        val rect = bounds(target)
        val time = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, rect.centerX().toFloat(), rect.centerY().toFloat(), 0).also {
                automation.injectInputEvent(it, true); it.recycle()
            }
        }
        Thread.sleep(300)
    }

    private fun swipe(x: Float, y: Float, distance: Float) {
        val start = SystemClock.uptimeMillis()
        repeat(11) { step ->
            val action = when (step) { 0 -> MotionEvent.ACTION_DOWN; 10 -> MotionEvent.ACTION_UP; else -> MotionEvent.ACTION_MOVE }
            MotionEvent.obtain(start, SystemClock.uptimeMillis(), action, x, y - distance * step / 10, 0).also {
                automation.injectInputEvent(it, true); it.recycle()
            }
            SystemClock.sleep(20)
        }
        Thread.sleep(200)
    }

    private fun bounds(node: AccessibilityNodeInfo) = Rect().also(node::getBoundsInScreen)
    private fun clickable(node: AccessibilityNodeInfo) = generateSequence(node) { it.parent }.first { it.isClickable }
    private fun text(value: String) = requireNotNull(node { it.text?.toString() == value }) { "Missing text: $value" }
    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? =
        automation.windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .firstNotNullOfOrNull { it.root?.let { root -> find(root, predicate) } }

    private fun find(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(root)) return root
        for (index in 0 until root.childCount) root.getChild(index)?.let { find(it, predicate)?.let { found -> return found } }
        return null
    }

    private fun waitUntil(predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(100)
        }
        screenshot("home-keyboard-failure.png")
        error("Keyboard/form condition timed out")
    }

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        val image = requireNotNull(automation.takeScreenshot())
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }
}
