package com.coffeepeek.admin.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.theme.CoffeePeekTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubmissionStatusTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sendingCanFailRetryAndShowSuccessWithWorkingActions() {
        val status = mutableStateOf(SubmissionStatus.Sending)
        var done = false
        var editing = false
        compose.setContent {
            CoffeePeekTheme(darkTheme = true) {
                SubmissionStatusScreen(
                    screenTitle = "Правка кофейни",
                    status = status.value,
                    title = when (status.value) {
                        SubmissionStatus.Sending -> "Отправляем правку…"
                        SubmissionStatus.Success -> "Правка отправлена!"
                        SubmissionStatus.Error -> "Правка не отправлена"
                    },
                    description = when (status.value) {
                        SubmissionStatus.Sending -> "Отправляем ваши изменения на проверку"
                        SubmissionStatus.Success -> "Спасибо за помощь! После проверки изменения появятся в карточке кофейни."
                        SubmissionStatus.Error -> "Не удалось подключиться. Введённые данные остались в форме."
                    },
                    onBack = {},
                ) {
                    when (status.value) {
                        SubmissionStatus.Error -> SubmissionErrorActions({ status.value = SubmissionStatus.Sending }, { editing = true })
                        SubmissionStatus.Success -> AppButton("Готово", onClick = { done = true })
                        else -> Unit
                    }
                }
            }
        }
        compose.onNodeWithText("Отправляем правку…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Загрузка").assertIsDisplayed()
        compose.onNodeWithText("Повторить отправку").assertDoesNotExist()
        screenshot("submission-sending.png")
        compose.runOnIdle { status.value = SubmissionStatus.Error }
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("Правка не отправлена").assertIsDisplayed()
        compose.onNodeWithText("Вернуться к форме").assertIsDisplayed()
        screenshot("submission-error.png")
        compose.onNodeWithText("Вернуться к форме").performClick()
        compose.runOnIdle { assertTrue(editing) }
        compose.onNodeWithText("Повторить отправку").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("Отправляем правку…").assertIsDisplayed()
        compose.runOnIdle { status.value = SubmissionStatus.Success }
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("Правка отправлена!").assertIsDisplayed()
        compose.onNodeWithContentDescription("Загрузка").assertDoesNotExist()
        screenshot("submission-success.png")
        compose.onNodeWithText("Готово").performClick()
        compose.runOnIdle { assertTrue(done) }
    }

    @Test fun errorActionsStayVisibleWithLargeTextAndShortViewport() {
        compose.setContent {
            CoffeePeekTheme(darkTheme = false) {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.6f)) {
                    SubmissionStatusContent(
                        SubmissionStatus.Error, "Кофейня не отправлена",
                        "Не удалось подключиться. Введённые данные остались в форме. ".repeat(5),
                        Modifier.fillMaxWidth().height(440.dp).padding(16.dp),
                    ) { SubmissionErrorActions({}, {}) }
                }
            }
        }
        compose.onNodeWithText("Повторить отправку").assertIsDisplayed()
        compose.onNodeWithText("Вернуться к форме").assertIsDisplayed()
        screenshot("submission-large-text.png")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: requireNotNull(instrumentation.targetContext.getExternalFilesDir(null))
        check(output.isDirectory || output.mkdirs())
        File(output, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
