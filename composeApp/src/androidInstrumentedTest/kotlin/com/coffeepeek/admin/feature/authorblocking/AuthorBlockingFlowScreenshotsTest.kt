package com.coffeepeek.admin.feature.authorblocking

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.feature.authorblocking.api.*
import com.coffeepeek.admin.feature.authorblocking.ui.*
import com.coffeepeek.admin.feature.community.ui.*
import com.coffeepeek.admin.feature.profile.domain.*
import com.coffeepeek.feature.profile.domain.*
import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckInPage
import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.admin.feature.profile.ui.*
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.theme.PlatformSystemBars
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.*
import com.coffeepeek.admin.ui.screen.profile.ProfileViewModel
import com.coffeepeek.admin.ui.screen.profile.SettingsScreen
import com.coffeepeek.domain.feature.feed.*
import com.coffeepeek.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import java.io.File

/** Production screens driven by local fixtures. No real account or backend writes. */
class AuthorBlockingFlowScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private val author = AuthorReference("public-anna", "anna-coffee", "Анна")
    private val post = CheckIn("anna-check-in", "surf-coffee", "Surf Coffee", "Нежный капучино, шоколад и фундук во вкусе. Отличное место для спокойного утра.",
        "2026-10-18T09:15:00Z", visitedAt = "2026-10-18T09:00:00Z", username = "Анна", authorId = "public-anna",
        authorAddress = PublicAddress("anna-coffee", "/users/anna-coffee", 1, false), rating = ReviewRating(5, 5, 4),
        drinkNameRu = "Капучино", visibility = CheckInVisibility.Public, moderationState = CheckInModerationState.Approved, helpfulCount = 12)

    @Test fun captureCompleteFlowAndAlternateStates() {
        val sessions = ScreenshotSessions(Session("screenshot-only", userId = "viewer"))
        val blocks = ScreenshotBlocks()
        val blockVm = ScreenshotBlockPresenter(blocks, sessions)
        val users = ScreenshotUsers()
        val other = post.copy(id = "max-check-in", username = "Максим", authorId = "public-max",
            authorAddress = PublicAddress("max-brews", "/users/max-brews", 1, false), shopName = "Why Knot", note = "Фильтр на Эфиопии: яркая ягода и приятная кислотность.", helpfulCount = 7)
        val feed = ScreenshotFeed().apply { load = { _, _, _ ->
            Result.success(FeedPage(listOf(post, other).filterNot { blocks.visibility.value.hides(it.authorId, it.authorAddress?.slug) }
                .map { FeedItem(it.createdAt, it) }, null))
        } }
        val community = CommunityViewModel(ScreenshotCheckIns(), sessions, ScreenshotShops(), feed, users, blocks)
        val profile = PublicProfileViewModel("anna-coffee", ScreenshotProfileRepository(blocks, post), sessions, users = users, visibility = blocks)
        val rootKoin = GlobalContext.get()
        val settings = ProfileViewModel(rootKoin.get(), users, sessions, ScreenshotShops(), rootKoin.get(), rootKoin.get())
        val screen = MutableStateFlow("feed")
        val dark = MutableStateFlow(false)
        val navigationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val overrides = module {
            single<AuthorBlockActions> { blockVm }
            factory { community }
        }
        loadKoinModules(overrides)
        try {
            compose.setContent {
                val route by screen.collectAsState()
                val isDark by dark.collectAsState()
                CoffeePeekTheme(darkTheme = isDark) {
                    PlatformSystemBars(isDark)
                    LaunchedEffect(Unit) {
                        Navigator.navigationEvents.collect { event -> when (event) {
                            is Navigator.NavEvent.NavigateTo -> when (event.screen) {
                                Navigator.Screen.BlockedAuthors -> screen.value = "blocks"
                                is Navigator.Screen.UserProfile -> screen.value = "profile"
                                else -> Unit
                            }
                            Navigator.NavEvent.PopBack, Navigator.NavEvent.NavigateUp -> screen.value = "settings"
                            else -> Unit
                        } }
                    }
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when (route) {
                            "feed" -> CommunityScreen(onCreateCheckIn = {})
                            "profile" -> OtherUserProfileScreen("anna-coffee", "Анна", onBack = { screen.value = "feed" },
                                onCoffeeShopClick = {}, onRoasterClick = {}, onLogin = {}, onUserClick = { _, _, _ -> }, vm = profile)
                            "settings" -> SettingsScreen(settings)
                            "blocks" -> BlockedAuthorsScreen(blockVm)
                            "shop" -> Scaffold(topBar = { CpTopBar("Чекины", onBack = { screen.value = "feed" }) }) { padding ->
                                Column(Modifier.padding(padding).padding(16.dp)) {
                                    Text("Surf Coffee", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(12.dp))
                                    CheckInDisplayCard(post, isOwn = false, fullVersion = true, onReportClick = {})
                                }
                            }
                            "own" -> Scaffold(topBar = { CpTopBar("Мои чекины", onBack = { screen.value = "feed" }) }) { padding ->
                                Box(Modifier.padding(padding).padding(16.dp)) {
                                    TimelineCheckInCard(post.copy(username = "Вы"), true, false, true, false, {}, {}, {}, {}, {}, { _, _ -> })
                                }
                            }
                        }
                        AuthorBlockingHost(blockVm)
                    }
                }
            }
            waitFor { community.state.value.items.size == 2 && !community.state.value.isLoading }
            shot("01-feed-before")
            compose.onAllNodesWithContentDescription("Действия с чекином").onFirst().performClick()
            compose.onNodeWithText("Заблокировать автора").assertIsDisplayed()
            shot("02-check-in-menu")
            compose.onNodeWithText("Заблокировать автора").performClick()
            compose.onNodeWithText("Заблокировать автора «Анна»?").assertIsDisplayed()
            shot("03-block-confirmation")
            compose.onNodeWithText("Отмена").performClick()
            assertEquals(0, blocks.writes)
            compose.onAllNodesWithContentDescription("Действия с чекином").onFirst().performClick()
            compose.onNodeWithText("Заблокировать автора").performClick()
            val pending = CompletableDeferred<Result<Unit>>()
            blocks.nextMutation = pending
            compose.onNodeWithText("Заблокировать", substring = false).performClick()
            waitFor { blockVm.prompt.value?.working == true }
            shot("04-block-saving")
            pending.complete(Result.failure(IllegalStateException("Simulated offline")))
            waitFor { blockVm.prompt.value?.error != null }
            shot("05-block-error-retry")
            blocks.nextMutation = CompletableDeferred(Result.success(Unit))
            compose.onNodeWithText("Повторить").performClick()
            waitFor { blockVm.prompt.value == null && community.state.value.items.size == 1 && !community.state.value.isLoading }
            assertEquals("Максим", community.state.value.items.single().username)
            shot("06-feed-after-block")
            screen.value = "profile"
            waitFor { profile.state.value.profile?.isBlockedByViewer == true }
            compose.onNodeWithText("Автор заблокирован").assertIsDisplayed()
            shot("07-blocked-profile")
            screen.value = "settings"
            waitFor { settings.uiState.value.isLoggedIn }
            compose.onNodeWithText("Заблокированные авторы").performScrollTo()
            shot("08-settings-entry")
            compose.onNodeWithText("Заблокированные авторы").performClick()
            waitFor { screen.value == "blocks" }
            compose.onNodeWithText("@anna-coffee").assertIsDisplayed()
            shot("09-blocked-list")
            compose.onNodeWithText("Разблокировать").performClick()
            shot("10-unblock-confirmation")
            blocks.nextMutation = CompletableDeferred(Result.success(Unit))
            compose.onAllNodesWithText("Разблокировать", substring = false).onLast().performClick()
            waitFor { blocks.state.value.items.isEmpty() && blockVm.prompt.value == null }
            shot("11-empty-list-after-unblock")
            screen.value = "feed"
            waitFor { community.state.value.items.size == 2 }
            shot("12-feed-restored")
            screen.value = "profile"
            waitFor { profile.state.value.profile?.isBlockedByViewer == false && !profile.state.value.isLoading }
            shot("13-author-profile")
            compose.onNodeWithContentDescription("Действия с автором").performClick()
            shot("14-profile-menu")
            compose.onNodeWithText("Заблокировать автора").performClick()
            shot("15-block-from-profile")
            compose.onNodeWithText("Отмена").performClick()
            assertTrue(blocks.state.value.items.isEmpty())
            screen.value = "shop"
            compose.onNodeWithContentDescription("Действия с чекином").performClick()
            shot("16-shop-check-in-menu")
            compose.onNodeWithText("Заблокировать автора").performClick()
            compose.onNodeWithText("Отмена").performClick()
            screen.value = "own"
            compose.onNodeWithContentDescription("Действия с чекином").performClick()
            compose.onNodeWithText("Заблокировать автора").assertDoesNotExist()
            shot("17-own-check-in-menu")
            compose.onNodeWithText("Редактировать").performClick()
            sessions.applySession(null)
            waitFor { community.state.value.isLoggedIn == false }
            screen.value = "feed"
            compose.onAllNodesWithContentDescription("Действия с чекином").onFirst().performClick()
            compose.onNodeWithText("Заблокировать автора").performClick()
            waitFor { blockVm.needsLogin.value }
            shot("18-guest-login-required")
            compose.onNodeWithText("Отмена").performClick()
            sessions.applySession(Session("screenshot-only", userId = "viewer"))
            waitFor { community.state.value.isLoggedIn == true }
            blocks.listMode = "loading"
            screen.value = "blocks"
            waitFor { blocks.state.value.loading }
            shot("19-list-loading")
            blocks.listMode = "error"
            blockVm.refresh()
            waitFor { blocks.state.value.error != null }
            compose.onNodeWithText("Нет заблокированных авторов").assertDoesNotExist()
            shot("20-list-error")
            blocks.listMode = "ready"
            compose.onNodeWithText("Повторить").performClick()
            waitFor { blocks.state.value.error == null && !blocks.state.value.loading }
            dark.value = true
            blocks.nextMutation = CompletableDeferred(Result.success(Unit))
            blockVm.requestBlock(author)
            shot("21-dark-block-confirmation")
            compose.onNodeWithText("Заблокировать", substring = false).performClick()
            waitFor { blocks.state.value.items.isNotEmpty() && blockVm.prompt.value == null }
            shot("22-dark-blocked-list")
        } finally {
            unloadKoinModules(overrides)
            community.close(); profile.close(); blockVm.close(); navigationScope.cancel()
        }
    }
    private fun waitFor(predicate: () -> Boolean) { compose.waitUntil(timeoutMillis = 15_000, condition = predicate); compose.waitForIdle() }
    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(250)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val destination = File(instrumentation.targetContext.getExternalFilesDir(null), "author-blocking-screenshots/$name.png")
        destination.parentFile!!.mkdirs()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        destination.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        println("AuthorBlockingScreenshot: ${destination.absolutePath}")
    }
}

