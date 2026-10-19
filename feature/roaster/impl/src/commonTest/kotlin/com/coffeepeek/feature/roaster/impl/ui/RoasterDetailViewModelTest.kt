package com.coffeepeek.feature.roaster.impl.ui

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.roaster.api.RoasterFavoriteTarget
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import com.coffeepeek.feature.roaster.domain.model.RoasterContact
import com.coffeepeek.feature.roaster.impl.ui.data.instagramDisplayText
import com.coffeepeek.feature.roaster.impl.ui.data.normalizeExternalUrl
import com.coffeepeek.feature.roaster.impl.ui.data.normalizeInstagramUrl
import com.coffeepeek.feature.roaster.impl.ui.data.prettyExternalLink
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterDetailEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RoasterDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var nextKey = 0
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(
        slug: String = "roast",
        repository: FakeRoasterRepository = FakeRoasterRepository(),
        favorites: FakeFavorites = FakeFavorites(),
        shopPhotos: RoasterShopPhotos = RoasterShopPhotos.None,
        analytics: RecordingDetailAnalytics = RecordingDetailAnalytics(),
    ) = RoasterDetailViewModel(slug, repository, favorites, shopPhotos, analytics).also { store.put("detail-${nextKey++}", it) }

    private fun TestScope.events(model: RoasterDetailViewModel): List<RoasterDetailEvent> =
        mutableListOf<RoasterDetailEvent>().also { list -> backgroundScope.launch { model.events.collect { list += it } } }

    @Test fun loadsDetailsAndDerivesFavoriteFromTheSlugKey() = runTest(dispatcher) {
        val repository = FakeRoasterRepository()
        val favorites = FakeFavorites().apply { keys.value = setOf("roast") }
        val model = model(repository = repository, favorites = favorites); runCurrent()
        assertEquals("roast", model.state.value.details?.id)
        assertFalse(model.state.value.isLoading)
        assertTrue(model.state.value.isFavorite)
        repeat(3) { model.onAction(RoasterDetailAction.Initialize) }; runCurrent()
        assertEquals(listOf("roast"), repository.detailCalls)
        favorites.keys.value = emptySet(); runCurrent()
        assertFalse(model.state.value.isFavorite)
    }

    @Test fun favoriteToggleSendsSlugTargetWithFirstPhotoAndReportsFailure() = runTest(dispatcher) {
        val favorites = FakeFavorites()
        val model = model(favorites = favorites); runCurrent()
        model.onAction(RoasterDetailAction.ToggleFavorite); runCurrent()
        assertEquals(listOf(RoasterFavoriteTarget("roast", "Roaster roast", "https://m/roast-1") to true), favorites.changes)
        favorites.failChange = true
        model.onAction(RoasterDetailAction.ToggleFavorite); runCurrent()
        assertEquals(RoasterMessage.FavoriteChangeFailed, model.state.value.message)
        model.onAction(RoasterDetailAction.DismissMessage); runCurrent()
        assertNull(model.state.value.message)
    }

    @Test fun toggleBeforeDetailsAreLoadedDoesNothing() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val favorites = FakeFavorites()
        val repository = FakeRoasterRepository().apply { detail = { gate.await(); Result.success(details(it)) } }
        val model = model(repository = repository, favorites = favorites); runCurrent()
        model.onAction(RoasterDetailAction.ToggleFavorite); runCurrent()
        assertTrue(favorites.changes.isEmpty())
        gate.complete(Unit); runCurrent()
    }

    @Test fun onlyShopsWithoutPhotosAreEnrichedAndMissingPhotosStayNull() = runTest(dispatcher) {
        val looked = mutableListOf<String>()
        val repository = FakeRoasterRepository().apply { detail = { Result.success(details(it,
            shops = listOf(shop("has", "https://m/has"), shop("blank", " "), shop("none")))) } }
        val photos = RoasterShopPhotos { slug -> looked += slug; if (slug == "blank") "https://m/found" else null }
        val model = model(repository = repository, shopPhotos = photos); runCurrent()
        assertEquals(listOf("blank", "none"), looked.sorted())
        assertEquals(listOf("https://m/has", "https://m/found", null), model.state.value.details?.shops?.map { it.photoUrl })
    }

    @Test fun enrichmentIsDroppedWhenANewerLoadReplacedTheDetails() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var call = 0
        val repository = FakeRoasterRepository().apply { detail = { slug ->
            val n = ++call
            Result.success(details(slug, shops = listOf(shop("s$n"))))
        } }
        val photos = RoasterShopPhotos { slug -> if (slug == "s1") { withContext(NonCancellable) { gate.await() }; "https://m/stale" } else "https://m/fresh" }
        val model = model(repository = repository, shopPhotos = photos); runCurrent()
        model.onAction(RoasterDetailAction.Retry); runCurrent()
        gate.complete(Unit); runCurrent()
        assertEquals(listOf("s2" to "https://m/fresh"), model.state.value.details?.shops?.map { it.slug to it.photoUrl })
    }

    @Test fun failureThenRetryRecoversAndBlankOrLongSlugNeverCallsTheRepository() = runTest(dispatcher) {
        var fail = true
        val repository = FakeRoasterRepository().apply { detail = { if (fail) Result.failure(IllegalStateException()) else Result.success(details(it)) } }
        val model = model(repository = repository); runCurrent()
        assertTrue(model.state.value.loadFailed); assertFalse(model.state.value.isLoading)
        fail = false
        model.onAction(RoasterDetailAction.Retry); runCurrent()
        assertFalse(model.state.value.loadFailed); assertEquals("roast", model.state.value.details?.id)
        listOf("", " ", "x".repeat(201)).forEach { slug ->
            val guarded = FakeRoasterRepository()
            val m = model(slug = slug, repository = guarded); runCurrent()
            assertTrue(m.state.value.loadFailed); assertTrue(guarded.detailCalls.isEmpty())
        }
    }

    @Test fun photoShopAndLinkActionsValidateTargetsReportAndEmitEvents() = runTest(dispatcher) {
        val repository = FakeRoasterRepository().apply { detail = { Result.success(details(it,
            shops = listOf(shop("cafe", "https://m/c")), contact = RoasterContact("@my.roast", "roast.example/path?x=1"))) } }
        val analytics = RecordingDetailAnalytics()
        val model = model(repository = repository, analytics = analytics); val events = events(model); runCurrent()
        model.onAction(RoasterDetailAction.OpenPhoto("https://elsewhere")); runCurrent()
        assertNull(model.state.value.previewPhotoUrl)
        model.onAction(RoasterDetailAction.OpenPhoto("https://m/roast-1")); runCurrent()
        assertEquals("https://m/roast-1", model.state.value.previewPhotoUrl)
        model.onAction(RoasterDetailAction.ClosePhoto); runCurrent()
        assertNull(model.state.value.previewPhotoUrl)
        model.onAction(RoasterDetailAction.OpenShop("unknown")); model.onAction(RoasterDetailAction.OpenShop("cafe"))
        model.onAction(RoasterDetailAction.OpenInstagram); model.onAction(RoasterDetailAction.OpenWebsite)
        model.onAction(RoasterDetailAction.Back); runCurrent()
        assertEquals(listOf("Photos:Open", "Shops:Select", "shop", "Contacts:Instagram", "Contacts:Website"), analytics.calls)
        assertEquals(listOf(RoasterDetailEvent.OpenShop("cafe"), RoasterDetailEvent.OpenLink("https://instagram.com/my.roast"),
            RoasterDetailEvent.OpenLink("https://roast.example/path?x=1"), RoasterDetailEvent.Back), events)
    }

    @Test fun contactActionsDoNothingWithoutLinks() = runTest(dispatcher) {
        val analytics = RecordingDetailAnalytics()
        val model = model(analytics = analytics); val events = events(model); runCurrent()
        model.onAction(RoasterDetailAction.OpenInstagram); model.onAction(RoasterDetailAction.OpenWebsite); runCurrent()
        assertTrue(analytics.calls.isEmpty() && events.isEmpty())
    }

    @Test fun linkRulesMatchTheLegacyScreen() {
        assertEquals("https://instagram.com/a_b", normalizeInstagramUrl(" @a_b/ "))
        assertEquals("HTTPS://instagram.com/x", normalizeInstagramUrl("HTTPS://instagram.com/x"))
        assertEquals("https://x.example", normalizeExternalUrl(" x.example "))
        assertEquals("http://x.example", normalizeExternalUrl("http://x.example"))
        assertEquals("@my.roast", instagramDisplayText("https://www.instagram.com/my.roast/?hl=en#top"))
        assertEquals("@handle", instagramDisplayText("@handle"))
        assertEquals("roast.example/path", prettyExternalLink("https://www.roast.example/path/?q=1#x"))
    }
}
