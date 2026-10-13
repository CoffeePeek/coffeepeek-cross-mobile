package com.coffeepeek.admin.ui.component

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the rendered control, so replacing explicit colors with Material defaults also fails. */
@RunWith(AndroidJUnit4::class)
class PlatformSwitchInteractionTest {
    @Test
    fun darkThumbsRemainVisibleAndRowsToggleOnceAcrossThemeChanges() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bounds = List(4) { AtomicReference(Rect.Zero) }
        val taps = AtomicInteger()
        val publicTaps = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                var dark by remember { mutableStateOf(true) }
                var checked by remember { mutableStateOf(false) }
                var isPublic by remember { mutableStateOf(false) }
                CoffeePeekTheme(darkTheme = dark) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.statusBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (dark) "Тёмная тема" else "Светлая тема", style = MaterialTheme.typography.headlineSmall)
                            SwitchRow("Уведомления", checked) { checked = it; taps.incrementAndGet() }
                            CheckInVisibilityControl(isPublic, true) { isPublic = it; publicTaps.incrementAndGet() }
                            listOf(false to true, true to true, false to false, true to false).forEachIndexed { index, (on, enabled) ->
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    PlatformSwitch(on, {}, enabled, Modifier.onGloballyPositioned { bounds[index].set(it.boundsInWindow()) })
                                    Text("${if (on) "Включён" else "Выключен"} · ${if (enabled) "доступен" else "недоступен"}")
                                }
                            }
                            TextButton(onClick = { dark = !dark }) { Text("Сменить тему") }
                        }
                    }
                }
            } }
            waitUntil { bounds.all { it.get().width > 0 } }
            instrumentation.waitForIdleSync()
            // Bright thumb area must be present in all four dark states, not just the active one.
            val shot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            save(shot.copy(Bitmap.Config.ARGB_8888, false), "switches-dark.png")
            bounds.forEach { ref ->
                val area = ref.get()
                var bright = 0
                for (y in area.top.toInt() until area.bottom.toInt()) for (x in area.left.toInt() until area.right.toInt()) {
                    val color = shot.getPixel(x, y)
                    if (android.graphics.Color.red(color) > 190 && android.graphics.Color.green(color) > 190 && android.graphics.Color.blue(color) > 170) bright++
                }
                assertTrue("Switch thumb disappeared: $area", bright > area.width * area.height * .1f)
            }
            shot.recycle()
            clickText("Уведомления")
            waitUntil { taps.get() == 1 }
            clickText("Уведомления")
            waitUntil { taps.get() == 2 }
            clickText("Сделать публичным")
            waitUntil { publicTaps.get() == 1 }
            clickText("Сменить тему")
            waitUntil { node { it.text?.toString() == "Светлая тема" } != null }
            instrumentation.waitForIdleSync()
            save(requireNotNull(instrumentation.uiAutomation.takeScreenshot()), "switches-light.png")
            clickText("Уведомления")
            waitUntil { taps.get() == 3 }
            assertEquals(1, publicTaps.get())
        }
    }

    private fun save(bitmap: Bitmap, name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun find(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(root)) return root
            for (i in 0 until root.childCount) root.getChild(i)?.let { find(it)?.let { found -> return found } }
            return null
        }
        return InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let(::find)
    }

    private fun clickText(text: String) {
        waitUntil { node { it.text?.toString() == text } != null }
        var target = node { it.text?.toString() == text }
        while (target != null && !target.isClickable) target = target.parent
        assertTrue(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue("Switch did not reach the expected state", condition())
    }
}
