package com.coffeepeek.feature.contributions.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.contributions.api.GeoPoint
import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.domain.rules.ShopBasicsIssue
import com.coffeepeek.feature.contributions.impl.shop.*
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
class AddShopViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private val minsk = CatalogOption("minsk", "Минск")
    private val brest = CatalogOption("brest", "Брест")
    private val repository = FakeRepository()
    private val catalogs = FakeCatalogs()
    private val location = FakeLocation()

    private fun TestScope.model(account: FakeAccount = FakeAccount()) =
        AddShopViewModel(repository, catalogs, location, account).also { store.put("shop", it); runCurrent() }

    private fun AddShopViewModel.send(vararg actions: AddShopAction, scope: TestScope) { actions.forEach { onAction(it); scope.runCurrent() } }
    private fun photo(n: Int) = PendingPhoto("p$n.jpg", "image/jpeg", byteArrayOf(n.toByte()))
    private fun withCities(vararg cities: CatalogOption, rest: Catalogs = Catalogs()) { catalogs.catalogs = { Result.success(rest.copy(cities = cities.toList())) } }
    private fun TestScope.toSchedule(vm: AddShopViewModel) {
        vm.send(AddShopAction.Name("Coffee"), AddShopAction.Address("Улица, 1"), AddShopAction.City("minsk"), scope = this)
        repeat(4) { vm.send(AddShopAction.Next, scope = this) }
        assertEquals(AddShopStep.Schedule, vm.state.value.step)
    }

    @Test fun aGuestCannotFillTheFormAndNothingIsLoaded() = runTest(dispatcher) {
        val vm = model(FakeAccount(null))
        assertEquals(false, vm.state.value.signedIn); assertFalse(vm.state.value.catalogsLoading)
        vm.send(AddShopAction.Name("x"), scope = this)
        assertEquals("", vm.state.value.name)
    }

    @Test fun nameIsCutAtTheLimitAndEachStepWaitsForItsOwnFields() = runTest(dispatcher) {
        withCities(minsk, brest)
        val vm = model()
        vm.send(AddShopAction.Name("x".repeat(80)), AddShopAction.Address("Улица, 1"), scope = this)
        assertEquals(ContributionLimits.SHOP_NAME, vm.state.value.name.length)
        vm.send(AddShopAction.Next, scope = this)
        assertEquals(AddShopStep.Basic, vm.state.value.step)
        assertEquals(listOf(ShopBasicsIssue.CityMissing), vm.state.value.basicsIssues)
        vm.send(AddShopAction.City("minsk"), AddShopAction.Next, scope = this)
        assertEquals(AddShopStep.Contacts, vm.state.value.step)
        vm.send(AddShopAction.Phone("abc"), AddShopAction.Email("nope"), AddShopAction.Next, scope = this)
        assertEquals(AddShopStep.Contacts, vm.state.value.step); assertFalse(vm.state.value.contactsValid)
        vm.send(AddShopAction.Phone(""), AddShopAction.Email(""), AddShopAction.Instagram("https://instagram.com/shop"), scope = this)
        assertTrue(vm.state.value.contactsValid)
        repeat(3) { vm.send(AddShopAction.Next, scope = this) }
        assertEquals(AddShopStep.Schedule, vm.state.value.step)
        vm.send(AddShopAction.Previous, scope = this)
        assertEquals(AddShopStep.Features, vm.state.value.step)
    }

    @Test fun aSingleCityIsChosenAutomaticallyAndSeveralAreNot() = runTest(dispatcher) {
        withCities(minsk)
        assertEquals("minsk", model().state.value.cityId)
        withCities(minsk, brest)
        assertNull(model().state.value.cityId)
    }

    @Test fun catalogFailureAllowsARetry() = runTest(dispatcher) {
        var attempts = 0
        catalogs.catalogs = { if (++attempts == 1) Result.failure(IllegalStateException("net")) else Result.success(Catalogs(cities = listOf(minsk, brest))) }
        val vm = model()
        assertTrue(vm.state.value.catalogsFailed); assertFalse(vm.state.value.catalogsLoading)
        vm.send(AddShopAction.RetryCatalogs, scope = this)
        assertFalse(vm.state.value.catalogsFailed); assertEquals(2, vm.state.value.catalogs.cities.size)
    }

    @Test fun aPickedPointFillsAddressAndCoordinatesAndAMatchingCityButKeepsAChosenOne() = runTest(dispatcher) {
        withCities(minsk, brest)
        val vm = model()
        vm.send(AddShopAction.MapPicked(52.09, 23.69, "Брест, ул. Мира, 5"), scope = this)
        with(vm.state.value) { assertEquals("brest", cityId); assertEquals(52.09, latitude); assertEquals("Брест, ул. Мира, 5", address); assertFalse(mapPickerOpen) }
        vm.send(AddShopAction.City("minsk"), AddShopAction.MapPicked(52.1, 23.7, "Брест, другая"), scope = this)
        assertEquals("minsk", vm.state.value.cityId)
        vm.send(AddShopAction.Address("Новый"), scope = this)
        assertNull(vm.state.value.latitude); assertNull(vm.state.value.longitude)
        vm.send(AddShopAction.OpenMap, scope = this); assertTrue(vm.state.value.mapPickerOpen)
        vm.send(AddShopAction.DismissMap, scope = this); assertFalse(vm.state.value.mapPickerOpen)
    }

    @Test fun theCurrentLocationFillsAnEmptyAddressOnlyAndReportsWhatWentWrong() = runTest(dispatcher) {
        withCities(minsk, brest)
        val vm = model()
        vm.send(AddShopAction.LocationGranted, scope = this)
        assertEquals(LocationHint.NotFound, vm.state.value.locationHint); assertFalse(vm.state.value.resolvingAddress)

        location.point = GeoPoint(53.9, 27.5)
        vm.send(AddShopAction.LocationGranted, scope = this)
        assertEquals(LocationHint.NoAddress, vm.state.value.locationHint); assertEquals(53.9, vm.state.value.latitude); assertEquals("", vm.state.value.address)

        location.address = "Минск, Ленина 1"
        vm.send(AddShopAction.LocationGranted, scope = this)
        with(vm.state.value) { assertEquals("Минск, Ленина 1", address); assertEquals("minsk", cityId); assertNull(locationHint) }

        vm.send(AddShopAction.Address("Мой адрес"), AddShopAction.LocationGranted, scope = this)
        // The phone position is not attached to an address the user typed.
        with(vm.state.value) { assertEquals("Мой адрес", address); assertNull(latitude); assertNull(longitude) }
        vm.send(AddShopAction.LocationDenied, scope = this)
        assertEquals(LocationHint.PermissionDenied, vm.state.value.locationHint)
    }

    @Test fun aLateAutomaticLookupNeverChangesAnAddressTheUserTypedMeanwhile() = runTest(dispatcher) {
        withCities(minsk, brest)
        location.point = GeoPoint(53.9, 27.5); location.address = "Минск, Ленина 1"
        val gate = CompletableDeferred<Unit>(); location.addressGate = gate
        val vm = model()
        vm.send(AddShopAction.LocationGranted, scope = this)
        vm.send(AddShopAction.Address("Брест, Советская 2"), scope = this)
        gate.complete(Unit); runCurrent()
        with(vm.state.value) { assertEquals("Брест, Советская 2", address); assertNull(latitude); assertNull(longitude); assertFalse(resolvingAddress) }
    }

    @Test fun aLateAutomaticLookupNeverReplacesAPointPickedOnTheMap() = runTest(dispatcher) {
        withCities(minsk, brest)
        location.point = GeoPoint(53.9, 27.5); location.address = null
        val gate = CompletableDeferred<Unit>(); location.addressGate = gate
        val vm = model()
        vm.send(AddShopAction.LocationGranted, scope = this)
        vm.send(AddShopAction.MapPicked(52.1, 23.7, "Брест, Советская 2"), scope = this)
        gate.complete(Unit); runCurrent()
        with(vm.state.value) { assertEquals("Брест, Советская 2", address); assertEquals(52.1, latitude); assertEquals(23.7, longitude); assertNull(locationHint) }
    }

    @Test fun photoLimitsAndRemovalByIndex() = runTest(dispatcher) {
        val vm = model()
        vm.send(AddShopAction.AddPhotos(List(ContributionLimits.SHOP_PHOTOS + 3) { photo(it) }), scope = this)
        assertEquals(ContributionLimits.SHOP_PHOTOS, vm.state.value.photos.size)
        vm.send(AddShopAction.AddPhotos(listOf(photo(99))), AddShopAction.RemovePhoto(0), scope = this)
        assertEquals("p1.jpg", vm.state.value.photos.first().fileName); assertEquals(ContributionLimits.SHOP_PHOTOS - 1, vm.state.value.photos.size)
        vm.send(AddShopAction.AddMenuPhotos(List(6) { photo(it) }), AddShopAction.RemoveMenuPhoto(1), scope = this)
        assertEquals(listOf("p0.jpg", "p2.jpg", "p3.jpg"), vm.state.value.menuPhotos.map { it.fileName })
    }

    @Test fun unifiedScheduleIsSentAsSevenDaysMondayFirstWithClosedDaysEmpty() = runTest(dispatcher) {
        withCities(minsk)
        val sent = mutableListOf<NewShop>()
        repository.shopSubmit = { sent += it; Result.success(Unit) }
        val vm = model()
        vm.send(AddShopAction.ToggleClosedDay(0), AddShopAction.ToggleClosedDay(3), AddShopAction.Preset("08:00", "20:00"), AddShopAction.AdjustClose(30), scope = this)
        toSchedule(vm)
        vm.send(AddShopAction.Next, scope = this)
        val week = sent.single().week
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 0), week.map { it.day })
        assertEquals(listOf(3, 0), week.filter { it.closed }.map { it.day })
        assertEquals(TimeInterval("08:00", "20:30"), week.first().intervals.single())
        assertEquals(SubmitState.Done, vm.state.value.submit)
    }

    @Test fun perDayScheduleStartsFromTheUnifiedOneAndKeepsEachDay() = runTest(dispatcher) {
        withCities(minsk)
        val sent = mutableListOf<NewShop>()
        repository.shopSubmit = { sent += it; Result.success(Unit) }
        val vm = model()
        vm.send(AddShopAction.ToggleClosedDay(2), AddShopAction.Preset("10:00", "22:00"), AddShopAction.PerDay(true), scope = this)
        assertTrue(vm.state.value.days.single { it.day == 2 }.closed)
        assertTrue(vm.state.value.days.filter { it.day != 2 }.all { it.open == "10:00" && it.close == "22:00" })
        vm.send(AddShopAction.AdjustDayOpen(5, -60), AddShopAction.AdjustDayClose(5, 120), AddShopAction.SetDayOpen(2, true), AddShopAction.SetDayOpen(4, false), scope = this)
        toSchedule(vm); vm.send(AddShopAction.Next, scope = this)
        val week = sent.single().week
        assertEquals(TimeInterval("09:00", "23:59"), week.single { it.day == 5 }.intervals.single())
        assertEquals(TimeInterval("10:00", "22:00"), week.single { it.day == 2 }.intervals.single())
        assertTrue(week.single { it.day == 4 }.closed)
        vm.send(AddShopAction.PerDay(false), scope = this); assertFalse(vm.state.value.perDay)
    }

    @Test fun theSubmittedShopHasTrimmedTextBlankOptionalsAsNullAndTheSelections() = runTest(dispatcher) {
        catalogs.catalogs = { Result.success(Catalogs(cities = listOf(minsk), beans = listOf(CatalogOption("arabica", "Арабика")))) }
        val sent = mutableListOf<NewShop>()
        repository.shopSubmit = { sent += it; Result.success(Unit) }
        val vm = model()
        vm.send(
            AddShopAction.Name("  Кофейня  "), AddShopAction.Address("  Улица, 1 "), AddShopAction.Description("   "), AddShopAction.Price(PriceRange.Moderate),
            AddShopAction.Email("shop@example.com"), AddShopAction.ToggleBean("arabica"), AddShopAction.ToggleEquipment("grinder"), AddShopAction.ToggleEquipment("grinder"),
            AddShopAction.ToggleRoaster("roast"), AddShopAction.ToggleBrewMethod("v60"), AddShopAction.AddPhotos(listOf(photo(1))), AddShopAction.AddMenuPhotos(listOf(photo(2))),
            scope = this,
        )
        repeat(5) { vm.send(AddShopAction.Next, scope = this) }
        val shop = sent.single()
        assertEquals("Кофейня", shop.name); assertEquals("Улица, 1", shop.address); assertEquals("minsk", shop.cityId); assertNull(shop.description)
        assertEquals(PriceRange.Moderate, shop.priceRange); assertEquals(Contacts("", "shop@example.com", "", ""), shop.contacts)
        assertEquals(listOf("arabica"), shop.beanIds); assertTrue(shop.equipmentIds.isEmpty()); assertEquals(listOf("roast"), shop.roasterIds); assertEquals(listOf("v60"), shop.brewMethodIds)
        assertEquals(1, shop.photos.size); assertEquals(1, shop.menuPhotos.size)
    }

    @Test fun aFailedSubmissionKeepsTheFormShowsTheServerMessageAndCanRetryOnce() = runTest(dispatcher) {
        withCities(minsk)
        val gate = CompletableDeferred<Result<Unit>>()
        var calls = 0
        repository.shopSubmit = { calls++; gate.await() }
        val vm = model()
        toSchedule(vm)
        vm.send(AddShopAction.Next, scope = this)
        assertEquals(SubmitState.Sending, vm.state.value.submit)
        repeat(5) { vm.send(AddShopAction.Next, AddShopAction.Previous, scope = this) }
        assertEquals(AddShopStep.Schedule, vm.state.value.step); assertEquals(1, calls)
        gate.complete(Result.failure(ContributionFailure(FailureKind.Other, "Кофейня уже существует"))); runCurrent()
        assertEquals(SubmitState.Failed("Кофейня уже существует"), vm.state.value.submit); assertEquals("Coffee", vm.state.value.name)

        repository.shopSubmit = { calls++; Result.failure(IllegalStateException("socket closed")) }
        vm.send(AddShopAction.Next, scope = this)
        assertEquals(SubmitState.Failed(null), vm.state.value.submit); assertEquals(2, calls)
        vm.send(AddShopAction.DismissFailure, scope = this)
        assertEquals(SubmitState.Idle, vm.state.value.submit)
        repository.shopSubmit = { calls++; Result.success(Unit) }
        vm.send(AddShopAction.Next, scope = this)
        assertEquals(SubmitState.Done, vm.state.value.submit)
        vm.send(AddShopAction.Next, scope = this)
        assertEquals(3, calls)
    }

    @Test fun anotherAccountStartsAnEmptyFormAndALateAnswerIsDropped() = runTest(dispatcher) {
        withCities(minsk)
        val slow = CompletableDeferred<Result<Unit>>()
        repository.shopSubmit = { slow.await() }
        val account = FakeAccount("one")
        val vm = model(account)
        toSchedule(vm)
        vm.send(AddShopAction.Next, scope = this)
        assertEquals(SubmitState.Sending, vm.state.value.submit)

        account.key.value = "two"; runCurrent()
        assertEquals("", vm.state.value.name); assertEquals(AddShopStep.Basic, vm.state.value.step); assertEquals(SubmitState.Idle, vm.state.value.submit)
        slow.complete(Result.success(Unit)); runCurrent()
        assertEquals(SubmitState.Idle, vm.state.value.submit)

        account.key.value = null; runCurrent()
        assertEquals(false, vm.state.value.signedIn)
    }
}
