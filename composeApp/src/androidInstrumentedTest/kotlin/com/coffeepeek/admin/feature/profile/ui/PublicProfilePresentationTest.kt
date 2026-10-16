package com.coffeepeek.admin.feature.profile.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.feature.catalog.api.RoasterCard
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.ui.FavoritesProfilePreviewContent
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
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.theme.PlatformSystemBars
import com.coffeepeek.admin.ui.component.FloatingNavItem
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.PlatformFloatingBottomNavBar
import com.coffeepeek.admin.ui.component.ProvideFloatingNavClearance
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.screen.checkins.grantCheckInFixtureStartupPermissions
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.admin.ui.screen.favorites.FavoritesScreen
import com.coffeepeek.admin.ui.screen.favorites.FavoritesViewModel
import com.coffeepeek.admin.ui.screen.profile.ProfileActions
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
@OptIn(ExperimentalComposeUiApi::class)
class PublicProfilePresentationTest {
    @Before
    fun grantStartupPermissions() = grantCheckInFixtureStartupPermissions()

    @Test
    fun ownProfileAndPersonalFavoritesScreenshots() {
        for (dark in listOf(true, false)) {
            val profileVm = PublicProfileViewModel("glasha", ProfileFixtureRepository(), ProfileFixtureSessions())
            val favoritesVm = personalFavoritesFixtureViewModel()
            try {
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    scenario.onActivity { activity -> activity.setContent {
                        CoffeePeekTheme(darkTheme = dark) {
                            PlatformSystemBars(darkTheme = dark)
                            ProvideFloatingNavClearance(100.dp) {
                                Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                                    UserProfileScreen(
                                        slug = "glasha",
                                        isOwnProfile = true,
                                        onEditProfile = {},
                                        onCoffeeShopClick = {},
                                        onRoasterClick = {},
                                        onLogin = {},
                                        onUserClick = { _, _, _ -> },
                                        onFavoritesClick = {},
                                        onCheckInsClick = {},
                                        topBarActions = {
                                            ProfileActions(onContributions = {}, onLogout = {})
                                        },
                                        favoritesPreview = {
                                            FavoritesProfilePreviewContent({}, {}, vm = favoritesVm)
                                        },
                                        bottomContentPadding = 100.dp,
                                        vm = profileVm,
                                    )
                                    PlatformFloatingBottomNavBar(
                                        items = listOf(
                                            FloatingNavItem("Поиск", CpIcons.Search, false) {},
                                            FloatingNavItem("Кофе", CpIcons.CoffeeBean, false) {},
                                            FloatingNavItem("Лента", CpIcons.Community, false) {},
                                            FloatingNavItem("Профиль", CpIcons.Profile, true) {},
                                            FloatingNavItem("Настройки", CpIcons.Settings, false) {},
                                        ),
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                    )
                                }
                            }
                        }
                    } }
                    waitUntil {
                        text("@glasha") != null && text("Территория кофе")?.isVisibleToUser == true &&
                            !favoritesVm.state.value.isLoading && !favoritesVm.state.value.areRoastersLoading
                    }
                    assertTrue("Own profile must not offer following", text("Подписаться") == null)
                    screenshot("own-profile-${if (dark) "dark" else "light"}.png")
                    screenshot("own-profile-stacked-${if (dark) "dark" else "light"}.png")
                    if (dark) {
                        click(node { it.contentDescription?.toString() == "Действия с профилем" }!!)
                        waitUntil {
                            listOf("Правки кофеен", "Отправленные кофейни", "Отправленные обжарщики", "Выйти")
                                .all { text(it)?.isVisibleToUser == true }
                        }
                        assertTrue("Editing should remain on the avatar", text("Редактировать профиль") == null)
                        screenshot("own-profile-menu-dark.png")
                        click(text("Правки кофеен")!!)
                        waitUntil { text("Правки кофеен") == null }
                    }
                    assertPairedRoastersMatchCafeHeight()
                    assertRoasterDetailsVisible(
                        checkNotNull(node { it.viewIdResourceName == "favorites-preview-roaster:roast" }), "Roast", ROAST_DESCRIPTION,
                    )
                    assertRoasterDetailsVisible(
                        checkNotNull(node { it.viewIdResourceName == "favorites-preview-roaster:roast-lab" }), "Roast Lab", ROAST_LAB_DESCRIPTION,
                    )
                    screenshot("own-profile-roasters-${if (dark) "dark" else "light"}.png")
                    var profileScroll = node { it.viewIdResourceName == "profile-check-in:matcha" }?.parent
                    while (profileScroll != null && !profileScroll.isScrollable) profileScroll = profileScroll.parent
                    assertTrue("Profile should scroll to show complete check-ins",
                        profileScroll?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true)
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    Thread.sleep(250)
                    scrollTo { node { it.contentDescription?.toString() == "Лайки: 3" } }
                    assertTrue("Profile check-ins should use the feed author header", text("glasha") != null)
                    assertTrue("Profile check-ins should use the feed shop heading", text("Территория кофе") != null)
                    screenshot("own-profile-checkins-${if (dark) "dark" else "light"}.png")
                    if (dark) {
                        scenario.onActivity { activity -> activity.setContent {
                            CoffeePeekTheme(darkTheme = true) {
                                PlatformSystemBars(darkTheme = true)
                                FavoritesScreen(vm = favoritesVm)
                            }
                        } }
                        waitUntil { text("Территория кофе")?.isVisibleToUser == true && text("Roast")?.isVisibleToUser == true }
                        screenshot("favorites-page-dark.png")
                        scrollTo { text(ROAST_LAB_DESCRIPTION) }
                        assertRoasterDetailsVisible(clickableAncestor(checkNotNull(text("Roast"))), "Roast", ROAST_DESCRIPTION)
                        assertRoasterDetailsVisible(clickableAncestor(checkNotNull(text("Roast Lab"))), "Roast Lab", ROAST_LAB_DESCRIPTION)
                        screenshot("favorites-roasters-dark.png")
                    }
                    val savedRoasters = favoritesVm.state.value.roasters
                    val savedDetails = favoritesVm.state.value.roasterDetails
                    scenario.onActivity { activity -> activity.setContent {
                        CoffeePeekTheme(darkTheme = dark) {
                            PlatformSystemBars(darkTheme = dark)
                            Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                                Scaffold(topBar = { CpTopBar("Обжарщики", onBack = {}) }) { padding ->
                                    Column(
                                        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        savedRoasters.forEach { roaster ->
                                            Box(Modifier.testTag("roaster-catalog:${roaster.id}")) {
                                                RoasterCard(
                                                    roaster = roaster, details = savedDetails[roaster.id], onClick = {},
                                                    isFavorite = true, isFavoriteLoading = false, onToggleFavorite = {},
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } }
                    waitUntil { text(ROAST_DESCRIPTION)?.isVisibleToUser == true && text(ROAST_LAB_DESCRIPTION)?.isVisibleToUser == true }
                    assertRoasterDetailsVisible(checkNotNull(node { it.viewIdResourceName == "roaster-catalog:roast" }), "Roast", ROAST_DESCRIPTION)
                    assertRoasterDetailsVisible(checkNotNull(node { it.viewIdResourceName == "roaster-catalog:roast-lab" }), "Roast Lab", ROAST_LAB_DESCRIPTION)
                    screenshot("roaster-catalog-${if (dark) "dark" else "light"}.png")
                }
            } finally {
                profileVm.close()
                favoritesVm.close()
            }
        }
    }

    @Test
    fun roasterDescriptionFallbackAndFooterRemainVisibleAtLargeFont() {
        val description = "$ROAST_LAB_DESCRIPTION Для каждой партии подбираем профиль, чтобы раскрыть сладость, аромат и характер зерна."
        val roaster = CatalogItem(
            id = "roast-lab", name = "Roast Lab", description = "   ",
            address = PublicAddress("roast-lab", "/roasters/roast-lab", 1, false),
            coffeeShopsCount = 3, coffeeProductsCount = 16,
        )
        val details = RoasterDetails(id = roaster.id, name = roaster.name, about = description)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                    CoffeePeekTheme(darkTheme = true) {
                        PlatformSystemBars(darkTheme = true)
                        Box(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp).semantics { testTagsAsResourceId = true }) {
                            RoasterCard(
                                roaster = roaster, details = details, onClick = {},
                                isFavorite = true, isFavoriteLoading = false, onToggleFavorite = {},
                                modifier = Modifier.width(300.dp).height(122.dp).testTag("roaster-constrained"),
                            )
                        }
                    }
                }
            } }
            waitUntil { text(description)?.isVisibleToUser == true }
            assertRoasterDetailsVisible(checkNotNull(node { it.viewIdResourceName == "roaster-constrained" }), roaster.name, description)
            screenshot("roaster-constrained-font-dark.png")
        }
    }

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
    fun publicProfileSectionsCanBeClosedAndReopened() {
        val vm = PublicProfileViewModel("glasha", ProfileFixtureRepository(), ProfileFixtureSessions())
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    CoffeePeekTheme(darkTheme = false) {
                        OtherUserProfileScreen(
                            "glasha", onBack = {}, onCoffeeShopClick = {}, onRoasterClick = {},
                            onLogin = {}, onUserClick = { _, _, _ -> }, vm = vm,
                        )
                    }
                } }
                waitUntil { text("@glasha") != null && !vm.state.value.isLoading }

                for (sectionTitle in listOf("Избранное", "Чекины", "Активность")) {
                    val openDescription = "Все: $sectionTitle"
                    scrollTo { node { it.contentDescription?.toString() == openDescription } }
                    repeat(2) {
                        click(node { it.contentDescription?.toString() == openDescription }!!)
                        waitUntil {
                            node { it.text?.toString() == sectionTitle && it.isVisibleToUser } != null &&
                                node { it.contentDescription?.toString() == "Закрыть" }?.isVisibleToUser == true
                        }
                        click(node { it.contentDescription?.toString() == "Закрыть" }!!)
                        waitUntil {
                            node { it.contentDescription?.toString() == "Закрыть" } == null &&
                                node { it.contentDescription?.toString() == openDescription }?.isVisibleToUser == true
                        }
                    }
                }
            }
        } finally { vm.close() }
    }

    @Test
    fun connectionSheetFitsEmptyContentAndKeepsTabsVisibleWhileLongListScrolls() {
        val state = mutableStateOf(PublicProfileUiState(
            isLoading = false,
            connectionKind = PublicProfileConnectionKind.Followers,
        ))
        val following = List(60) { index ->
            val name = "connection${index.toString().padStart(2, '0')}"
            PublicProfileConnection(name, null, name, "/users/$name")
        }
        fun visibleConnectionTab(label: String): AccessibilityNodeInfo? {
            val header = node { it.contentDescription?.toString() == "Закрыть" && it.isVisibleToUser } ?: return null
            val headerBottom = bounds(header).bottom
            return node { it.text?.toString() == label && it.isVisibleToUser && bounds(it).top > headerBottom }
        }
        fun selectConnectionTab(label: String) {
            var dispatched = false
            waitUntil {
                if (dispatched) return@waitUntil true
                val tab = visibleConnectionTab(label) ?: return@waitUntil false
                val control = generateSequence(tab) { it.parent }.firstOrNull { it.isClickable && it.isVisibleToUser }
                (control?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true).also { dispatched = it }
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(darkTheme = true) {
                    PublicProfileConnectionsSheet(
                        state = state.value,
                        onDismiss = {},
                        onSelectKind = { kind ->
                            state.value = state.value.copy(
                                connectionKind = kind,
                                connections = if (kind == PublicProfileConnectionKind.Following) following else emptyList(),
                            )
                        },
                        onLoadMore = {},
                        onRetry = {},
                        onUserClick = {},
                    )
                }
            } }
            waitUntil { text("Пока нет подписчиков")?.isVisibleToUser == true }
            val emptyHeader = settledBounds { node { it.contentDescription?.toString() == "Закрыть" } }
            val windowBounds = activeWindowBounds()
            val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
            assertTrue("An empty connections sheet must leave the profile visible above it", emptyHeader.top > windowBounds.centerY())
            assertTrue("The empty sheet must stay anchored near the bottom", windowBounds.bottom - bounds(checkNotNull(text("Пока нет подписчиков"))).bottom < 96 * density)
            screenshot("profile-connections-fitted-empty.png")

            selectConnectionTab("Подписки")
            waitUntil { text("@connection00")?.isVisibleToUser == true }
            val longHeader = settledBounds { node { it.contentDescription?.toString() == "Закрыть" } }
            assertTrue("A long list should expand beyond the empty sheet", emptyHeader.top - longHeader.top > 48 * density)
            assertTrue("The expanded connections sheet must retain its top margin", longHeader.top > windowBounds.top + windowBounds.height() * 0.06f)
            scrollTo { text("@connection59") }
            val lastConnection = bounds(checkNotNull(text("@connection59")))
            val scrolledHeader = bounds(checkNotNull(node { it.contentDescription?.toString() == "Закрыть" }))
            assertTrue("The close control must remain fixed above a scrolled list", kotlin.math.abs(longHeader.top - scrolledHeader.top) <= 2)
            assertTrue("Both connection tabs must remain visible", visibleConnectionTab("Подписчики") != null && visibleConnectionTab("Подписки") != null)
            assertTrue("The final connection must be reachable beneath the tabs", lastConnection.top > scrolledHeader.bottom && lastConnection.bottom <= windowBounds.bottom)
            screenshot("profile-connections-fitted-long.png")

            selectConnectionTab("Подписчики")
            waitUntil { text("Пока нет подписчиков")?.isVisibleToUser == true }
            val restoredHeader = settledBounds { node { it.contentDescription?.toString() == "Закрыть" } }
            assertTrue("Returning to an empty tab must shrink the sheet again", kotlin.math.abs(emptyHeader.top - restoredHeader.top) <= 2)
            assertTrue("The previous tab's rows must be removed", text("@connection59") == null)
        }
    }

    @Test
    fun profileActivitySheetFitsCalendarWithoutUnusedSpaceBelowIt() {
        val vm = PublicProfileViewModel("glasha", ProfileFixtureRepository(), ProfileFixtureSessions())
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    CoffeePeekTheme(darkTheme = true) {
                        OtherUserProfileScreen(
                            "glasha", onBack = {}, onCoffeeShopClick = {}, onRoasterClick = {},
                            onLogin = {}, onUserClick = { _, _, _ -> }, vm = vm,
                        )
                    }
                } }
                waitUntil { text("@glasha") != null && !vm.state.value.isLoadingActivity }
                scrollTo { node { it.contentDescription?.toString() == "Все: Активность" } }
                click(checkNotNull(node { it.contentDescription?.toString() == "Все: Активность" }))
                waitUntil { text("По времени Минска")?.isVisibleToUser == true }
                settledBounds { node { it.contentDescription?.toString() == "Закрыть" } }
                val calendarBottom = bounds(checkNotNull(text("По времени Минска"))).bottom
                val windowBounds = activeWindowBounds()
                val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
                assertTrue("The activity sheet must end just below its calendar", windowBounds.bottom - calendarBottom < 96 * density)
                assertTrue("The close control must stay visible above the calendar", node { it.contentDescription?.toString() == "Закрыть" }?.isVisibleToUser == true)
                screenshot("profile-activity-fitted.png")
            }
        } finally { vm.close() }
    }

    @Test
    fun personalActivityCalendarSlotIsRestrictedToOwnProfile() {
        for (ownProfile in listOf(true, false)) {
            val vm = PublicProfileViewModel("glasha", ProfileFixtureRepository(), ProfileFixtureSessions())
            try {
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    scenario.onActivity { activity -> activity.setContent {
                        CoffeePeekTheme(darkTheme = false) {
                            UserProfileScreen(
                                slug = "glasha",
                                isOwnProfile = ownProfile,
                                onCoffeeShopClick = {}, onRoasterClick = {}, onLogin = {},
                                onUserClick = { _, _, _ -> },
                                activityCalendar = { Text("Личный календарь с фотографиями") },
                                vm = vm,
                            )
                        }
                    } }
                    waitUntil { vm.state.value.profile?.activity != null }
                    scrollTo { node { it.contentDescription?.toString() == "Все: Активность" } }
                    click(checkNotNull(node { it.contentDescription?.toString() == "Все: Активность" }))
                    if (ownProfile) {
                        waitUntil { text("Личный календарь с фотографиями")?.isVisibleToUser == true }
                        assertTrue("The supplied personal calendar must replace the public calendar", text("Дни с чекинами") == null)
                    } else {
                        waitUntil { text("Дни с чекинами")?.isVisibleToUser == true }
                        assertTrue("Another user's profile must never compose the personal calendar", text("Личный календарь с фотографиями") == null)
                    }
                }
            } finally { vm.close() }
        }
    }

    @Test
    fun ownProfileUsesSharedSectionsWithoutFollowAndOpensPersonalDestinations() {
        val vm = PublicProfileViewModel("glasha", ProfileFixtureRepository(), ProfileFixtureSessions())
        val openedDestination = AtomicReference<String>()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent {
                    CoffeePeekTheme(darkTheme = false) {
                        UserProfileScreen(
                            slug = "glasha",
                            isOwnProfile = true,
                            onCoffeeShopClick = {},
                            onRoasterClick = {},
                            onLogin = {},
                            onUserClick = { _, _, _ -> },
                            onFavoritesClick = { openedDestination.set("favorites") },
                            onCheckInsClick = { openedDestination.set("checkins") },
                            vm = vm,
                        )
                    }
                } }
                waitUntil { text("@glasha") != null && text("Подписчики") != null }
                listOf("Чекины", "Кофейни", "Обжарщики", "Подписчики", "Избранное").forEach {
                    assertTrue("Missing own profile label: $it", text(it) != null)
                }
                assertTrue("Own profile must not offer following", text("Подписаться") == null)
                assertTrue("Own profile must not offer unfollowing", text("Вы подписаны") == null)

                click(node { it.contentDescription?.toString() == "Все: Избранное" }!!)
                waitUntil { openedDestination.get() == "favorites" }
                assertTrue("Own favorites should open their destination", node { it.contentDescription?.toString() == "Закрыть" } == null)

                scrollTo { node { it.contentDescription?.toString() == "Все: Чекины" } }
                click(node { it.contentDescription?.toString() == "Все: Чекины" }!!)
                waitUntil { openedDestination.get() == "checkins" }
                assertTrue("Own check-ins should open their destination", node { it.contentDescription?.toString() == "Закрыть" } == null)

                scrollTo { text("Активность") }
                assertTrue("Own profile should retain shared activity", text("Активность")?.isVisibleToUser == true)
            }
        } finally { vm.close() }
    }

    @Test
    fun profileActionsKeepModerationAndLogoutInsideMenuAndDispatchEachAction() {
        val selectedAction = AtomicReference<String>()
        val actions = listOf(
            "Правки кофеен" to ContributionKind.Changes.name,
            "Отправленные кофейни" to ContributionKind.Shops.name,
            "Отправленные обжарщики" to ContributionKind.Roasters.name,
            "Выйти" to "logout",
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CoffeePeekTheme(darkTheme = false) {
                    Box(
                        Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        ProfileActions(
                            onContributions = { selectedAction.set(it.name) },
                            onLogout = { selectedAction.set("logout") },
                        )
                    }
                }
            } }
            waitUntil { node { it.contentDescription?.toString() == "Действия с профилем" } != null }
            actions.forEach { (label, expectedAction) ->
                actions.forEach { (menuLabel, _) ->
                    assertTrue("Profile action must stay inside the menu: $menuLabel", text(menuLabel) == null)
                }
                click(node { it.contentDescription?.toString() == "Действия с профилем" }!!)
                waitUntil { actions.all { (menuLabel, _) -> text(menuLabel)?.isVisibleToUser == true } }
                assertTrue("Editing should remain on the avatar", text("Редактировать профиль") == null)
                click(text(label)!!)
                waitUntil { selectedAction.get() == expectedAction }
                waitUntil { text(label) == null }
            }
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

    private fun bounds(node: AccessibilityNodeInfo) = Rect().also(node::getBoundsInScreen)

    private fun activeWindowBounds() = bounds(checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow))

    private fun settledBounds(find: () -> AccessibilityNodeInfo?): Rect {
        var previous = Rect()
        var settledSamples = 0
        waitUntil {
            val current = find()?.takeIf { it.isVisibleToUser }?.let(::bounds)
            settledSamples = if (current != null && current == previous && !current.isEmpty) settledSamples + 1 else 0
            if (current != null) previous = current
            settledSamples >= 3
        }
        return previous
    }

    private fun assertRoasterDetailsVisible(card: AccessibilityNodeInfo, name: String, description: String) {
        val cardBounds = Rect().also(card::getBoundsInScreen)
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val tolerance = kotlin.math.ceil(density).toInt().coerceAtLeast(1)
        fun find(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
            root.refresh()
            if (predicate(root)) return root
            for (index in 0 until root.childCount) root.getChild(index)?.let { find(it, predicate)?.let { found -> return found } }
            return null
        }
        fun isInsideCard(candidate: AccessibilityNodeInfo): Boolean {
            val bounds = Rect().also(candidate::getBoundsInScreen)
            return bounds.width() > 0 && bounds.height() > 0 &&
                bounds.left >= cardBounds.left - tolerance && bounds.right <= cardBounds.right + tolerance &&
                bounds.top >= cardBounds.top - tolerance && bounds.bottom <= cardBounds.bottom + tolerance
        }
        // Android can export a tagged layout wrapper and its clickable card as
        // accessibility siblings. Bind fallback matches to the measured card's bounds.
        fun findInCard(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? =
            find(card) { predicate(it) && isInsideCard(it) }
                ?: node { predicate(it) && isInsideCard(it) }
        waitUntil {
            findInCard { it.text?.toString() == name } != null &&
                findInCard { it.text?.toString() == description } != null
        }
        val title = checkNotNull(findInCard { it.text?.toString() == name }) {
            "Missing roaster title '$name' inside $cardBounds (tagged children=${card.childCount})"
        }
        val details = checkNotNull(findInCard { it.text?.toString() == description }) {
            "Missing roaster description '$name' inside $cardBounds"
        }
        assertTrue("Roaster description must stay visible: $name", details.isVisibleToUser)
        val titleBounds = Rect().also(title::getBoundsInScreen)
        val detailsBounds = Rect().also(details::getBoundsInScreen)
        assertTrue("Description must sit beneath its title: $name", detailsBounds.top >= titleBounds.bottom - tolerance)
        assertTrue("Description must sit to the right of the logo: $name", detailsBounds.left >= cardBounds.left + 32 * density)
        listOf("Кофейни", "Каталог").forEach { label ->
            val metric = checkNotNull(findInCard {
                val value = it.text?.toString().orEmpty()
                value.contains(label) && value.length <= label.length + 12
            })
            val metricBounds = Rect().also(metric::getBoundsInScreen)
            assertTrue("Roaster footer must remain visible: $name / $label", metric.isVisibleToUser)
            assertTrue("Roaster footer must stay below the description: $name / $label", metricBounds.top >= detailsBounds.bottom - tolerance)
            assertTrue("Roaster footer must fit inside the card: $name / $label", metricBounds.bottom <= cardBounds.bottom + tolerance)
        }
    }

    private fun clickableAncestor(label: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var target: AccessibilityNodeInfo? = label
        while (target != null && !target.isClickable) target = target.parent
        return checkNotNull(target)
    }

    private fun assertPairedRoastersMatchCafeHeight() {
        val cafe = checkNotNull(node { it.viewIdResourceName == "favorites-preview-shop:territory-coffee" })
        val cafeBounds = Rect().also(cafe::getBoundsInScreen)
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val tolerance = kotlin.math.ceil(density).toInt().coerceAtLeast(1)
        val expectedWidth = (300 * density).toInt()
        repeat(5) {
            val top = node { it.viewIdResourceName == "favorites-preview-roaster:roast" }
            val bottom = node { it.viewIdResourceName == "favorites-preview-roaster:roast-lab" }
            val topBounds = top?.let { Rect().also(it::getBoundsInScreen) }
            val bottomBounds = bottom?.let { Rect().also(it::getBoundsInScreen) }
            if (top?.isVisibleToUser == true && bottom?.isVisibleToUser == true &&
                topBounds != null && bottomBounds != null && topBounds.width() >= expectedWidth - tolerance &&
                bottomBounds.width() >= expectedWidth - tolerance) {
                assertTrue("Roasters must share one column", kotlin.math.abs(topBounds.left - bottomBounds.left) <= tolerance &&
                    kotlin.math.abs(topBounds.right - bottomBounds.right) <= tolerance)
                assertTrue("Roasters must be stacked with a gap", bottomBounds.top > topBounds.bottom)
                assertTrue("Roaster column must match adjacent cafe height: cafe=$cafeBounds, top=$topBounds, bottom=$bottomBounds",
                    kotlin.math.abs((bottomBounds.bottom - topBounds.top) - cafeBounds.height()) <= tolerance)
                assertTrue("Roaster column must align with adjacent cafe", kotlin.math.abs(topBounds.top - cafeBounds.top) <= tolerance)
                return
            }
            val carousel = checkNotNull(node { it.viewIdResourceName == "favorites-preview-carousel" })
            assertTrue("Favorites carousel should scroll horizontally", carousel.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(250)
        }
        screenshot("own-profile-roaster-layout-failure.png")
        error("Both roasters should be fully visible in one column")
    }

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

private const val ROAST_DESCRIPTION = "Белорусская обжарка с 2010 года. Чистый вкус и сезонные лоты."
private const val ROAST_LAB_DESCRIPTION = "Обжариваем небольшие партии для фильтра и эспрессо в Минске."

private fun personalFavoritesFixtureViewModel(): FavoritesViewModel {
    val shop = CoffeeShopDetails(
        shop = CoffeeShop(
            id = "territory-coffee",
            title = "Территория кофе",
            rating = 4.7,
            reviewCount = 12,
            cityName = "Минск",
            priceRange = "2",
            photoUrl = null,
            address = "пр. Независимости, 95",
            isOpen = true,
            isFavorite = true,
            publicAddress = PublicAddress("territory-coffee", "/coffee-shops/territory-coffee", 1, false),
        ),
    )
    val roaster = CatalogItem(
        id = "roast", name = "Roast", slug = "roast",
        address = PublicAddress("roast", "/roasters/roast", 1, false),
        coffeeShopsCount = 7, coffeeProductsCount = 38, availableCoffeeProducts = 24,
        description = ROAST_DESCRIPTION,
    )
    val secondRoaster = CatalogItem(
        id = "roast-lab", name = "Roast Lab", slug = "roast-lab",
        address = PublicAddress("roast-lab", "/roasters/roast-lab", 1, false),
        coffeeShopsCount = 3, coffeeProductsCount = 16, availableCoffeeProducts = 12,
        description = ROAST_LAB_DESCRIPTION,
    )
    val shops = object : FavoriteRepository {
        override suspend fun getFavoriteIds() = setOf(shop.shop.id)
        override suspend fun isFavorite(shopId: String) = shopId == shop.shop.id
        override suspend fun getFavorites() = Result.success(listOf(shop))
        override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> = error("Screenshot fixture is read-only")
        override suspend fun removeFavorite(shopId: String): Result<Unit> = error("Screenshot fixture is read-only")
        override suspend fun clearAll() = Unit
    }
    val favorites = object : RoasterFavorites {
        override fun observeFavorites() = MutableStateFlow(listOf(roaster, secondRoaster))
        override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> = error("Screenshot fixture is read-only")
    }
    val roasters = object : RoasterRepository {
        override suspend fun getRoasters() = Result.success(emptyList<RoasterSummary>())
        override suspend fun getRoaster(id: String): Result<RoasterDetails> {
            val item = listOf(roaster, secondRoaster).single { it.id == id }
            return Result.success(RoasterDetails(
                id = item.id, name = item.name, about = item.description,
                publicAddress = item.address,
            ))
        }
        override suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult> = error("Screenshot fixture is read-only")
        override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<RoasterSubmission>> = error("Unused")
    }
    return FavoritesViewModel(shops, favorites, roasters)
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
            description = ROAST_DESCRIPTION,
            coffeeShopsCount = 7, coffeeProductsCount = 38,
        ),
        PublicProfileFavorite(
            "roaster-lab", "Roast Lab", PublicFavoriteKind.Roaster, slug = "roast-lab",
            publicAddress = PublicAddress("roast-lab", "/roasters/roast-lab", 1, false),
            description = ROAST_LAB_DESCRIPTION,
            coffeeShopsCount = 3, coffeeProductsCount = 16,
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

    private fun entry(id: String, title: String, note: String): PublicProfileCheckIn {
        val isHome = id != "matcha"
        val createdAt = "2026-10-14T10:00:00Z"
        val source = CheckIn(
            id = id, shopId = if (isHome) "" else "territory-coffee",
            shopName = if (isHome) "" else "Территория кофе", note = note, createdAt = createdAt,
            username = "glasha", rating = if (isHome) null else ReviewRating(5, 4, 5),
            drinkNameRu = if (isHome) null else title,
            shopAddress = if (isHome) null else PublicAddress("territory-coffee", "/coffee-shops/territory-coffee", 1, false),
            visibility = CheckInVisibility.Public, moderationState = CheckInModerationState.Approved,
            helpfulCount = if (isHome) 2 else 3, isHome = isHome,
            homePreparation = if (isHome) com.coffeepeek.domain.feature.feed.FeedHomePreparation(
                "2026-10-14", "hario-v60", title, 15.0, 150, 5,
            ) else null,
        )
        return PublicProfileCheckIn(
            id = id, coffeeShopSlug = if (isHome) null else "territory-coffee",
            venue = if (isHome) "Дома" else "Территория кофе", title = title, details = "Hario V60 · 15 г",
            rating = 4.7, note = note, occurredAt = createdAt, photoUrls = emptyList(), source = source,
        )
    }
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