/** Public presentation/visibility fixtures; the feature module tests its real repository and presenter. */
private class ScreenshotBlocks : AuthorVisibility {
    val state = MutableStateFlow(AuthorBlockingListState(signedIn = true, ready = true))
    override val visibility = MutableStateFlow(AuthorVisibilitySnapshot())
    @Volatile var nextMutation: CompletableDeferred<Result<Unit>>? = null
    @Volatile var listMode = "ready"
    @Volatile var writes = 0
    override suspend fun refresh(): Result<Unit> {
        state.update { it.copy(loading = listMode == "loading", error = if (listMode == "error") "Не удалось загрузить блокировки. Повторите попытку." else null) }
        return Result.success(Unit)
    }
    suspend fun setBlocked(author: AuthorReference, blocked: Boolean): Result<Unit> {
        writes++
        val result = nextMutation?.await() ?: Result.success(Unit)
        result.onSuccess {
            state.update { previous -> previous.copy(items = previous.items.filterNot { it.id == author.id }.let {
                if (blocked) it + BlockedAuthorItem(requireNotNull(author.id), author.name, author.slug, author.avatarUrl) else it
            }) }
            visibility.update { AuthorVisibilitySnapshot(it.revision + 1, blockedIds = state.value.items.mapTo(mutableSetOf()) { it.id }, blockedSlugs = state.value.items.mapNotNullTo(mutableSetOf()) { it.slug }) }
        }
        return result
    }
}

