package com.coffeepeek.feature.contributions.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.domain.rules.RoasterIssue
import com.coffeepeek.feature.contributions.impl.roaster.*
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
class AddRoasterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private val minsk = CatalogOption("minsk", "Минск")
    private val brest = CatalogOption("brest", "Брест")
    private val repository = FakeRepository()
    private val catalogs = FakeCatalogs()

    private fun TestScope.model(account: FakeAccount = FakeAccount()) =
        AddRoasterViewModel(repository, catalogs, account).also { store.put("roaster", it); runCurrent() }

    private fun AddRoasterViewModel.send(vararg actions: AddRoasterAction, scope: TestScope) { actions.forEach { onAction(it); scope.runCurrent() } }
    private fun photo(n: Int) = PendingPhoto("p$n.jpg", "image/jpeg", byteArrayOf(n.toByte()))
    private fun withCities(vararg cities: CatalogOption) { catalogs.catalogs = { Result.success(Catalogs(cities = cities.toList())) } }

    @Test fun aGuestCannotFillTheFormAndNothingIsLoaded() = runTest(dispatcher) {
        val vm = model(FakeAccount(null))
        assertEquals(false, vm.state.value.signedIn); assertFalse(vm.state.value.citiesLoading)
        vm.send(AddRoasterAction.Name("x"), scope = this)
        assertEquals("", vm.state.value.name)
    }

    @Test fun onlyTheNameIsRequiredAndItIsCutAtTheLimit() = runTest(dispatcher) {
        withCities(minsk, brest)
        val vm = model()
        assertFalse(vm.state.value.valid)
        vm.send(AddRoasterAction.Name("x".repeat(150)), scope = this)
        assertEquals(ContributionLimits.ROASTER_NAME, vm.state.value.name.length)
        assertTrue(vm.state.value.valid)
    }

    @Test fun theCityAndTheAddressComeTogether() = runTest(dispatcher) {
        withCities(minsk, brest)
        val vm = model()
        vm.send(AddRoasterAction.Name("Roast"), AddRoasterAction.Address("Улица, 1"), scope = this)
        assertEquals(setOf(RoasterIssue.CityMissing), vm.state.value.issues)
        vm.send(AddRoasterAction.City("brest"), scope = this)
        assertTrue(vm.state.value.valid)
        vm.send(AddRoasterAction.Address(""), scope = this)
        assertEquals(setOf(RoasterIssue.AddressMissing), vm.state.value.issues)
    }

    @Test fun theOnlyCityIsChosenWithTheAddress() = runTest(dispatcher) {
        withCities(minsk)
        val vm = model()
        vm.send(AddRoasterAction.Name("Roast"), scope = this)
        assertNull(vm.state.value.cityId)
        vm.send(AddRoasterAction.Address("Улица, 1"), scope = this)
        assertEquals("minsk", vm.state.value.cityId)
        assertTrue(vm.state.value.valid)
    }

    @Test fun contactsAreChecked() = runTest(dispatcher) {
        val vm = model()
        vm.send(AddRoasterAction.Name("Roast"), AddRoasterAction.Instagram("bad handle!"), AddRoasterAction.Website("not a site"), scope = this)
        assertEquals(setOf(RoasterIssue.Instagram, RoasterIssue.Website), vm.state.value.issues)
    }

    @Test fun photosStopAtTheLimit() = runTest(dispatcher) {
        val vm = model()
        vm.send(AddRoasterAction.AddPhotos((1..4).map(::photo)), AddRoasterAction.AddPhotos((5..9).map(::photo)), scope = this)
        assertEquals(ContributionLimits.ROASTER_PHOTOS, vm.state.value.photos.size)
        vm.send(AddRoasterAction.RemovePhoto(0), scope = this)
        assertEquals(4, vm.state.value.photos.size)
    }

    @Test fun anInvalidFormIsNotSent() = runTest(dispatcher) {
        val vm = model()
        vm.send(AddRoasterAction.Submit, scope = this)
        assertEquals(RoasterSubmitState.Idle, vm.state.value.submit)
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun theRoasterIsSentTrimmedWithTheInstagramLinkAndWithoutBlankParts() = runTest(dispatcher) {
        withCities(minsk, brest)
        var sent: NewRoaster? = null
        repository.roasterSubmit = { sent = it; Result.success(RoasterSubmitted("r1", "Pending", true, "Принято")) }
        val vm = model()
        vm.send(
            AddRoasterAction.Name("  Roast  "), AddRoasterAction.About("   "), AddRoasterAction.Instagram("@roast"), AddRoasterAction.Website(" roast.by "),
            AddRoasterAction.AddPhotos(listOf(photo(1))), AddRoasterAction.Submit, scope = this,
        )
        assertEquals(
            NewRoaster("Roast", about = null, cityId = null, address = null, instagramLink = "https://instagram.com/roast", siteLink = "roast.by", photos = listOf(photo(1))),
            sent,
        )
        assertEquals(RoasterSubmitState.Done(RoasterSubmitted("r1", "Pending", true, "Принято")), vm.state.value.submit)
    }

    @Test fun aSentRoasterCannotBeSentAgainAndAFailureCanBeRetried() = runTest(dispatcher) {
        var calls = 0
        repository.roasterSubmit = { calls++; Result.failure(IllegalStateException("Connection lost")) }
        val vm = model()
        vm.send(AddRoasterAction.Name("Roast"), AddRoasterAction.Submit, scope = this)
        assertEquals(RoasterSubmitState.Failed(null), vm.state.value.submit); assertEquals("Roast", vm.state.value.name)
        repository.roasterSubmit = { calls++; Result.success(RoasterSubmitted("r1", "Pending", false, "Адрес проверит модератор")) }
        vm.send(AddRoasterAction.Submit, scope = this)
        assertEquals(RoasterSubmitState.Done(RoasterSubmitted("r1", "Pending", false, "Адрес проверит модератор")), vm.state.value.submit)
        assertEquals(2, calls)
        vm.send(AddRoasterAction.Submit, AddRoasterAction.Submit, scope = this)
        assertEquals(2, calls, "nothing is sent after a success")
    }

    @Test fun theCityIsSentOnlyWithTheAddress() = runTest(dispatcher) {
        withCities(minsk, brest)
        var sent: NewRoaster? = null
        repository.roasterSubmit = { sent = it; Result.success(RoasterSubmitted("r1", "Pending", true, "")) }
        val vm = model()
        vm.send(AddRoasterAction.Name("Roast"), AddRoasterAction.City("brest"), AddRoasterAction.Address("Улица, 5"), AddRoasterAction.Submit, scope = this)
        assertEquals("brest", sent?.cityId); assertEquals("Улица, 5", sent?.address)
    }

    @Test fun aRefusalKeepsTheFormAndShowsTheServerMessage() = runTest(dispatcher) {
        repository.roasterSubmit = { Result.failure(ContributionFailure(FailureKind.Invalid, "Укажите название; город и адрес вместе")) }
        val vm = model()
        vm.send(AddRoasterAction.Name("Roast"), AddRoasterAction.Submit, scope = this)
        assertEquals(RoasterSubmitState.Failed("Укажите название; город и адрес вместе"), vm.state.value.submit)
        assertEquals("Roast", vm.state.value.name)
        vm.send(AddRoasterAction.DismissFailure, scope = this)
        assertEquals(RoasterSubmitState.Idle, vm.state.value.submit)
    }

    @Test fun aSecondSubmitWhileSendingIsIgnored() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<RoasterSubmitted>>()
        var calls = 0
        repository.roasterSubmit = { calls++; gate.await() }
        val vm = model()
        vm.send(AddRoasterAction.Name("Roast"), AddRoasterAction.Submit, AddRoasterAction.Submit, scope = this)
        assertEquals(RoasterSubmitState.Sending, vm.state.value.submit)
        assertEquals(1, calls)
        gate.complete(Result.success(RoasterSubmitted("r1", "Pending", false, "")))
        runCurrent()
        assertIs<RoasterSubmitState.Done>(vm.state.value.submit)
    }

    @Test fun anAccountChangeStartsAnEmptyFormAndDropsTheLateAnswer() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<RoasterSubmitted>>()
        repository.roasterSubmit = { gate.await() }
        val account = FakeAccount("one")
        val vm = model(account)
        vm.send(AddRoasterAction.Name("Roast"), AddRoasterAction.Submit, scope = this)
        account.key.value = "two"; runCurrent()
        gate.complete(Result.success(RoasterSubmitted("r1", "Pending", false, ""))); runCurrent()
        assertEquals(RoasterSubmitState.Idle, vm.state.value.submit)
        assertEquals("", vm.state.value.name)
    }

    @Test fun theRoasterCannotBeSentWhileTheCityListLoadsAndAFailedListKeepsTheForm() = runTest(dispatcher) {
        val release = CompletableDeferred<Result<Catalogs>>()
        catalogs.catalogs = { release.await() }
        val vm = model()
        vm.send(AddRoasterAction.Name("Roast"), scope = this)
        assertTrue(vm.state.value.citiesLoading); assertTrue(vm.state.value.valid); assertFalse(vm.state.value.ready)
        vm.send(AddRoasterAction.Submit, scope = this)
        assertEquals(RoasterSubmitState.Idle, vm.state.value.submit)
        assertTrue(repository.requests.isEmpty(), "nothing is sent before the list has answered")

        release.complete(Result.failure(ContributionFailure(FailureKind.Other)))
        runCurrent()
        assertTrue(vm.state.value.citiesFailed); assertEquals("Roast", vm.state.value.name); assertTrue(vm.state.value.ready)
        vm.send(AddRoasterAction.Submit, scope = this)
        assertTrue(vm.state.value.submit != RoasterSubmitState.Idle, "a failed list does not block sending")
    }

    @Test fun aFailedCityListCanBeRetried() = runTest(dispatcher) {
        catalogs.catalogs = { Result.failure(ContributionFailure(FailureKind.Other)) }
        val vm = model()
        assertTrue(vm.state.value.citiesFailed)
        withCities(minsk, brest)
        vm.send(AddRoasterAction.RetryCities, scope = this)
        assertFalse(vm.state.value.citiesFailed); assertEquals(2, vm.state.value.cities.size)
    }
}
