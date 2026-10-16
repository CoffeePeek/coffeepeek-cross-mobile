package com.coffeepeek.admin.feature.profile.ui

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.feature.profile.domain.PublicFavoriteKind
import com.coffeepeek.admin.feature.profile.domain.PublicProfile
import com.coffeepeek.admin.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckInPage
import com.coffeepeek.admin.feature.profile.domain.PublicProfileFavorite
import com.coffeepeek.admin.feature.profile.domain.PublicProfileRepository
import com.coffeepeek.admin.feature.profile.domain.PublicProfileStats
import com.coffeepeek.admin.feature.profile.domain.PublicProfileFollowState
import com.coffeepeek.admin.feature.profile.domain.PublicProfileConnection
import com.coffeepeek.admin.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.admin.feature.profile.domain.PublicProfileConnectionsPage
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.theme.PlatformSystemBars
import com.coffeepeek.admin.ui.screen.checkins.grantCheckInFixtureStartupPermissions
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Local repository fixtures exercise the production screen without modifying server data. */
@RunWith(AndroidJUnit4::class)
class PublicProfilePresentationTest {
    @Before
    fun grantStartupPermissions() = grantCheckInFixtureStartupPermissions()

    @Test
    fun publicProfileShowsNicknameAndRequestedSectionsInBothThemes() {
        for (dark in listOf(false, true)) {
            val vm = PublicProfileViewModel("glasha", ProfileFixtureRepository(), ProfileFixtureSessions())
            try {
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    scenario.onActivity { activity -> activity.setContent {
                        CoffeePeekTheme(darkTheme = dark) {
                            PlatformSystemBars(darkTheme = dark)
                            OtherUserProfileScreen("glasha", onBack = {}, onCoffeeShopClick = {}, onRoasterClick = {}, onLogin = {}, onUserClick = { _, _, _ -> }, vm = vm)
                        }
                    } }
                    waitUntil { text("@glasha") != null && text("Подписчики") != null }
                    listOf("Чекины", "Кофейни", "Обжарщики", "Подписчики", "Избранное").forEach {
                        assertTrue("Missing profile label: $it", text(it) != null)
                    }
                    assertTrue(text("Глаша") == null)
                    assertTrue(text("Минск") == null)
                    assertTrue(text("Публичные чекины") == null)
                    assertTrue(text("Назад") == null)
                    click(text("Подписаться")!!)
                    waitUntil { text("Вы подписаны") != null && text("19") != null }
                    click(text("Вы подписаны")!!)
                    waitUntil { text("Подписаться") != null && text("18") != null }
                    screenshot("public-profile-refined-${if (dark) "dark" else "light"}.png")
                    scrollTo { text("Активность") }
                    assertTrue(text("Календарь активности") == null)
                    click(node { it.contentDescription?.toString() == "Все: Активность" }!!)
                    waitUntil { text("Дни с чекинами") != null }
                    assertTrue(node { it.contentDescription?.toString() == "14, есть чекины" } != null)
                }
            } finally { vm.close() }
        }
    }

    @Test
    fun allCheckInsLoadsNextPageAndFavoriteOpensItsPublicShop() {
        val repository = ProfileFixtureRepository()
        val vm = PublicProfileViewModel("glasha", repository, ProfileFixtureSessions())
        val openedShop = AtomicReference<String>()
        val openedRoaster = AtomicReference<String>()
        val openedUser = AtomicReference<String>()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    CoffeePeekTheme(darkTheme = false) {
                        OtherUserProfileScreen(
                            "glasha", onBack = {}, onCoffeeShopClick = { openedShop.set(it) },
                            onRoasterClick = { openedRoaster.set(it) }, onLogin = {},
                            onUserClick = { slug, _, _ -> openedUser.set(slug) }, vm = vm,
                        )
                    }
                } }
                waitUntil { text("Территория кофе") != null }
                click(text("Территория кофе")!!)
                waitUntil { openedShop.get() == "territory-coffee" }
                click(node { it.contentDescription?.toString() == "Все: Избранное" }!!)
                waitUntil { node { it.contentDescription?.toString() == "Закрыть" } != null }
                scrollTo { text("Roast") }
                click(text("Roast")!!)
                waitUntil { openedRoaster.get() == "roast" }
                waitUntil { text("Подписчики")?.isVisibleToUser == true }
                click(text("Подписчики")!!)
                waitUntil { text("@petr") != null }
                click(text("@petr")!!)
                waitUntil { openedUser.get() == "petr" }
                waitUntil { node { it.contentDescription?.toString() == "Все: Чекины" } != null }
                scrollTo { node { it.contentDescription?.toString() == "Все: Чекины" } }
                click(node { it.contentDescription?.toString() == "Все: Чекины" }!!)
                scrollTo { text("Загрузить ещё") }
                click(text("Загрузить ещё")!!)
                waitUntil { repository.cursors.contains("next-page") }
                scrollTo { text("Кения Киганджо") }
                assertEquals(listOf(null, "next-page"), repository.cursors.toList())
                screenshot("public-profile-checkins.png")
            }
        } finally { vm.close() }
    }

    private fun text(value: String) = node { it.text?.toString() == value }

    private fun scrollTo(find: () -> AccessibilityNodeInfo?) {
        repeat(12) {
            if (find()?.isVisibleToUser == true) return
            node { it.isScrollable && it.isVisibleToUser }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(150)
        }
        screenshot("public-profile-scroll-failure.png")
        assertTrue("Profile content is not reachable", find()?.isVisibleToUser == true)
    }

    private fun node(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun find(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            root.refresh()
            if (predicate(root)) return root
            for (index in 0 until root.childCount) root.getChild(index)?.let { find(it)?.let { found -> return found } }
            return null
        }
        return InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let(::find)
    }

    private fun click(label: AccessibilityNodeInfo) {
        var target: AccessibilityNodeInfo? = label
        while (target != null && !target.isClickable) target = target.parent
        assertTrue(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue("Profile UI did not reach the expected state", condition())
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val shot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            shot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        shot.recycle()
    }
}