private class ScreenshotBlockPresenter(
    private val fixture: ScreenshotBlocks,
    private val sessions: ScreenshotSessions,
) : AuthorBlockingPresenter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val blocks = fixture.state
    override val prompt = MutableStateFlow<AuthorBlockPrompt?>(null)
    override val needsLogin = MutableStateFlow(false)
    override fun requestBlock(author: AuthorReference) = request(author, true)
    override fun requestUnblock(author: AuthorReference) = request(author, false)
    private fun request(author: AuthorReference, blocked: Boolean) {
        if (!sessions.isActiveSession(sessions.peekSession())) needsLogin.value = true
        else prompt.value = AuthorBlockPrompt(author, blocked)
    }
    override fun dismiss() { if (prompt.value?.working != true) prompt.value = null }
    override fun loginHandled() { needsLogin.value = false }
    override fun refresh() { scope.launch { fixture.refresh() } }
    override fun confirm() {
        val request = prompt.value?.takeUnless { it.working } ?: return
        prompt.value = request.copy(working = true)
        scope.launch {
            fixture.setBlocked(request.author, request.blocked)
                .onSuccess { prompt.value = null }
                .onFailure { prompt.value = request.copy(error = "Не удалось сохранить блокировку. Проверьте подключение и повторите попытку.") }
        }
    }
    override fun close() { scope.cancel() }
}

private class ScreenshotProfileRepository(private val blocks: ScreenshotBlocks, private val post: CheckIn) : PublicProfileRepository {
    override suspend fun getProfile(slug: String): Result<PublicProfile> = Result.success(PublicProfile("anna-coffee", "Анна", "Исследую кофейни Минска и делюсь любимым кофе.", null,
        PublicProfileStats(24, 8, 3, 42), emptyList(), null, "/users/anna-coffee", authorId = "public-anna", isBlockedByViewer = blocks.visibility.value.hides("public-anna", "anna-coffee")))
    override suspend fun getCheckIns(slug: String, cursor: String?) = Result.success(PublicProfileCheckInPage(listOf(PublicProfileCheckIn(post.id, post.shopId, post.shopName, "Капучино", null, 4.7, post.note, post.createdAt, emptyList(), post)), null))
    override suspend fun setFollowing(slug: String, isFollowing: Boolean) = Result.success(PublicProfileFollowState(isFollowing, 42))
    override suspend fun getFavorites(slug: String) = Result.success(emptyList<PublicProfileFavorite>())
    override suspend fun getActivity(slug: String, year: Int, month: Int, timeZone: String) = Result.success(PublicProfileActivity("$year-${month.toString().padStart(2, '0')}", setOf(3, 7, 10, 18), totalCheckIns = 4))
    override suspend fun getConnections(slug: String, kind: PublicProfileConnectionKind, page: Int, pageSize: Int) = Result.success(PublicProfileConnectionsPage(emptyList(), 0, 1, 20))
}
