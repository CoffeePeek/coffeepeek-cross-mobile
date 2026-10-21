package com.coffeepeek.admin.feature.notifications.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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
import com.coffeepeek.feature.notifications.api.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NotificationsPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun onePushSwitchAndUnreadActionsWorkInBothThemes() {
        var toggles = 0
        var readId: String? = null
        var readAll = 0
        val dark = mutableStateOf(false)
        compose.setContent {
            CoffeePeekTheme(darkTheme = dark.value) {
                Surface {
                    NotificationsContent(NotificationInboxState(accountId = "owner", settingsLoaded = true,
                        pushEnabled = false, deviceAvailable = true, permissionGranted = false, unreadCount = 2,
                        items = listOf(NotificationItem("first", NotificationKind.ShopApproved, "Кофейня добавлена",
                            "«Территория кофе» уже на карте. Спасибо за ваш вклад!", "2026-10-21T08:00:00Z", false),
                            NotificationItem("second", NotificationKind.NewShop, "Новая кофейня на карте",
                                "Время для новых кофейных открытий!", "2026-10-21T07:00:00Z", false),
                            NotificationItem("third", NotificationKind.General, "Новое в CoffeePeek",
                                "Делитесь кофейными открытиями с друзьями.", "2026-10-20T18:40:00Z", true))),
                        {}, { toggles++ }, { readId = it }, { readAll++ }, {}, {})
                }
            }
        }
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        screenshot("notifications-light.png")
        compose.onNode(isToggleable()).assertIsOff().performClick()
        compose.runOnIdle { assertEquals(1, toggles) }
        compose.onNodeWithText("Кофейня добавлена").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("first", readId) }
        compose.onNodeWithText("Прочитать все").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, readAll); dark.value = true }
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        screenshot("notifications-dark.png")
        compose.onNode(isToggleable()).performClick()
        compose.runOnIdle { assertEquals(2, toggles) }
    }

    @Test fun largeTextAndShortViewportKeepSettingsAndNotificationsReachable() {
        compose.setContent {
            CoffeePeekTheme(darkTheme = false) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.6f)) {
                    Surface {
                        Box(Modifier.height(340.dp)) {
                            NotificationsContent(NotificationInboxState(accountId = "owner", settingsLoaded = true,
                                deviceAvailable = true, permissionGranted = true, unreadCount = 1,
                                items = listOf(NotificationItem("first", NotificationKind.RoasterRejected, "Обжарщик не одобрен",
                                    "Проверьте данные и отправьте заявку ещё раз.", "2026-10-21T08:00:00Z", false))),
                                {}, {}, {}, {}, {}, {})
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Push-уведомления").performScrollTo().assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Обжарщик не одобрен"))
        compose.onNodeWithText("Обжарщик не одобрен").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Прочитать все"))
        compose.onNodeWithText("Прочитать все").assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
