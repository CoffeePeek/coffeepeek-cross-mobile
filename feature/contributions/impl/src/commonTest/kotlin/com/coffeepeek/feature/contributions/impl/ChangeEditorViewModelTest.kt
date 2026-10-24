package com.coffeepeek.feature.contributions.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.impl.change.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChangeEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private val repository = FakeRepository()
    private val catalogs = FakeCatalogs()
    private val shop = ShopSnapshot(
        slug = "cafe", title = "Cafe", description = "Old text", contacts = Contacts(phone = "+375291112233", instagram = "https://instagram.com/cafe"),
        photos = listOf(ShopPhotoRef("p1", "u1"), ShopPhotoRef("p2", "u2")), tagIds = listOf("wifi"), roasterIds = listOf("r1"),
        menu = listOf(MenuRow("espresso", Availability.Present, 4.5)), menuPhotos = listOf(ShopPhotoRef("m1", "mu1")),
    )

    private fun TestScope.model(section: ChangeSection, requestId: String? = null, account: FakeAccount = FakeAccount()): ChangeEditorViewModel {
        catalogs.shop = { Result.success(shop) }
        catalogs.drinks = { Result.success(listOf(DrinkOption("espresso", "Эспрессо"), DrinkOption("latte", "Латте"))) }
        return ChangeEditorViewModel("cafe", section, requestId, repository, catalogs, account).also { store.put("editor-$section", it); runCurrent() }
    }

    private fun ChangeEditorViewModel.send(vararg actions: ChangeEditorAction, scope: TestScope) { actions.forEach { onAction(it); scope.runCurrent() } }
    private fun photo(n: Int) = PendingPhoto("p$n.jpg", "image/jpeg", byteArrayOf(n.toByte()))

    @Test fun aGuestSeesNothingToEditAndNothingIsLoaded() = runTest(dispatcher) {
        val vm = model(ChangeSection.Description, account = FakeAccount(null))
        assertEquals(false, vm.state.value.signedIn); assertFalse(vm.state.value.isLoading)
        vm.send(ChangeEditorAction.Description("x"), scope = this)
        assertEquals("", vm.state.value.description)
    }

    @Test fun theEditorStartsFromTheShopAsItIsNow() = runTest(dispatcher) {
        val vm = model(ChangeSection.Menu)
        val state = vm.state.value
        assertEquals("Cafe", state.shopTitle); assertEquals("Old text", state.description)
        assertEquals("+375291112233", state.phone); assertEquals(listOf("p1", "p2"), state.retainedPhotos.map { it.id })
        assertEquals(setOf("wifi"), state.tagIds)
        assertEquals(listOf(MenuRowState("espresso", "Эспрессо", Availability.Present, "4.5"), MenuRowState("latte", "Латте")), state.menuRows)
        assertEquals(listOf("m1"), state.retainedMenuPhotos.map { it.id })
    }

    @Test fun anUnchangedSectionCanBeSentAndDescriptionIsCutAtTheLimit() = runTest(dispatcher) {
        val vm = model(ChangeSection.Description)
        assertTrue(vm.state.value.canSubmit)
        vm.send(ChangeEditorAction.Description("x".repeat(2000)), scope = this)
        assertEquals(ContributionLimits.DESCRIPTION, vm.state.value.description.length)
        assertTrue(vm.state.value.canSubmit)
    }

    @Test fun contactsAreCheckedAndAnInstagramLinkIsAccepted() = runTest(dispatcher) {
        val vm = model(ChangeSection.Contacts)
        assertTrue(vm.state.value.canSubmit)
        vm.send(ChangeEditorAction.Phone("abc"), scope = this)
        assertFalse(vm.state.value.canSubmit); assertEquals(1, vm.state.value.contactIssues.size)
        vm.send(ChangeEditorAction.Phone(" +375 29 111 22 33 "), scope = this)
        assertTrue(vm.state.value.canSubmit)
    }

    @Test fun photosStopAtTheLimitCountingSavedAndUploadedOnes() = runTest(dispatcher) {
        val vm = model(ChangeSection.Photos)
        vm.send(ChangeEditorAction.AddPhotos((1..12).map(::photo)), scope = this)
        assertEquals(ContributionLimits.SHOP_PHOTOS - 2, vm.state.value.newPhotos.size)
        vm.send(ChangeEditorAction.RemoveRetainedPhoto("p1"), ChangeEditorAction.RemoveNewPhoto(0), scope = this)
        assertEquals(listOf("p2"), vm.state.value.retainedPhotos.map { it.id })
        assertEquals(ContributionLimits.SHOP_PHOTOS - 3, vm.state.value.newPhotos.size)
    }

    @Test fun menuPhotosStopAtTheirOwnLimit() = runTest(dispatcher) {
        val vm = model(ChangeSection.Menu)
        vm.send(ChangeEditorAction.AddMenuPhotos((1..9).map(::photo)), scope = this)
        assertEquals(ContributionLimits.MENU_PHOTOS - 1, vm.state.value.newMenuPhotos.size)
    }

    @Test fun onlyTheSectionOfTheEditorIsSent() = runTest(dispatcher) {
        val vm = model(ChangeSection.Tags)
        vm.send(ChangeEditorAction.Toggle("wifi"), ChangeEditorAction.Toggle("quiet"), ChangeEditorAction.Description("changed"), ChangeEditorAction.Submit, scope = this)
        assertEquals<List<ChangeDraft>>(listOf(ChangeDraft.Selection("cafe", ChangeSection.Tags, listOf("quiet"))), repository.drafts)
        assertIs<EditorSubmit.Done>(vm.state.value.submit)
    }

    @Test fun theMenuIsSentWithEveryDrinkAndTheCommaPrice() = runTest(dispatcher) {
        val vm = model(ChangeSection.Menu)
        vm.send(ChangeEditorAction.SetAvailability("latte", Availability.Absent), ChangeEditorAction.SetPrice("espresso", "5,25"), ChangeEditorAction.Submit, scope = this)
        val draft = assertIs<ChangeDraft.Menu>(repository.drafts.single())
        assertEquals(listOf(MenuEntry("espresso", Availability.Present, 5.25), MenuEntry("latte", Availability.Absent, null)), draft.items)
        assertEquals(listOf("m1"), draft.retainedPhotoIds)
    }

    @Test fun descriptionAndContactsAreSentTrimmedAsTheirOwnDrafts() = runTest(dispatcher) {
        val contacts = model(ChangeSection.Contacts)
        contacts.send(ChangeEditorAction.Email(" a@b.by "), ChangeEditorAction.Website(""), ChangeEditorAction.Submit, scope = this)
        val sent = assertIs<ChangeDraft.ContactsChange>(repository.drafts.single())
        assertEquals(Contacts("+375291112233", "a@b.by", null, "https://instagram.com/cafe"), sent.contacts)
    }

    @Test fun editingLaysTheRequestOverTheShopAndSavesWithAnUpdate() = runTest(dispatcher) {
        val uploaded = UploadedPhoto("n.jpg", "image/jpeg", "key", 10)
        repository.change = {
            Result.success(request(it, ChangeSection.Photos, ChangePayload(photos = ChangePhotos(retainedIds = listOf("p2", "gone"), uploaded = listOf(uploaded)))))
        }
        val vm = model(ChangeSection.Photos, requestId = "req")
        assertEquals(listOf("p2"), vm.state.value.retainedPhotos.map { it.id })
        assertEquals(listOf(uploaded), vm.state.value.uploadedPhotos)
        vm.send(ChangeEditorAction.Submit, scope = this)
        assertTrue("edit:req" in repository.requests)
        assertEquals(ChangeDraft.Photos("cafe", listOf("p2"), emptyList(), listOf(uploaded)), repository.drafts.single())
    }

    @Test fun editingTakesTheRequestsMenuAndDescription() = runTest(dispatcher) {
        repository.change = {
            Result.success(request(it, ChangeSection.Menu, ChangePayload(menu = ChangeMenu(items = listOf(MenuEntry("latte", Availability.Present, 6.0)), retainedPhotoIds = emptyList()))))
        }
        val vm = model(ChangeSection.Menu, requestId = "req")
        assertEquals(listOf(MenuRowState("espresso", "Эспрессо"), MenuRowState("latte", "Латте", Availability.Present, "6")), vm.state.value.menuRows)
        assertTrue(vm.state.value.retainedMenuPhotos.isEmpty())
    }

    @Test fun aFailedLoadCanBeRetriedAndShowsTheServerMessage() = runTest(dispatcher) {
        catalogs.catalogs = { Result.failure(ContributionFailure(FailureKind.Other, "Сервис недоступен")) }
        val vm = ChangeEditorViewModel("cafe", ChangeSection.Tags, null, repository, catalogs, FakeAccount()).also { store.put("e", it); runCurrent() }
        assertEquals(EditorLoadError.Failed, vm.state.value.loadError); assertEquals("Сервис недоступен", vm.state.value.loadMessage)
        assertFalse(vm.state.value.canSubmit)
        catalogs.catalogs = { Result.success(Catalogs(tags = listOf(CatalogOption("wifi", "Wi-Fi")))) }
        catalogs.shop = { Result.success(shop) }
        vm.send(ChangeEditorAction.Reload, scope = this)
        assertNull(vm.state.value.loadError); assertEquals(1, vm.state.value.catalogs.tags.size)
    }

    @Test fun aFailedDrinksReadFailsTheMenuEditorInsteadOfShowingAnEmptyMenu() = runTest(dispatcher) {
        repository.change = {
            Result.success(request(it, ChangeSection.Menu, ChangePayload(menu = ChangeMenu(items = listOf(MenuEntry("espresso", Availability.Present, 4.5)), retainedPhotoIds = listOf("m1")))))
        }
        val vm = model(ChangeSection.Menu, requestId = "req")
        catalogs.drinks = { Result.failure(ContributionFailure(FailureKind.Other, "Сервис недоступен")) }
        vm.send(ChangeEditorAction.Reload, scope = this)
        assertEquals(EditorLoadError.Failed, vm.state.value.loadError)
        assertFalse(vm.state.value.canSubmit)
        // A retry brings the saved items back, so a photo-only change keeps them.
        catalogs.drinks = { Result.success(listOf(DrinkOption("espresso", "Эспрессо"), DrinkOption("latte", "Латте"))) }
        vm.send(ChangeEditorAction.Reload, scope = this)
        assertNull(vm.state.value.loadError)
        vm.send(ChangeEditorAction.Submit, scope = this)
        val sent = repository.drafts.single() as ChangeDraft.Menu
        assertEquals(MenuEntry("espresso", Availability.Present, 4.5), sent.items.first { it.slug == "espresso" })
    }

    @Test fun anItemWhoseDrinkLeftTheCatalogKeepsItsRowAndIsSentAgain() = runTest(dispatcher) {
        repository.change = {
            Result.success(request(it, ChangeSection.Menu, ChangePayload(menu = ChangeMenu(items = listOf(MenuEntry("retired", Availability.Present, 3.0)), retainedPhotoIds = emptyList()))))
        }
        val vm = model(ChangeSection.Menu, requestId = "req")
        assertTrue(MenuRowState("retired", "retired", Availability.Present, "3") in vm.state.value.menuRows)
        vm.send(ChangeEditorAction.Submit, scope = this)
        assertTrue(MenuEntry("retired", Availability.Present, 3.0) in (repository.drafts.single() as ChangeDraft.Menu).items)
    }

    @Test fun aSuccessfulEmptyDrinkCatalogIsAllowedWhenTheShopHasNoMenu() = runTest(dispatcher) {
        catalogs.shop = { Result.success(shop.copy(menu = emptyList())) }
        catalogs.drinks = { Result.success(emptyList()) }
        val vm = ChangeEditorViewModel("cafe", ChangeSection.Menu, null, repository, catalogs, FakeAccount()).also { store.put("empty", it); runCurrent() }
        assertNull(vm.state.value.loadError); assertTrue(vm.state.value.menuRows.isEmpty())
        assertTrue(vm.state.value.canSubmit)
    }

    @Test fun aMissingRequestIsNotFound() = runTest(dispatcher) {
        val vm = model(ChangeSection.Tags, requestId = "missing")
        assertEquals(EditorLoadError.NotFound, vm.state.value.loadError)
    }

    @Test fun aRefusalKeepsTheFormAndShowsTheServerMessage() = runTest(dispatcher) {
        repository.changeSubmit = { Result.failure(ContributionFailure(FailureKind.Invalid, "Заявка не принята")) }
        val vm = model(ChangeSection.Description)
        vm.send(ChangeEditorAction.Description("new"), ChangeEditorAction.Submit, scope = this)
        assertEquals(EditorSubmit.Failed("Заявка не принята"), vm.state.value.submit)
        assertEquals("new", vm.state.value.description)
        vm.send(ChangeEditorAction.DismissFailure, scope = this)
        assertEquals(EditorSubmit.Idle, vm.state.value.submit)
    }

    @Test fun aSecondSubmitWhileSendingIsIgnored() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<ChangeRequest>>()
        repository.changeSubmit = { gate.await() }
        val vm = model(ChangeSection.Description)
        vm.send(ChangeEditorAction.Submit, ChangeEditorAction.Submit, scope = this)
        assertEquals(EditorSubmit.Sending, vm.state.value.submit)
        assertEquals(1, repository.drafts.size)
        gate.complete(Result.success(request("new")))
        runCurrent()
        assertIs<EditorSubmit.Done>(vm.state.value.submit)
    }

    @Test fun anAccountChangeStartsAnEmptyEditorAndDropsTheLateAnswer() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<ChangeRequest>>()
        repository.changeSubmit = { gate.await() }
        val account = FakeAccount("one")
        val vm = model(ChangeSection.Description, account = account)
        vm.send(ChangeEditorAction.Submit, scope = this)
        account.key.value = "two"; runCurrent()
        gate.complete(Result.success(request("late"))); runCurrent()
        assertEquals(EditorSubmit.Idle, vm.state.value.submit)
        assertEquals("Old text", vm.state.value.description)
    }

    @Test fun suggestListShowsTheShopNameAndSurvivesALoadFailure() = runTest(dispatcher) {
        catalogs.shop = { Result.success(shop) }
        val named = SuggestChangeViewModel("cafe", catalogs, FakeAccount()).also { store.put("s1", it); runCurrent() }
        assertEquals("Cafe", named.state.value.shopTitle); assertFalse(named.state.value.isLoading)
        catalogs.shop = { Result.failure(ContributionFailure(FailureKind.Other)) }
        val unnamed = SuggestChangeViewModel("cafe", catalogs, FakeAccount()).also { store.put("s2", it); runCurrent() }
        assertNull(unnamed.state.value.shopTitle); assertFalse(unnamed.state.value.isLoading)
        val guest = SuggestChangeViewModel("cafe", catalogs, FakeAccount(null)).also { store.put("s3", it); runCurrent() }
        assertEquals(false, guest.state.value.signedIn)
    }
}
