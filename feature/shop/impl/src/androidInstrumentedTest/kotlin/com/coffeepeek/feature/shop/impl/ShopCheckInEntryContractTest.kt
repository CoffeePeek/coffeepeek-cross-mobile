package com.coffeepeek.feature.shop.impl

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPickerController
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Exercises the public modal entry with fake transport/platform ports, never a real write. */
class ShopCheckInEntryContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    private class Drafts : ShopCheckInDraftStore {
        var snapshot: ShopCheckInDraftSnapshot? = null
        override fun open(initial: ShopCheckInCreateInput) = Result.success(
            snapshot ?: ShopCheckInDraftSnapshot(initial.copy(text = "Тестовая заметка")).also { snapshot = it },
        )
        override fun save(snapshot: ShopCheckInDraftSnapshot): Result<Unit> {
            this.snapshot = snapshot
            return Result.success(Unit)
        }
        override fun clear(shopSlug: String): Result<Unit> {
            snapshot = null
            return Result.success(Unit)
        }
    }

    private val dates = object : ShopCheckInDateFormatter {
        override fun label(visitedAtIso: String) = "9 октября 2026"
        override fun pickerMillis(visitedAtIso: String) = 1791504000000L
        override fun visitInstant(pickerMillis: Long) = "2026-10-09T09:00:00Z"
        override fun nowMillis() = 1791547200000L
    }
    private val photos = object : ShopCheckInPhotoPicker {
        @Composable override fun rememberController() = ShopCheckInPhotoPickerController(false, { _, _ -> }, {})
    }

    @Test fun submissionNotifiesCallerAndReopeningCreatesFreshModalViewModel() {
        val writes = mutableListOf<ShopCheckInCreateInput>()
        val bindings = mutableListOf<Pair<String, String>>()
        val repository = object : ShopCheckInRepository {
            override suspend fun getDrinkOptions() = Result.success(emptyList<ShopConsumedDrinkOption>())
            override suspend fun create(input: ShopCheckInCreateInput): Result<Unit> {
                writes += input
                return Result.success(Unit)
            }
        }
        val drafts = Drafts()
        val entry = createShopCheckInCreateEntry(repository,
            { id, slug -> bindings += id to slug; drafts }, dates, photos)
        val shown = mutableStateOf(true)
        var submitted = 0
        var feed = 0
        var history = 0
        ActivityScenario.launch(ShopFormTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    if (shown.value) entry.Content("internal-id", "public-address", "Тестовая кофейня",
                        onDismiss = { shown.value = false }, onSubmitted = { submitted++ },
                        onGoToFeed = { feed++ }, onOpenHistory = { history++ })
                }
            } }
            compose.onNodeWithText("Оставить чекин").performScrollTo().performClick()
            compose.onNodeWithText("Чекин создан!").assertIsDisplayed()
            compose.onNodeWithText("К ленте").performClick()
            compose.onNodeWithText("Мои чекины").performClick()
            compose.runOnIdle {
                assertEquals(1, submitted)
                assertEquals(1, feed)
                assertEquals(1, history)
                assertEquals("public-address", writes.single().shopSlug)
                shown.value = false
            }
            compose.onNodeWithText("Чекин создан!").assertDoesNotExist()
            compose.runOnIdle { shown.value = true }
            compose.onNodeWithText("Оставить чекин").performScrollTo().performClick()
            compose.onNodeWithText("Чекин создан!").assertIsDisplayed()
            compose.runOnIdle {
                assertEquals(2, submitted)
                assertEquals(2, writes.size)
                assertEquals(listOf("internal-id" to "public-address", "internal-id" to "public-address"), bindings)
            }
        }
    }

    @Test fun dismissAndReopenRetainsDraftRatherThanCompletedScreenState() {
        val drafts = Drafts()
        val repository = object : ShopCheckInRepository {
            override suspend fun getDrinkOptions() = Result.success(emptyList<ShopConsumedDrinkOption>())
            override suspend fun create(input: ShopCheckInCreateInput) = error("No writes in dismiss test")
        }
        val entry = createShopCheckInCreateEntry(repository, { _, _ -> drafts }, dates, photos)
        val shown = mutableStateOf(true)
        ActivityScenario.launch(ShopFormTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    if (shown.value) entry.Content("internal-id", "public-address", "Тестовая кофейня",
                        onDismiss = { shown.value = false }, onSubmitted = {}, onGoToFeed = {}, onOpenHistory = {})
                }
            } }
            compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Сохранённая заметка")
            compose.onNodeWithContentDescription("Закрыть форму чекина").performClick()
            compose.onNodeWithText("Оставить чекин").assertDoesNotExist()
            compose.runOnIdle { assertEquals("Сохранённая заметка", drafts.snapshot!!.input.text); shown.value = true }
            compose.onNodeWithText("Сохранённая заметка").assertIsDisplayed()
        }
    }

    @Test fun hostDisposalCancelsWriteButKeepsUncertaintyGuardOnReopen() {
        val drafts = Drafts()
        var writes = 0
        var cancelled = false
        val repository = object : ShopCheckInRepository {
            override suspend fun getDrinkOptions() = Result.success(emptyList<ShopConsumedDrinkOption>())
            override suspend fun create(input: ShopCheckInCreateInput): Result<Unit> {
                writes++
                try { awaitCancellation() } finally { cancelled = true }
            }
        }
        val entry = createShopCheckInCreateEntry(repository, { _, _ -> drafts }, dates, photos)
        val shown = mutableStateOf(true)
        ActivityScenario.launch(ShopFormTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(FontFamily.Default) {
                    if (shown.value) entry.Content("internal-id", "public-address", "Тестовая кофейня",
                        onDismiss = { shown.value = false }, onSubmitted = {}, onGoToFeed = {}, onOpenHistory = {})
                }
            } }
            compose.onNodeWithText("Оставить чекин").performScrollTo().performClick()
            compose.onNodeWithText("Отправляем чекин…").assertIsDisplayed()
            compose.runOnIdle { shown.value = false }
            compose.onNodeWithText("Отправляем чекин…").assertDoesNotExist()
            compose.runOnIdle {
                assertTrue(cancelled)
                assertTrue(drafts.snapshot!!.deliveryUnconfirmed)
                shown.value = true
            }
            compose.onNodeWithText("Оставить чекин").performScrollTo().assertIsNotEnabled().performClick()
            compose.runOnIdle { assertEquals(1, writes) }
        }
    }
}