private class ProfileFixtureRepository : PublicProfileRepository {
    val cursors = CopyOnWriteArrayList<String?>()
    private var following = false

    override suspend fun getProfile(slug: String) = Result.success(PublicProfile(
        slug = slug, nickname = "glasha", about = "Кофе, люди и хорошие дни ☕", avatarUrl = null,
        stats = PublicProfileStats(24, 12, 6, if (following) 19 else 18),
        favorites = emptyList(), activity = null, isFollowing = following,
    ))

    override suspend fun getFavorites(slug: String) = Result.success(listOf(
        PublicProfileFavorite(
            "shop", "Территория кофе", PublicFavoriteKind.CoffeeShop, slug = "territory-coffee",
            publicAddress = PublicAddress("territory-coffee", "/coffee-shops/territory-coffee", 1, false),
            address = "пр. Независимости 95", rating = 4.7, reviewCount = 12, isOpen = true,
            tags = listOf("To Go", "Laptop Friendly"),
        ),
        PublicProfileFavorite(
            "roaster", "Roast", PublicFavoriteKind.Roaster, slug = "roast",
            publicAddress = PublicAddress("roast", "/roasters/roast", 1, false),
            description = "Белорусская обжарка из Минска, работающая с кофе с 2010 года. Команда обжаривает зерно для кофеен и дома, сохраняя чистый вкус и характер каждого лота.",
            coffeeShopsCount = 7, coffeeProductsCount = 38,
        ),
        PublicProfileFavorite("drink", "Матча латте", PublicFavoriteKind.Drink, drinkSlug = "matcha_latte"),
    ))

    override suspend fun getActivity(slug: String, year: Int, month: Int, timeZone: String) = Result.success(
        PublicProfileActivity("$year-${month.toString().padStart(2, '0')}", setOf(2, 4, 6, 9, 12, 14), timeZone, 6,
            mapOf(2 to 1, 4 to 1, 6 to 1, 9 to 1, 12 to 1, 14 to 1)),
    )

    override suspend fun setFollowing(slug: String, isFollowing: Boolean): Result<PublicProfileFollowState> {
        following = isFollowing
        return Result.success(PublicProfileFollowState(isFollowing, if (following) 19 else 18))
    }

    override suspend fun getConnections(slug: String, kind: PublicProfileConnectionKind, page: Int, pageSize: Int) = Result.success(
        PublicProfileConnectionsPage(listOf(PublicProfileConnection("petr", null, "petr", "/users/petr")), 1, page, pageSize),
    )

    override suspend fun getCheckIns(slug: String, cursor: String?): Result<PublicProfileCheckInPage> {
        cursors += cursor
        return Result.success(if (cursor == null) PublicProfileCheckInPage(
            listOf(entry("matcha", "Матча", "Очень уютное место для вечерних прогулок."), entry("v60", "Колумбия Робле Вуш-Вуш", "Яркий и насыщенный ягодный вкус.")),
            "next-page",
        ) else PublicProfileCheckInPage(listOf(entry("kenya", "Кения Киганджо", "Сочный фильтр с нотами чёрной смородины.")), null))
    }

    private fun entry(id: String, title: String, note: String) = PublicProfileCheckIn(
        id = id, coffeeShopSlug = null, venue = "Дома", title = title, details = "Hario V60 · 15 г",
        rating = 4.7, note = note, occurredAt = "2026-10-14T10:00:00Z", photoUrls = emptyList(),
    )
}

private class ProfileFixtureSessions : SessionRepository {
    private val current = MutableStateFlow<Session?>(Session("fixture-token", userId = "viewer"))
    override fun peekSession() = current.value
    override fun applySession(session: Session?) { current.value = session }
    override fun isActiveSession(session: Session?) = session != null && session == current.value
    override suspend fun getSession() = current.value
    override suspend fun persistSession(session: Session?) { current.value = session }
    override suspend fun saveSession(session: Session?) { current.value = session }
    override suspend fun warmCache() = Unit
    override fun observeSession() = current
    override suspend fun isLoggedIn() = current.value != null
}
