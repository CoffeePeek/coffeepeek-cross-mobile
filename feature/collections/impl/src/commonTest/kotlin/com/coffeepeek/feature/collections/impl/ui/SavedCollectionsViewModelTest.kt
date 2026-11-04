package com.coffeepeek.feature.collections.impl.ui

import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SavedCollectionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val saved = SavedTestRepository()
    private val snapshots = SavedTestSnapshots()
    private val account = SavedTestAccount()
    private val target = SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "shop")
    private val cover get() = CollectionCoverInput(byteArrayOf(1, 2, 3), "image/jpeg", "cover.jpg")

    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { Dispatchers.resetMain() }

    @Test fun emptyPrivateListIsCreatedWithoutExportingOrChangingGlobalFavorites() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Editor)
        runCurrent()
        vm.onAction(SavedCollectionsAction.Title(" My list "))
        vm.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals(1, saved.creates)
        assertEquals("created", vm.state.value.collection?.summary?.id)
        assertEquals(SavedCollectionVisibility.Private, vm.state.value.collection?.summary?.visibility)
        assertTrue(vm.state.value.collection?.places.orEmpty().isEmpty())
        assertFalse(vm.state.value.hasEdits())
        assertEquals(0, snapshots.creates)
    }

    @Test fun coverFailureKeepsCreatedIdentityAndRetryDoesNotCreateAnotherList() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Editor)
        runCurrent()
        saved.uploadFailure = true
        vm.onAction(SavedCollectionsAction.Title("Cover list"))
        vm.onAction(SavedCollectionsAction.CoverPicked(cover, vm.state.value.accountVersion))
        vm.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals("created", vm.state.value.collection?.summary?.id)
        assertNotNull(vm.state.value.coverInput)
        assertEquals(SavedCollectionsFailure.Save, vm.state.value.failure)
        saved.uploadFailure = false
        vm.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals(1, saved.creates)
        assertEquals(2, saved.uploads)
        assertNull(vm.state.value.coverInput)
        assertNull(vm.state.value.failure)
    }

    @Test fun metadataConflictRetainsDraftUntilExplicitReload() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Editor, "private")
        runCurrent()
        saved.updateFailure = SavedCollectionConflict()
        vm.onAction(SavedCollectionsAction.Title("Unsaved title"))
        vm.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals("Unsaved title", vm.state.value.title)
        assertEquals(SavedCollectionsFailure.Conflict, vm.state.value.failure)
        assertEquals(0, saved.creates)
        saved.value = privateList.copy(summary = privateList.summary.copy(title = "Other device", revision = 3))
        vm.onAction(SavedCollectionsAction.Reload)
        runCurrent()
        assertEquals("Other device", vm.state.value.title)
        assertEquals(3L, vm.state.value.collection?.summary?.revision)
    }

    @Test fun changedAccountErasesInputAndRejectsAnOldGalleryCallback() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Editor)
        runCurrent()
        val version = vm.state.value.accountVersion
        vm.onAction(SavedCollectionsAction.CoverPicked(cover, version))
        runCurrent()
        val retained = checkNotNull(vm.state.value.coverInput).bytes
        account.owner.value = "second"
        runCurrent()
        assertTrue(retained.all { it == 0.toByte() })
        vm.onAction(SavedCollectionsAction.CoverPicked(cover, version))
        runCurrent()
        assertNull(vm.state.value.coverInput)
        assertEquals(0, saved.uploads)
    }

    @Test fun cancelDoesNotWriteMembershipOrRollbackGlobalFavorite() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Picker)
        runCurrent()
        vm.onAction(SavedCollectionsAction.ToggleMembership("public"))
        vm.onAction(SavedCollectionsAction.Back)
        runCurrent()
        assertEquals(0, saved.membershipWrites)
        assertEquals(0, saved.creates)
        assertEquals(0, snapshots.creates)
        assertEquals(0, snapshots.favoriteReads)
    }

    @Test fun membershipSelectionIsSentOnceAsAnAtomicSet() = runTest(dispatcher) {
        saved.membershipGate = CompletableDeferred()
        val vm = model(SavedCollectionsMode.Picker)
        runCurrent()
        vm.onAction(SavedCollectionsAction.ToggleMembership("private"))
        vm.onAction(SavedCollectionsAction.ToggleMembership("public"))
        vm.onAction(SavedCollectionsAction.CommitMemberships)
        runCurrent()
        vm.onAction(SavedCollectionsAction.CommitMemberships)
        runCurrent()
        assertEquals(1, saved.membershipWrites)
        assertEquals(setOf("private", "public"), saved.sentMemberships)
        assertEquals(target, saved.sentTarget)
        saved.membershipGate!!.complete(Unit)
        runCurrent()
    }

    @Test fun profileSettingRejectsPrivateListsAndSupportsNone() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Preference)
        runCurrent()
        vm.onAction(SavedCollectionsAction.Feature("private"))
        runCurrent()
        assertEquals(0, saved.featureWrites)
        vm.onAction(SavedCollectionsAction.Feature(null))
        runCurrent()
        assertNull(vm.state.value.featuredId)
        assertEquals(1, saved.featureWrites)
        assertEquals(0, snapshots.favoriteReads)
    }

    @Test fun preferenceRefreshClearsTheTitleOfACollectionMadePrivateElsewhere() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Preference)
        runCurrent()
        assertEquals("public", vm.state.value.featuredId)
        saved.lists = listOf(publicList.summary.copy(visibility = SavedCollectionVisibility.Private))
        saved.featuredIdValue = null
        vm.onAction(SavedCollectionsAction.Reload)
        runCurrent()
        assertNull(vm.state.value.featuredId)
        assertTrue(vm.state.value.lists.none { it.visibility == SavedCollectionVisibility.Public })
    }

    @Test fun featuredRefreshDropsStalePublicContentBeforeTheRequestCompletes() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Featured)
        runCurrent()
        assertNotNull(vm.state.value.collection)
        saved.featuredGate = CompletableDeferred()
        saved.featuredValue = null
        vm.onAction(SavedCollectionsAction.Reload)
        runCurrent()
        assertNull(vm.state.value.collection)
        saved.featuredGate!!.complete(Unit)
        runCurrent()
        assertNull(vm.state.value.collection)
        assertEquals(0, account.loads)
        assertEquals(0, snapshots.favoriteReads)
    }

    @Test fun privateListExportOmitsLinksAndAccountChangeInvalidatesItsCompletion() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Editor, "private")
        val events = mutableListOf<SavedCollectionsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }
        runCurrent()
        vm.onAction(SavedCollectionsAction.ShareLink)
        vm.onAction(SavedCollectionsAction.ShareImages)
        runCurrent()
        assertTrue(events.none { it is SavedCollectionsEvent.Share })
        val export = events.filterIsInstance<SavedCollectionsEvent.Export>().single()
        assertTrue(export.value.url.isBlank())
        assertTrue(vm.isCurrentExport(export.epoch, export.value.id))
        account.owner.value = null
        runCurrent()
        assertFalse(vm.isCurrentExport(export.epoch, export.value.id))
    }

    @Test fun anonymousListReadNeverLoadsPrivateAccountOrFavorites() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Public, "public")
        runCurrent()
        assertEquals(publicList, vm.state.value.collection)
        assertEquals(0, account.loads)
        assertEquals(0, snapshots.favoriteReads)
        vm.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals(0, saved.creates)
        assertEquals(0, saved.updates)
    }

    @Test fun pendingGalleryPreparationCannotPublishThePreviousCover() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Editor)
        runCurrent()
        vm.onAction(SavedCollectionsAction.Title("Waiting for cover"))
        vm.onAction(SavedCollectionsAction.CoverPreparing(true, vm.state.value.accountVersion))
        vm.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals(0, saved.creates)
    }

    @Test fun aPrivatePosterNeverCarriesAnAccidentallyProvidedCanonicalLink() {
        val value = privateList.copy(summary = privateList.summary.copy(url = "https://fixture.invalid/private"))
        assertTrue(value.toSharedPoster().url.isBlank())
    }

    @Test fun aFailedPickerLoadCannotAccidentallyClearAllMemberships() = runTest(dispatcher) {
        saved.mineFailure = IllegalStateException("Offline")
        val vm = model(SavedCollectionsMode.Picker)
        runCurrent()
        vm.onAction(SavedCollectionsAction.CommitMemberships)
        runCurrent()
        assertEquals(SavedCollectionsFailure.Load, vm.state.value.failure)
        assertEquals(0, saved.membershipWrites)
    }

    @Test fun failedPickerLoadCannotCreateThenReplaceUnknownMemberships() = runTest(dispatcher) {
        saved.lists = listOf(privateList.summary.copy(containsItem = true))
        saved.mineFailure = IllegalStateException("Offline")
        val vm = model(SavedCollectionsMode.Picker)
        runCurrent()
        vm.onAction(SavedCollectionsAction.QuickTitle("New list"))
        vm.onAction(SavedCollectionsAction.CreateQuick)
        vm.onAction(SavedCollectionsAction.Title("Clears transient failure"))
        vm.onAction(SavedCollectionsAction.CommitMemberships)
        runCurrent()
        assertFalse(vm.state.value.membershipsLoaded)
        assertNull(vm.state.value.quickTitle)
        assertEquals(0, saved.creates)
        assertEquals(0, saved.membershipWrites)
        assertTrue(saved.lists.single().containsItem)
    }

    @Test fun successfulPickerRetryPreservesLoadedMembershipsWhenCreatingAList() = runTest(dispatcher) {
        saved.mineFailure = IllegalStateException("Offline")
        val vm = model(SavedCollectionsMode.Picker)
        runCurrent()
        saved.mineFailure = null
        saved.lists = listOf(privateList.summary.copy(containsItem = true))
        vm.onAction(SavedCollectionsAction.Reload)
        runCurrent()
        assertTrue(vm.state.value.membershipsLoaded)
        vm.onAction(SavedCollectionsAction.QuickTitle("New list"))
        vm.onAction(SavedCollectionsAction.CreateQuick)
        runCurrent()
        vm.onAction(SavedCollectionsAction.CommitMemberships)
        runCurrent()
        assertEquals(1, saved.creates)
        assertEquals(1, saved.membershipWrites)
        assertEquals(setOf("private", "created"), saved.sentMemberships)
    }

    @Test fun successfulEmptyPickerLoadAllowsFirstListCreation() = runTest(dispatcher) {
        saved.lists = emptyList()
        val vm = model(SavedCollectionsMode.Picker)
        runCurrent()
        assertTrue(vm.state.value.membershipsLoaded)
        vm.onAction(SavedCollectionsAction.QuickTitle("First list"))
        vm.onAction(SavedCollectionsAction.CreateQuick)
        runCurrent()
        vm.onAction(SavedCollectionsAction.CommitMemberships)
        runCurrent()
        assertEquals(1, saved.creates)
        assertEquals(setOf("created"), saved.sentMemberships)
    }

    @Test fun aGalleryResultAfterCancelCannotRepopulateTheHiddenDraft() = runTest(dispatcher) {
        val vm = model(SavedCollectionsMode.Editor)
        runCurrent()
        val version = vm.state.value.accountVersion
        vm.onAction(SavedCollectionsAction.Back)
        vm.onAction(SavedCollectionsAction.CoverPicked(cover, version))
        runCurrent()
        assertNull(vm.state.value.coverInput)
        assertEquals(0, saved.uploads)
    }

    @Test fun cancelledLoadForTheSameAccountIsAnErrorInsteadOfAnEmptyLibrary() = runTest(dispatcher) {
        saved.mineFailure = CancellationException("Interrupted load")
        val vm = model(SavedCollectionsMode.Library)
        runCurrent()
        assertFalse(vm.state.value.loading)
        assertEquals(SavedCollectionsFailure.Load, vm.state.value.failure)
        assertTrue(vm.state.value.lists.isEmpty())
    }

    @Test fun cancelledCoverSaveRetainsCreatedIdAndDraftWithARetryableError() = runTest(dispatcher) {
        saved.uploadError = CancellationException("Interrupted upload")
        val vm = model(SavedCollectionsMode.Editor)
        runCurrent()
        vm.onAction(SavedCollectionsAction.Title("Pending cover"))
        vm.onAction(SavedCollectionsAction.CoverPicked(cover, vm.state.value.accountVersion))
        vm.onAction(SavedCollectionsAction.Save)
        runCurrent()
        assertEquals("created", vm.state.value.collection?.summary?.id)
        assertEquals(SavedCollectionsFailure.Save, vm.state.value.failure)
        assertNotNull(vm.state.value.coverInput)
        assertFalse(vm.state.value.busy)
    }

    private fun model(mode: SavedCollectionsMode, id: String? = null) =
        SavedCollectionsViewModel(saved, snapshots, account, mode, id, target.takeIf { mode == SavedCollectionsMode.Picker },
            "author".takeIf { mode == SavedCollectionsMode.Featured })
}
