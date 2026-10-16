package com.coffeepeek.admin.feature.homecheckin.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.coffeepeek.admin.ui.screen.checkins.grantCheckInFixtureStartupPermissions
import com.coffeepeek.admin.feature.catalog.api.BrewMethodOption
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.theme.PlatformSystemBars
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateSheet
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateButton
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInInput
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInVisibility
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith

/** Actual production sheets, driven with local data without an authenticated account. */
@RunWith(AndroidJUnit4::class)
class HomeCheckInFormInteractionTest {
    @Before
    fun grantStartupPermissions() = grantCheckInFixtureStartupPermissions()

    @Test
    fun publicHomeCardShowsRecipeAndItsActionsWork() {
        val edits = AtomicInteger()
        val hides = AtomicInteger()
        val entry = com.coffeepeek.domain.model.CheckIn(
            id = "home-demo", shopId = "", shopName = "", note = "Ягодный и мягкий",
            createdAt = "2026-10-13T08:30:00Z", username = "goloologo", isHome = true,
            visibility = com.coffeepeek.domain.model.CheckInVisibility.Public,
            moderationState = com.coffeepeek.domain.model.CheckInModerationState.Approved,
            homePreparation = com.coffeepeek.domain.feature.feed.FeedHomePreparation(
                "2026-10-13", "aeropress", "Эфиопия Иргачеффе", 18.0, 120, 5,
            ),
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(darkTheme = true) {
                    Scaffold(topBar = { CpTopBar("Лента", onBack = {}) }) { padding ->
                        com.coffeepeek.admin.feature.community.ui.TimelineCheckInCard(
                            entry, isOwn = true, isPublicTimeline = true, canAct = true, changingVisibility = false,
                            onShopClick = { error("A home check-in has no shop link") },
                            onEdit = { edits.incrementAndGet() }, onVisibility = { hides.incrementAndGet() },
                            onHelpful = {}, onReport = {}, onPhotoClick = { _, _ -> },
                            modifier = Modifier.padding(padding).padding(16.dp),
                        )
                    }
                }
            } }
            waitForText("Эфиопия Иргачеффе")
            screenshot("home-public-feed-card.png")
            clickDescription("Действия с чекином")
            clickText("Редактировать")
            waitUntil { edits.get() == 1 }
            clickDescription("Действия с чекином")
            clickText("Скрыть")
            waitUntil { hides.get() == 1 }
        }
    }

    @Test
    fun creationChoicesAndHomeFieldsRemainInteractive() {
        val initial = HomeCheckInInput(preparedAt = "2026-10-13T08:30:00Z", preparedOn = "2026-10-13", tasteScore = 5)
        val draft = AtomicReference(initial)
        val saved = AtomicReference<HomeCheckInInput>()
        val shops = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    var stage by remember { mutableStateOf("choice") }
                    var state by remember { mutableStateOf(HomeCheckInUiState(
                        loading = false, month = HomeMonth(2026, 10), editing = true, draft = initial,
                        brewMethods = listOf(BrewMethodOption("hario-v60", "Hario V60")),
                    )) }
                    CoffeePeekTheme(darkTheme = true) {
                        PlatformSystemBars(darkTheme = true)
                        Scaffold(topBar = { CpTopBar("Чекины", onBack = {}) },
                            floatingActionButton = { CheckInCreateButton { stage = "choice" } },
                        ) { padding ->
                            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding))
                        }
                        if (stage == "choice") CheckInCreateSheet(
                            onDismiss = { stage = "closed" },
                            onShop = { shops.incrementAndGet(); stage = "closed" },
                            onHome = { stage = "home" },
                        )
                        if (stage == "home") SwipeDismissModalBottomSheet(onDismissRequest = { stage = "closed" }) {
                            HomeEditorContent(state,
                                onDraftChange = { state = state.copy(draft = it); draft.set(it) },
                                onQueryChange = { state = state.copy(catalogQuery = it) }, onSearch = {},
                                onProductSelected = { error("Catalog is not used by this scenario") },
                                onSave = { saved.set(state.draft) }, onClose = { stage = "closed" },
                                modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
                            )
                        }
                    }
                }
            }
            waitForText("Новый чекин")
            screenshot("home-create-visibility.png")
            // Cafe is selected immediately; no extra choice is required.
            clickText("Продолжить")
            waitUntil { shops.get() == 1 }
            clickDescription("Добавить чекин")
            waitForText("Новый чекин")
            assertTrue(node { it.contentDescription?.toString() == "Закрыть выбор чекина" } == null)
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            clickDescription("Добавить чекин")
            waitForText("Новый чекин")
            clickText("Дома")
            clickText("Продолжить")
            waitForText("Кофе дома")
            screenshot("home-editor-private-visibility.png")
            assertEquals(HomeCheckInVisibility.Private, draft.get().visibility)
            clickPublicSwitch(true)
            waitUntil { draft.get().visibility == HomeCheckInVisibility.Public }
            clickPublicSwitch(false)
            waitUntil { draft.get().visibility == HomeCheckInVisibility.Private }
            clickPublicSwitch(true)
            waitUntil { draft.get().visibility == HomeCheckInVisibility.Public }
            clickDescription("Выбрать дату приготовления")
            waitForText("Готово")
            clickText("Готово")
            waitForText("Кофе дома")
            assertEquals("Selecting the same date preserves the original time", initial.preparedAt, draft.get().preparedAt)
            clickDescription("Выбрать способ заваривания")
            waitForText("Hario V60")
            clickText("Hario V60")
            waitUntil { draft.get().brewMethodCode == "hario-v60" }
            clickDescription("Выбрать зерно")
            waitForText("Своё зерно")
            waitForText("Зерно не найдено")
            assertTrue(node { it.text?.toString() in listOf("Из каталога", "Найти", "Не указывать") } == null)
            screenshot("home-bean-picker-updated.png")
            val customLabel = requireNotNull(node { it.contentDescription?.toString() == "Название своего зерна" })
            val custom = find(customLabel) { control -> control.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT } }
                ?: generateSequence(customLabel.parent) { it.parent }.firstOrNull { control ->
                    control.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
                }
                ?: error("Custom bean input missing: ${customLabel.className}; ${customLabel.actionList}")
            assertTrue(custom.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Эфиопия Иргачеффе")
            }))
            waitForText("Эфиопия Иргачеффе")
            clickText("Использовать своё зерно")
            waitForText("Кофе дома")
            waitUntil { draft.get().customBeanName == "Эфиопия Иргачеффе" }
            clickDescription("Как получился кофе?: выбрать 4 из 5")
            waitUntil { draft.get().tasteScore == 4 }
            screenshot("home-editor-public-visibility.png")
            clickText("Сохранить чекин")
            waitUntil { saved.get() != null }
            assertEquals("hario-v60", saved.get().brewMethodCode)
            assertEquals("Эфиопия Иргачеффе", saved.get().customBeanName)
            assertEquals(4, saved.get().tasteScore)
            assertEquals(HomeCheckInVisibility.Public, saved.get().visibility)
            clickDescription("Закрыть форму")
            waitUntil { node { it.contentDescription?.toString() == "Добавить чекин" } != null }
        }
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Wait for the platform sheet transition to finish before capturing pixels.
        Thread.sleep(700)
        val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }

    private fun waitForText(text: String) = waitUntil { node { it.text?.toString() == text } != null }
    private fun clickPublicSwitch(checked: Boolean) {
        waitUntil { node { it.isCheckable && it.isChecked != checked } != null }
        click(requireNotNull(node { it.isCheckable && it.isChecked != checked }))
        // Wait for Compose semantics, not just the callback's draft update, before the next tap.
        waitUntil { node { it.isCheckable && it.isChecked == checked } != null }
    }
    private fun clickText(text: String) {
        waitForText(text)
        click(requireNotNull(node { it.text?.toString() == text }) { "Missing: $text" })
    }
    private fun clickDescription(text: String) {
        waitUntil { node { it.contentDescription?.toString() == text } != null }
        click(requireNotNull(node { it.contentDescription?.toString() == text }) { "Missing: $text" })
    }

    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        return InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let { find(it, predicate) }
    }

    private fun find(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(root)) return root
        for (i in 0 until root.childCount) root.getChild(i)?.let { find(it, predicate)?.let { found -> return found } }
        return null
    }

    private fun click(label: AccessibilityNodeInfo) {
        var target: AccessibilityNodeInfo? = label
        while (target != null && !target.isClickable) target = target.parent
        assertTrue("No clickable control for ${label.text ?: label.contentDescription}",
            target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue("Home check-in form did not reach the expected state", condition())
    }
}
