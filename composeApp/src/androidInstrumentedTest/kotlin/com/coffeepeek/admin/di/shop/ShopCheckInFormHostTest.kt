package com.coffeepeek.admin.di.shop

import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.ui.screen.shop.CheckInDraftStore
import com.coffeepeek.admin.ui.screen.shop.LegacyShopCheckInFormRenderer
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPickerController
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Exercises Android composition with fake auth/transport, never writes to the backend. */
class ShopCheckInFormHostTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val dates = AndroidShopCheckInDateFormatter(now = { 1791547200000L })
    private val photos = object : ShopCheckInPhotoPicker {
        @Composable override fun rememberController() = ShopCheckInPhotoPickerController(false, { _, _ -> }, {})
    }

    @Test fun androidModalSubmitsOnceAndRetainsConfirmationUntilCallerNavigates() {
        val sessions = Sessions()
        val legacy = CheckInDraftStore { dates.nowMillis() }
        legacy.save(legacy.open("internal-id").copy(note = "Safe fixture note"))
        val reads = AtomicInteger()
        val writes = mutableListOf<ShopCheckInCreateInput>()
        val repository = object : ShopCheckInRepository {
            override suspend fun getDrinkOptions(): Result<List<ShopConsumedDrinkOption>> {
                reads.incrementAndGet()
                return Result.success(emptyList())
            }
            override suspend fun create(input: ShopCheckInCreateInput): Result<Unit> {
                writes += input
                return Result.success(Unit)
            }
        }
        val renderer = AndroidShopCheckInFormRenderer(AndroidSessionShopCheckInEntry(
            sessions, repository, AndroidShopCheckInDraftStore(legacy, dates), dates, photos,
        ))
        val shown = mutableStateOf(true)
        var refreshed = 0
        var feed = 0
        var history = 0
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme {
                    if (shown.value) renderer.Content("internal-id", "public-slug", "Fixture shop",
                        onDismiss = { shown.value = false }, onSubmitted = { refreshed++ },
                        onGoToFeed = { feed++ }, onOpenHistory = { history++ }) { Text("Legacy form") }
                }
            } }
            compose.onNodeWithText("Legacy form").assertDoesNotExist()
            compose.onNodeWithText("Оставить чекин").performScrollTo().performClick()
            compose.onNodeWithText("Чекин создан!").assertIsDisplayed()
            compose.onNodeWithText("К ленте").performClick()
            compose.onNodeWithText("Мои чекины").performClick()
            compose.runOnIdle {
                assertFalse(renderer.usesLegacyDraft)
                assertEquals(1, reads.get())
                assertEquals("public-slug", writes.single().shopSlug)
                assertEquals(1, refreshed)
                assertEquals(1, feed)
                assertEquals(1, history)
                assertTrue(shown.value)
                assertNull(legacy.peek())
            }
        }
    }

    @Test fun accountChangeCancelsPendingWriteClosesModalAndClearsOnlyItsDraft() {
        val sessions = Sessions()
        val legacy = CheckInDraftStore { dates.nowMillis() }
        legacy.save(legacy.open("internal-id").copy(note = "First account private note"))
        val started = AtomicBoolean()
        val cancelled = AtomicBoolean()
        val repository = object : ShopCheckInRepository {
            override suspend fun getDrinkOptions() = Result.success(emptyList<ShopConsumedDrinkOption>())
            override suspend fun create(input: ShopCheckInCreateInput): Result<Unit> {
                started.set(true)
                try { awaitCancellation() } finally { cancelled.set(true) }
            }
        }
        val renderer = AndroidShopCheckInFormRenderer(AndroidSessionShopCheckInEntry(
            sessions, repository, AndroidShopCheckInDraftStore(legacy, dates), dates, photos,
        ))
        val shown = mutableStateOf(true)
        var submitted = 0
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme {
                    if (shown.value) renderer.Content("internal-id", "public-slug", "Fixture shop",
                        onDismiss = { shown.value = false }, onSubmitted = { submitted++ },
                        onGoToFeed = {}, onOpenHistory = {}) { Text("Legacy form") }
                }
            } }
            compose.onNodeWithText("Оставить чекин").performScrollTo().performClick()
            compose.waitUntil(5_000) { started.get() }
            compose.runOnIdle { sessions.applySession(Session("other-token", userId = "other")) }
            compose.waitUntil(5_000) { cancelled.get() && !shown.value }
            compose.runOnIdle {
                assertEquals(0, submitted)
                assertNull(legacy.peek())
                shown.value = true
            }
            compose.onNodeWithText("First account private note").assertDoesNotExist()
            compose.onNodeWithText("Оставить чекин").assertExists()
        }
    }

    @Test fun defaultRendererStillUsesLegacyForm() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent {
                LegacyShopCheckInFormRenderer.Content("id", "slug", "Shop", {}, {}, {}, {}) { Text("Legacy form") }
            } }
            compose.onNodeWithText("Legacy form").assertIsDisplayed()
            assertTrue(LegacyShopCheckInFormRenderer.usesLegacyDraft)
        }
    }

    private class Sessions : SessionRepository {
        private val state = MutableStateFlow<Session?>(Session("token", userId = "owner"))
        override fun peekSession() = state.value
        override fun applySession(session: Session?) { state.value = session }
        override fun isActiveSession(session: Session?) = session != null && session.accessToken.isNotBlank()
        override suspend fun getSession() = state.value
        override suspend fun persistSession(session: Session?) = Unit
        override suspend fun saveSession(session: Session?) = applySession(session)
        override suspend fun warmCache() = Unit
        override fun observeSession() = state
        override suspend fun isLoggedIn() = isActiveSession(state.value)
    }
}
