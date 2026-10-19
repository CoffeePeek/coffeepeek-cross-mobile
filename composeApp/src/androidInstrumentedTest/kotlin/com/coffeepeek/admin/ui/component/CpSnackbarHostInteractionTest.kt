package com.coffeepeek.admin.ui.component

import android.graphics.Bitmap
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.screen.checkins.grantCheckInFixtureStartupPermissions
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CpSnackbarHostInteractionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Before fun grantStartupPermissions() = grantCheckInFixtureStartupPermissions()

    @Test
    fun longMessagesCanBeClosedOrRetriedAndDoNotBlockTheScreenInEitherTheme() {
        val result = AtomicReference<SnackbarResult?>()
        val screenTaps = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                var dark by remember { mutableStateOf(true) }
                val messages = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                CoffeePeekTheme(darkTheme = dark) {
                    Surface(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().statusBarsPadding()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = {
                                    result.set(null)
                                    scope.launch {
                                        result.set(messages.showSnackbar(MESSAGE, "Повторить", true, SnackbarDuration.Indefinite))
                                    }
                                }) { Text("Показать сообщение") }
                                Button(onClick = { dark = !dark }) { Text("Сменить тему") }
                                Button(onClick = { screenTaps.incrementAndGet() }) { Text("Проверить экран") }
                            }
                            CpSnackbarHost(messages, Modifier.align(Alignment.BottomCenter).testTag(HOST), actionIcon = CpIcons.Retry)
                        }
                    }
                }
            } }
            for ((index, theme) in listOf("dark", "light").withIndex()) {
                compose.onNodeWithText("Показать сообщение").performClick()
                compose.onNodeWithText(MESSAGE).assertIsDisplayed()
                compose.onNodeWithContentDescription("Скрыть сообщение").assertIsDisplayed()
                compose.onNodeWithText("Повторить").assertIsDisplayed()
                saveScreenshot("update-notice-$theme.png")
                compose.onNodeWithContentDescription("Скрыть сообщение").performClick()
                compose.waitUntil(5_000) { result.get() == SnackbarResult.Dismissed }
                compose.onNodeWithText(MESSAGE).assertDoesNotExist()
                compose.onNodeWithText("Проверить экран").performClick()
                assertEquals(index + 1, screenTaps.get())
                // A regular recomposition must not resurrect a dismissed message.
                compose.onNodeWithText("Сменить тему").performClick()
                compose.onNodeWithText(MESSAGE).assertDoesNotExist()
                compose.onNodeWithText("Показать сообщение").performClick()
                compose.onNodeWithText("Повторить").performClick()
                compose.waitUntil(5_000) { result.get() == SnackbarResult.ActionPerformed }
                compose.onNodeWithText(MESSAGE).assertDoesNotExist()
            }
        }
    }

    @Test
    fun retryProgressDisablesRepeatedTapsButCanStillBeDismissed() {
        val taps = AtomicInteger()
        val result = AtomicReference<SnackbarResult?>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                var dark by remember { mutableStateOf(true) }
                var busy by remember { mutableStateOf(false) }
                val messages = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                CoffeePeekTheme(darkTheme = dark) {
                    Surface(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().statusBarsPadding()) {
                            Column {
                                Button(onClick = { scope.launch {
                                    result.set(null)
                                    result.set(messages.showSnackbar(MESSAGE, "Повторить", true, SnackbarDuration.Indefinite))
                                } }) { Text("Показать сообщение") }
                                Button(onClick = { busy = false; dark = !dark }) { Text("Завершить повтор") }
                            }
                            CpSnackbarHost(messages, Modifier.align(Alignment.BottomCenter).testTag(HOST),
                                actionIcon = CpIcons.Retry, actionInProgress = busy,
                                onAction = { taps.incrementAndGet(); busy = true })
                        }
                    }
                }
            } }
            for ((index, theme) in listOf("dark", "light").withIndex()) {
                compose.onNodeWithText("Показать сообщение").performClick()
                compose.onNodeWithText("Повторить").performClick()
                compose.onNodeWithText("Повторяем…").assertIsDisplayed().assertIsNotEnabled()
                compose.onNodeWithText("Повторяем…").performClick()
                assertEquals(index + 1, taps.get())
                compose.onNodeWithText(MESSAGE).assertIsDisplayed()
                compose.mainClock.advanceTimeBy(600)
                saveScreenshot("update-notice-progress-$theme.png")
                compose.onNodeWithContentDescription("Скрыть сообщение").performClick()
                compose.waitUntil(5_000) { result.get() == SnackbarResult.Dismissed }
                compose.onNodeWithText(MESSAGE).assertDoesNotExist()
                compose.onNodeWithText("Завершить повтор").performClick()
                compose.onNodeWithText(MESSAGE).assertDoesNotExist()
            }
        }
    }

    @Test
    fun updateMessagesExpireWithoutUserInteraction() {
        val result = AtomicReference<SnackbarResult?>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                val messages = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                CoffeePeekTheme {
                    Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        Button(onClick = { scope.launch {
                            result.set(messages.showSnackbar(MESSAGE, "Повторить", true, SnackbarDuration.Long))
                        } }) { Text("Показать сообщение") }
                        CpSnackbarHost(messages, Modifier.align(Alignment.BottomCenter), actionIcon = CpIcons.Retry)
                    }
                }
            } }
            compose.onNodeWithText("Показать сообщение").assertIsDisplayed()
            compose.mainClock.autoAdvance = false
            compose.onNodeWithText("Показать сообщение").performClick()
            compose.mainClock.advanceTimeBy(1_000)
            compose.onNodeWithText(MESSAGE).assertIsDisplayed()
            compose.mainClock.advanceTimeBy(30_000)
            compose.waitUntil(5_000) { result.get() == SnackbarResult.Dismissed }
            compose.mainClock.advanceTimeBy(1_000)
            compose.onNodeWithText(MESSAGE).assertDoesNotExist()
        }
    }

    private fun saveScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(requireNotNull(context.getExternalFilesDir(null)), name)
        val bitmap = compose.onNodeWithTag(HOST).captureToImage().asAndroidBitmap()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Public test screenshots survive the runner uninstalling the fixture app.
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CoffeePeekTest")
            }
            val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            requireNotNull(context.contentResolver.openOutputStream(uri)).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private companion object {
        const val HOST = "coffeepeek-message"
        const val MESSAGE = "Обновление пока недоступно для вашего аккаунта Google Play. Попробуйте позже."
    }
}
