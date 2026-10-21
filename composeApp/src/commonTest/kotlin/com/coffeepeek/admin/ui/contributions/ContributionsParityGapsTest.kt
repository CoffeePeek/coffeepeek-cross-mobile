package com.coffeepeek.admin.ui.contributions

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.addshop.AddShopStep
import com.coffeepeek.admin.ui.screen.addshop.AddShopViewModel
import com.coffeepeek.admin.ui.screen.addshop.shiftTime
import com.coffeepeek.admin.ui.screen.contributions.ContributionItem
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.admin.ui.screen.contributions.MyContributionsUiState
import com.coffeepeek.admin.ui.screen.contributions.MyContributionsViewModel
import com.coffeepeek.admin.ui.screen.roaster.AddRoasterViewModel
import com.coffeepeek.admin.ui.screen.shopchange.ShopChangeEditorViewModel
import com.coffeepeek.admin.utils.MAX_MENU_PHOTOS
import com.coffeepeek.admin.utils.MAX_SHOP_PHOTOS
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.ShopChangeRequestRepository
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.*

/** Parity of the legacy Contributions screens' rules that the existing submission tests do not pin. */
class ContributionsParityGapsTest {
    // ---- A. My contributions ----

    @Test fun shopListMapsPublishedWaitingAndRejectedSubmissionsAndOnlyStatusesWithItemsGetATab() = runBlocking {
        val shops = Shops().apply {
            submissions = { status, _ ->
                when (status) {
                    ModerationStatus.Approved -> page(
                        ShopSubmission("a", "Published", "Street 1", ModerationStatus.Approved, null, "published-slug"),
                        ShopSubmission("b", "Waiting", "Street 2", ModerationStatus.Approved, null, null),
                    )
                    ModerationStatus.Pending -> page()
                    ModerationStatus.Rejected -> page(ShopSubmission("c", "Rejected", null, ModerationStatus.Rejected, "Нет адреса", null))
                }
            }
        }
        val vm = MyContributionsViewModel(ContributionKind.Shops, shops, Roasters(), Changes())
        try {
            val state = loaded(vm)
            assertEquals(listOf(ModerationStatus.Approved, ModerationStatus.Rejected), state.visibleTabs)
            val approved = state.tabs.getValue(ModerationStatus.Approved).items
            assertEquals(Navigator.Screen.ShopDetail("published-slug"), approved[0].target)
            assertNull(approved[0].meta)
            assertNull(approved[1].target)
            assertEquals("Скоро появится в каталоге", approved[1].meta)
            assertEquals("Street 1", approved[0].subtitle)
            val rejected = state.tabs.getValue(ModerationStatus.Rejected).items.single()
            assertEquals("Нет адреса", rejected.rejectedReason)
            assertNull(rejected.meta)
            assertEquals(2, state.tabs.getValue(ModerationStatus.Approved).totalCount)
        } finally { vm.close() }
    }

    @Test fun roasterListUsesAboutAsSubtitleAndHasNoLinkToThePublishedRoaster() = runBlocking {
        val roasters = Roasters().apply {
            submissions = { status, _ ->
                if (status == ModerationStatus.Approved) page(RoasterSubmission("r", "Roast", "О нас", ModerationStatus.Approved, null)) else page()
            }
        }
        val vm = MyContributionsViewModel(ContributionKind.Roasters, Shops(), roasters, Changes())
        try {
            val item = loaded(vm).tabs.getValue(ModerationStatus.Approved).items.single()
            assertEquals(ContributionItem(id = "r", title = "Roast", subtitle = "О нас"), item)
        } finally { vm.close() }
    }

    @Test fun changeListShowsSectionTitleSummaryDateAndOpensTheRequest() = runBlocking {
        val created = "2026-10-15T07:30:00Z"
        fun request(id: String, section: ShopChangeSection, payload: ShopChangePayload, status: ModerationStatus = ModerationStatus.Pending) =
            ShopChangeRequest(id, "shop", section, payload, status, createdAtUtc = created)
        val changes = Changes().apply {
            mine = { _, _, status ->
                if (status != ModerationStatus.Pending) page() else page(
                    request("d", ShopChangeSection.Description, ShopChangePayload(description = "")),
                    request("c", ShopChangeSection.Contacts, ShopChangePayload(contacts = ShopChangeContacts(email = "a@b.c"))),
                    request("p", ShopChangeSection.Photos, ShopChangePayload(photos = ShopChangeGallery(listOf("1", "2"), listOf(UploadedPhotoMeta("n", "image/jpeg", "k", 1))))),
                    request("t", ShopChangeSection.Tags, ShopChangePayload(tagIds = listOf("a", "b", "c"))),
                    request("r", ShopChangeSection.Roasters, ShopChangePayload(roasterIds = listOf("a"))),
                    request("e", ShopChangeSection.Equipment, ShopChangePayload(equipmentIds = listOf("a", "b"))),
                    request("b", ShopChangeSection.BrewMethods, ShopChangePayload(brewMethodIds = listOf("a"))),
                    request("m", ShopChangeSection.Menu, ShopChangePayload(menu = ShopChangeMenu(items = listOf(ShopChangeMenuItem("x", MenuItemAvailability.Present)), newPhotos = listOf(UploadedPhotoMeta("n", "image/jpeg", "k", 1))))),
                    request("empty", ShopChangeSection.Description, ShopChangePayload()),
                )
            }
        }
        val vm = MyContributionsViewModel(ContributionKind.Changes, Shops(), Roasters(), changes)
        try {
            val items = loaded(vm).tabs.getValue(ModerationStatus.Pending).items.associateBy { it.id }
            assertEquals("Описание очищено", items.getValue("d").subtitle)
            assertEquals("Телефон, почта, сайт и Instagram", items.getValue("c").subtitle)
            assertEquals("Фото: 2 сохранено · 1 добавлено", items.getValue("p").subtitle)
            assertEquals("Выбрано особенностей: 3", items.getValue("t").subtitle)
            assertEquals("Выбрано обжарщиков: 1", items.getValue("r").subtitle)
            assertEquals("Выбрано оборудования: 2", items.getValue("e").subtitle)
            assertEquals("Выбрано методов: 1", items.getValue("b").subtitle)
            assertEquals("Меню: 1 позиций · 1 новых фото", items.getValue("m").subtitle)
            assertEquals("Текст описания заведения", items.getValue("empty").subtitle)
            val first = items.getValue("d")
            assertEquals("Описание", first.title)
            assertEquals(utcIsoToLocalDateTime(created), first.meta)
            assertEquals(ShopChangeSection.Description, first.changeSection)
            assertEquals(Navigator.Screen.ShopChangeRequestDetail(requestId = "d"), first.target)
        } finally { vm.close() }
    }

    @Test fun oneFailingStatusFailsTheWholeListWithItsMessageOrADefault() = runBlocking {
        val shops = Shops().apply {
            submissions = { status, _ -> if (status == ModerationStatus.Pending) Result.failure(IllegalStateException("Сервер недоступен")) else page() }
        }
        val vm = MyContributionsViewModel(ContributionKind.Shops, shops, Roasters(), Changes())
        try {
            val state = withTimeout(5_000) { vm.state.first { !it.isLoading } }
            assertEquals("Сервер недоступен", state.error)
            assertTrue(state.tabs.isEmpty())
        } finally { vm.close() }
        val silent = MyContributionsViewModel(ContributionKind.Shops, Shops().apply { submissions = { _, _ -> Result.failure(Exception()) } }, Roasters(), Changes())
        try { assertEquals("Ошибка загрузки", withTimeout(5_000) { silent.state.first { !it.isLoading } }.error) } finally { silent.close() }
    }

    @Test fun loadMoreAppendsOnePageAtATimeAndAFailureKeepsTheLoadedItems() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var secondPageRequests = 0
        val shops = Shops().apply {
            submissions = { status, number ->
                when {
                    status != ModerationStatus.Approved -> page()
                    number == 1 -> Result.success(PagedResult(listOf(submission("1")), totalCount = 3, totalPages = 3, currentPage = 1))
                    number == 2 -> { secondPageRequests++; gate.await(); Result.success(PagedResult(listOf(submission("2")), 3, 3, 2)) }
                    else -> Result.failure(IllegalStateException("Сбой"))
                }
            }
        }
        val vm = MyContributionsViewModel(ContributionKind.Shops, shops, Roasters(), Changes())
        try {
            loaded(vm)
            vm.loadMore(ModerationStatus.Pending)
            assertEquals(1, vm.state.value.tabs[ModerationStatus.Approved]?.items?.size)
            vm.loadMore(ModerationStatus.Approved)
            assertTrue(vm.state.value.tabs.getValue(ModerationStatus.Approved).isLoadingMore)
            repeat(5) { vm.loadMore(ModerationStatus.Approved) }
            gate.complete(Unit)
            val second = withTimeout(5_000) { vm.state.first { it.tabs[ModerationStatus.Approved]?.items?.size == 2 } }
            assertEquals(1, secondPageRequests)
            assertTrue(second.tabs.getValue(ModerationStatus.Approved).hasMore)
            vm.loadMore(ModerationStatus.Approved)
            val failed = withTimeout(5_000) { vm.state.first { it.tabs[ModerationStatus.Approved]?.isLoadingMore == false && it.tabs[ModerationStatus.Approved]?.page == 2 } }
            assertEquals(listOf("1", "2"), failed.tabs.getValue(ModerationStatus.Approved).items.map { it.id })
            assertNull(failed.error)
        } finally { vm.close() }
    }

    @Test fun lastPageDisablesLoadMore() = runBlocking {
        val shops = Shops().apply {
            submissions = { status, _ -> if (status == ModerationStatus.Approved) Result.success(PagedResult(listOf(submission("1")), 1, 1, 1)) else page() }
        }
        val vm = MyContributionsViewModel(ContributionKind.Shops, shops, Roasters(), Changes())
        try {
            assertFalse(loaded(vm).tabs.getValue(ModerationStatus.Approved).hasMore)
        } finally { vm.close() }
    }

    // ---- B. Add shop wizard ----

    @Test fun nameIsCutAt55AndEachStepWaitsForItsOwnValidFields() = runBlocking {
        val vm = wizard()
        try {
            vm.onNameChange("x".repeat(80))
            assertEquals(55, vm.state.value.name.length)
            vm.onAddressChange("Улица, 1")
            vm.nextStep()
            assertEquals(AddShopStep.BASIC, vm.state.value.currentStep)
            assertEquals("Выберите город", vm.state.value.cityError)
            vm.onCitySelect(City("minsk", "Минск"))
            vm.nextStep()
            assertEquals(AddShopStep.CONTACTS, vm.state.value.currentStep)
            vm.onPhoneChange("abc"); vm.onEmailChange("not-an-email"); vm.onWebsiteChange("bad url"); vm.onInstagramChange("a")
            assertFalse(vm.state.value.contactsStepValid)
            vm.nextStep()
            assertEquals(AddShopStep.CONTACTS, vm.state.value.currentStep)
            vm.onPhoneChange(""); vm.onEmailChange(""); vm.onWebsiteChange(""); vm.onInstagramChange("")
            vm.nextStep(); vm.nextStep(); vm.nextStep()
            assertEquals(AddShopStep.SCHEDULE, vm.state.value.currentStep)
            vm.prevStep()
            assertEquals(AddShopStep.FEATURES, vm.state.value.currentStep)
        } finally { vm.close() }
    }

    @Test fun aSingleCityIsChosenAutomaticallyAndAPickedPointFillsAddressCoordinatesAndAMatchingCity() = runBlocking {
        val single = wizard(ShopCatalogs(cities = listOf(City("minsk", "Минск"))))
        try { assertEquals("minsk", single.state.value.selectedCity?.id) } finally { single.close() }

        val several = wizard(ShopCatalogs(cities = listOf(City("minsk", "Минск"), City("brest", "Брест"))))
        try {
            assertNull(several.state.value.selectedCity)
            several.onLocationPicked(52.09, 23.69, "Брест, ул. Мира, 5")
            val state = several.state.value
            assertEquals("brest", state.selectedCity?.id)
            assertEquals(52.09, state.latitude); assertEquals(23.69, state.longitude)
            assertEquals("Брест, ул. Мира, 5", state.address)
            several.onCitySelect(City("minsk", "Минск"))
            several.onLocationPicked(52.1, 23.7, "Брест, другая улица")
            assertEquals("minsk", several.state.value.selectedCity?.id)
            several.onAddressChange("Новый адрес")
            assertNull(several.state.value.latitude); assertNull(several.state.value.longitude)
        } finally { several.close() }
    }

    @Test fun photoLimitsAndRemovalByIndex() = runBlocking {
        val vm = wizard()
        try {
            vm.addPhotos(List(MAX_SHOP_PHOTOS + 3) { PickedImage(byteArrayOf(it.toByte()), "p$it.jpg") })
            assertEquals(MAX_SHOP_PHOTOS, vm.state.value.photos.size)
            vm.addPhotos(listOf(PickedImage(byteArrayOf(9), "extra.jpg")))
            assertEquals(MAX_SHOP_PHOTOS, vm.state.value.photos.size)
            vm.removePhoto(0)
            assertEquals("p1.jpg", vm.state.value.photos.first().fileName)
            vm.addMenuPhotos(List(MAX_MENU_PHOTOS + 2) { PickedImage(byteArrayOf(it.toByte()), "m$it.jpg") })
            assertEquals(MAX_MENU_PHOTOS, vm.state.value.menuPhotos.size)
            vm.removeMenuPhoto(1)
            assertEquals(listOf("m0.jpg", "m2.jpg", "m3.jpg"), vm.state.value.menuPhotos.map { it.fileName })
        } finally { vm.close() }
    }

    @Test fun timeSteppersClampToTheDay() {
        assertEquals("09:30", shiftTime("09:00", 30))
        assertEquals("00:00", shiftTime("00:10", -30))
        assertEquals("23:59", shiftTime("23:50", 30))
        assertEquals("09:00", shiftTime("broken", 0))
    }

    @Test fun unifiedScheduleIsSubmittedAsSevenDaysMondayFirstWithClosedDaysEmpty() = runBlocking {
        val inputs = mutableListOf<CreateShopInput>()
        val vm = wizard(create = { inputs += it; Result.success(Unit) })
        try {
            fillBasic(vm)
            vm.toggleClosedDay(0); vm.toggleClosedDay(3)
            vm.applySchedulePreset("08:00", "20:00")
            vm.adjustUnifiedClose(30)
            repeat(4) { vm.nextStep() }
            withTimeout(5_000) { vm.state.first { it.isSuccess } }
            val schedules = inputs.single().schedules
            assertEquals(listOf(1, 2, 3, 4, 5, 6, 0), schedules.map { it.dayOfWeek })
            assertEquals(listOf(3, 0), schedules.filter { it.isClosed }.map { it.dayOfWeek })
            assertTrue(schedules.filter { it.isClosed }.all { it.intervals.isEmpty() })
            assertEquals(ScheduleInterval("08:00", "20:30"), schedules.first().intervals.single())
        } finally { vm.close() }
    }

    @Test fun perDayScheduleStartsFromTheUnifiedOneAndKeepsEachDay() = runBlocking {
        val inputs = mutableListOf<CreateShopInput>()
        val vm = wizard(create = { inputs += it; Result.success(Unit) })
        try {
            fillBasic(vm)
            vm.toggleClosedDay(2)
            vm.applySchedulePreset("10:00", "22:00")
            vm.setUsePerDaySchedule(true)
            assertTrue(vm.state.value.schedules.single { it.dayOfWeek == 2 }.isClosed)
            assertTrue(vm.state.value.schedules.filter { it.dayOfWeek != 2 }.all { it.openTime == "10:00" && it.closeTime == "22:00" })
            vm.adjustDayOpenTime(5, -60)
            vm.adjustDayCloseTime(5, 120)
            repeat(4) { vm.nextStep() }
            withTimeout(5_000) { vm.state.first { it.isSuccess } }
            val schedules = inputs.single().schedules
            assertEquals(ScheduleInterval("09:00", "23:59"), schedules.single { it.dayOfWeek == 5 }.intervals.single())
            assertEquals(ScheduleInterval("10:00", "22:00"), schedules.single { it.dayOfWeek == 1 }.intervals.single())
            assertTrue(schedules.single { it.dayOfWeek == 2 }.let { it.isClosed && it.intervals.isEmpty() })
        } finally { vm.close() }
    }

    @Test fun submittedShopTrimsTextTurnsBlankOptionalsIntoNullAndSendsTheSelections() = runBlocking {
        val inputs = mutableListOf<CreateShopInput>()
        val catalogs = ShopCatalogs(
            cities = listOf(City("minsk", "Минск")),
            beans = listOf(CatalogItem("arabica", "Арабика")), equipment = listOf(CatalogItem("grinder", "Кофемолка")),
            roasters = listOf(CatalogItem("roast", "Roast")), brewMethods = listOf(CatalogItem("v60", "V60")),
        )
        val vm = wizard(catalogs, create = { inputs += it; Result.success(Unit) })
        try {
            vm.onNameChange("  Кофейня  "); vm.onAddressChange("  Улица, 1 "); vm.onDescriptionChange("   ")
            vm.onPriceRangeSelect(2); vm.onEmailChange("shop@example.com"); vm.onPhoneChange("  ")
            vm.toggleBean("arabica"); vm.toggleEquipment("grinder"); vm.toggleRoaster("roast"); vm.toggleBrewMethod("v60"); vm.toggleBrewMethod("v60"); vm.toggleBrewMethod("v60")
            repeat(5) { vm.nextStep() }
            withTimeout(5_000) { vm.state.first { it.isSuccess } }
            val input = inputs.single()
            assertEquals("Кофейня", input.name); assertEquals("Улица, 1", input.address); assertEquals("minsk", input.cityId)
            assertNull(input.description); assertNull(input.phone); assertEquals("shop@example.com", input.email)
            assertEquals(2, input.priceRange)
            assertEquals(listOf("arabica"), input.coffeeBeanIds); assertEquals(listOf("grinder"), input.equipmentIds)
            assertEquals(listOf("roast"), input.roasterIds); assertEquals(listOf("v60"), input.brewMethodIds)
        } finally { vm.close() }
    }

    @Test fun catalogFailureShowsItsMessageAndRetryLoadsAgain() = runBlocking {
        var attempts = 0
        val shops = Shops().apply {
            catalogs = { if (++attempts == 1) Result.failure(IllegalStateException("Нет сети")) else Result.success(ShopCatalogs()) }
        }
        val vm = AddShopViewModel(shops)
        try {
            assertEquals("Нет сети", withTimeout(5_000) { vm.state.first { !it.isLoadingCatalogs } }.catalogsError)
            vm.loadCatalogs()
            val state = withTimeout(5_000) { vm.state.first { !it.isLoadingCatalogs && it.catalogsError == null } }
            assertEquals(2, attempts); assertNull(state.catalogsError)
        } finally { vm.close() }
    }

    // ---- C. Suggest a shop change ----

    @Test fun editorPrefillsTheCurrentShopAndSendsOnlyTheChosenSection() = runBlocking {
        val drafts = mutableListOf<ShopChangeDraft>()
        val details = CoffeeShopDetails(
            CoffeeShop("shop", "Coffee Shop", null, cityName = "City", priceRange = null, photoUrl = null),
            description = "Текущее описание",
            contact = ShopContact(instagram = "shop_cafe", email = "shop@example.com", website = null, phone = "+375291112233"),
            shopPhotos = listOf(ShopPhoto("p1", "https://x/1"), ShopPhoto("p2", "https://x/2")),
            tagItems = listOf(CatalogItem("wifi", "Wi-Fi")), roasters = listOf(CatalogItem("roast", "Roast")),
            equipmentItems = listOf(CatalogItem("grinder", "Кофемолка")), brewMethodItems = listOf(CatalogItem("v60", "V60")),
        )
        val catalogs = ShopCatalogs(shopTags = listOf(CatalogItem("wifi", "Wi-Fi"), CatalogItem("pets", "Животные")))
        val shops = Shops().apply { this.details = { Result.success(details) }; this.catalogs = { Result.success(catalogs) } }
        val changes = Changes().apply { create = { drafts += it; Result.success(submitted(it.section)) } }

        val contacts = ShopChangeEditorViewModel("shop", ShopChangeSection.Contacts, "", shops, changes)
        try {
            withTimeout(5_000) { contacts.state.first { !it.isLoading } }
            val state = contacts.state.value
            assertEquals("Coffee Shop", state.shopTitle)
            assertEquals("Текущее описание", state.description)
            assertEquals("+375291112233", state.phone); assertEquals("shop@example.com", state.email)
            contacts.onEmailChange("not-an-email")
            assertFalse(contacts.state.value.canSubmit)
            contacts.onEmailChange("  "); contacts.onPhoneChange("+375291112233 ")
            assertTrue(contacts.state.value.canSubmit)
            contacts.submit()
            val draft = withTimeout(5_000) { contacts.state.first { it.submittedRequest != null } }.let { drafts.single() }
            assertEquals(ShopChangeContacts(phoneNumber = "+375291112233", email = null, siteLink = null, instagramLink = "shop_cafe"), draft.contacts)
        } finally { contacts.close() }

        val tags = ShopChangeEditorViewModel("shop", ShopChangeSection.Tags, "", shops, changes)
        try {
            withTimeout(5_000) { tags.state.first { !it.isLoading } }
            assertEquals(setOf("wifi"), tags.state.value.selectedTagIds)
            tags.toggleTag("pets"); tags.toggleTag("wifi")
            tags.submit()
            withTimeout(5_000) { tags.state.first { it.submittedRequest != null } }
            assertEquals(listOf("pets"), drafts.last().tagIds)
            assertEquals(ShopChangeSection.Tags, drafts.last().section)
        } finally { tags.close() }
    }


    // Legacy quirk, kept until decided: the contacts editor validates Instagram as a handle only, so a prefilled link blocks the send until it is retyped.
    @Test fun aPrefilledInstagramLinkFailsTheHandleValidationOfTheContactsEditor() = runBlocking {
        val details = CoffeeShopDetails(CoffeeShop("shop", "Shop", null, cityName = "City", priceRange = null, photoUrl = null),
            contact = ShopContact(instagram = "https://instagram.com/shop"))
        val vm = ShopChangeEditorViewModel("shop", ShopChangeSection.Contacts, "", Shops().apply { this.details = { Result.success(details) } }, Changes())
        try {
            withTimeout(5_000) { vm.state.first { !it.isLoading } }
            assertNotNull(vm.state.value.instagramError)
            assertFalse(vm.state.value.canSubmit)
            vm.onInstagramChange("shop")
            assertTrue(vm.state.value.canSubmit)
        } finally { vm.close() }
    }

    @Test fun editorCapsPhotosAtTheLimitCountingRetainedAndEarlierUploads() = runBlocking {
        val photos = (1..8).map { ShopPhoto("p$it", "https://x/$it") }
        val shops = Shops().apply { details = { Result.success(CoffeeShopDetails(CoffeeShop("shop", "Shop", null, cityName = "City", priceRange = null, photoUrl = null), shopPhotos = photos)) } }
        val vm = ShopChangeEditorViewModel("shop", ShopChangeSection.Photos, "", shops, Changes())
        try {
            withTimeout(5_000) { vm.state.first { !it.isLoading } }
            vm.addPhotos(List(5) { PickedImage(byteArrayOf(it.toByte()), "n$it.jpg") })
            assertEquals(2, vm.state.value.newPhotos.size)
            assertEquals(MAX_SHOP_PHOTOS, vm.state.value.photoCount)
            vm.removeRetainedPhoto("p1"); vm.removeNewPhoto(0)
            assertEquals(MAX_SHOP_PHOTOS - 2, vm.state.value.photoCount)
            vm.addMenuPhotos(List(6) { PickedImage(byteArrayOf(it.toByte()), "m$it.jpg") })
            assertEquals(MAX_MENU_PHOTOS, vm.state.value.menuPhotoCount)
        } finally { vm.close() }
    }

    @Test fun editingARequestOverlaysItsPayloadAndAFailedLoadShowsTheMessage() = runBlocking {
        val shops = Shops()
        val changes = Changes().apply {
            existing = ShopChangeRequest("req", "shop", ShopChangeSection.Tags, ShopChangePayload(tagIds = listOf("pets")), ModerationStatus.Rejected,
                rejectionReason = "Не подходит", createdAtUtc = "2026-10-15T00:00:00Z")
        }
        val vm = ShopChangeEditorViewModel("shop", ShopChangeSection.Tags, "req", shops, changes)
        try {
            withTimeout(5_000) { vm.state.first { !it.isLoading } }
            assertEquals(setOf("pets"), vm.state.value.selectedTagIds)
            assertEquals(ModerationStatus.Rejected, vm.state.value.requestStatus)
        } finally { vm.close() }

        val broken = ShopChangeEditorViewModel("shop", ShopChangeSection.Tags, "", Shops().apply { catalogs = { Result.failure(IllegalStateException("Каталоги недоступны")) } }, Changes())
        try {
            val state = withTimeout(5_000) { broken.state.first { !it.isLoading } }
            assertEquals("Каталоги недоступны", state.error)
        } finally { broken.close() }
    }

    // ---- D. Add roaster ----

    @Test fun cityAndAddressMustBeGivenTogetherAndInstagramIsNormalized() = runBlocking {
        val inputs = mutableListOf<CreateRoasterInput>()
        val shops = Shops().apply { catalogs = { Result.success(ShopCatalogs(cities = listOf(City("minsk", "Минск"), City("brest", "Брест")))) } }
        val roasters = Roasters().apply { submit = { inputs += it; Result.success(RoasterSubmissionResult("id", "Pending", true, "ок")) } }
        val vm = AddRoasterViewModel(roasters, shops)
        try {
            withTimeout(5_000) { vm.state.first { !it.isLoadingCatalogs } }
            assertEquals("Введите название", vm.state.value.nameError)
            vm.onNameChange("n".repeat(120))
            assertEquals(100, vm.state.value.name.length)
            vm.onAddressChange("Улица, 1")
            assertEquals("Для адреса выберите город", vm.state.value.locationError); assertFalse(vm.state.value.canSubmit)
            vm.onAddressChange(""); vm.onCitySelect(City("minsk", "Минск"))
            assertEquals("Для выбранного города укажите адрес", vm.state.value.locationError)
            vm.onAddressChange("  Улица, 1  ")
            assertNull(vm.state.value.locationError)
            vm.onInstagramChange("a"); assertNotNull(vm.state.value.instagramError)
            vm.onInstagramChange("@roast.shop"); assertNull(vm.state.value.instagramError)
            vm.onWebsiteChange("bad url"); assertNotNull(vm.state.value.websiteError); assertFalse(vm.state.value.canSubmit)
            vm.onWebsiteChange("https://roast.example")
            vm.onAboutChange("   ")
            vm.submit()
            withTimeout(5_000) { vm.state.first { it.result != null } }
            val input = inputs.single()
            assertEquals("https://instagram.com/roast.shop", input.instagramLink)
            // The roaster form sends the address as typed (the data layer trims it).
            assertEquals("  Улица, 1  ", input.address)
            assertEquals("minsk", input.cityId); assertNull(input.about); assertEquals("https://roast.example", input.siteLink)
        } finally { vm.close() }

        val urls = mutableListOf<CreateRoasterInput>()
        val second = AddRoasterViewModel(Roasters().apply { submit = { urls += it; Result.success(RoasterSubmissionResult("id", "Pending", false, "")) } }, Shops())
        try {
            withTimeout(5_000) { second.state.first { !it.isLoadingCatalogs } }
            second.onNameChange("Roast"); second.onInstagramChange("https://instagram.com/roast"); second.submit()
            withTimeout(5_000) { second.state.first { it.result != null } }
            assertEquals("https://instagram.com/roast", urls.single().instagramLink)
            assertNull(urls.single().cityId); assertNull(urls.single().address); assertNull(urls.single().siteLink)
        } finally { second.close() }
    }

    @Test fun roasterPhotosAreCappedAtFiveAndRemovedByIndex() = runBlocking {
        val vm = AddRoasterViewModel(Roasters(), Shops())
        try {
            withTimeout(5_000) { vm.state.first { !it.isLoadingCatalogs } }
            vm.addPhotos(List(4) { PickedImage(byteArrayOf(it.toByte()), "p$it.jpg") })
            vm.addPhotos(List(4) { PickedImage(byteArrayOf(it.toByte()), "q$it.jpg") })
            assertEquals(5, vm.state.value.photos.size)
            vm.removePhoto(0)
            assertEquals("p1.jpg", vm.state.value.photos.first().fileName)
        } finally { vm.close() }
    }

    @Test fun roasterCannotBeSubmittedWhileTheCityCatalogLoadsAndAFailedCatalogKeepsTheForm() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val shops = Shops().apply { catalogs = { release.await(); Result.failure(IllegalStateException("Города недоступны")) } }
        val vm = AddRoasterViewModel(Roasters(), shops)
        try {
            vm.onNameChange("Roast")
            assertTrue(vm.state.value.isLoadingCatalogs)
            assertFalse(vm.state.value.canSubmit)
            release.complete(Unit)
            val state = withTimeout(5_000) { vm.state.first { !it.isLoadingCatalogs } }
            assertEquals("Города недоступны", state.error)
            assertEquals("Roast", state.name); assertTrue(state.canSubmit)
        } finally { vm.close() }
    }

    // ---- helpers ----

    private fun <T> page(vararg items: T): Result<PagedResult<T>> = Result.success(PagedResult(items.toList(), items.size, if (items.isEmpty()) 0 else 1, 1))
    private fun submission(id: String) = ShopSubmission(id, "Shop $id", null, ModerationStatus.Approved, null, null)
    private fun submitted(section: ShopChangeSection) = ShopChangeRequest("request", "shop", section, ShopChangePayload(), ModerationStatus.Pending, createdAtUtc = "2026-10-15T00:00:00Z")

    private suspend fun loaded(vm: MyContributionsViewModel): MyContributionsUiState = withTimeout(5_000) { vm.state.first { !it.isLoading } }

    private suspend fun wizard(
        catalogs: ShopCatalogs = ShopCatalogs(),
        create: suspend (CreateShopInput) -> Result<Unit> = { error("Unused") },
    ): AddShopViewModel {
        val vm = AddShopViewModel(Shops().apply { this.catalogs = { Result.success(catalogs) }; this.create = create })
        withTimeout(5_000) { vm.state.first { !it.isLoadingCatalogs } }
        return vm
    }

    private fun fillBasic(vm: AddShopViewModel) {
        vm.onNameChange("Coffee Shop"); vm.onAddressChange("Улица, 1"); vm.onCitySelect(City("minsk", "Минск"))
        repeat(4) { vm.nextStep() }
        assertEquals(AddShopStep.SCHEDULE, vm.state.value.currentStep)
    }
}

private class Shops : ShopRepository {
    var catalogs: suspend () -> Result<ShopCatalogs> = { Result.success(ShopCatalogs()) }
    var create: suspend (CreateShopInput) -> Result<Unit> = { error("Unused") }
    var details: suspend () -> Result<CoffeeShopDetails> = {
        Result.success(CoffeeShopDetails(CoffeeShop("shop", "Coffee Shop", null, cityName = "City", priceRange = null, photoUrl = null)))
    }
    var submissions: suspend (ModerationStatus, Int) -> Result<PagedResult<ShopSubmission>> = { _, _ -> Result.success(PagedResult(emptyList(), 0, 0, 1)) }
    override suspend fun getCatalogs() = catalogs()
    override suspend fun createShop(input: CreateShopInput) = create(input)
    override suspend fun getShopDetails(id: String) = details()
    override suspend fun getMenuDrinks() = Result.success(emptyList<CoffeeDrinkDefinition>())
    override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int) = submissions(status, page)
    override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("Unused")
    override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("Unused")
    override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("Unused")
}

private class Roasters : RoasterRepository {
    var submit: suspend (CreateRoasterInput) -> Result<RoasterSubmissionResult> = { error("Unused") }
    var submissions: suspend (ModerationStatus, Int) -> Result<PagedResult<RoasterSubmission>> = { _, _ -> Result.success(PagedResult(emptyList(), 0, 0, 1)) }
    override suspend fun submitRoaster(input: CreateRoasterInput) = submit(input)
    override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int) = submissions(status, page)
    override suspend fun getRoasters(): Result<List<RoasterSummary>> = error("Unused")
    override suspend fun getRoaster(id: String): Result<RoasterDetails> = error("Unused")
}

private class Changes : ShopChangeRequestRepository {
    var existing = ShopChangeRequest("request", "shop", ShopChangeSection.Description, ShopChangePayload(description = "x"), ModerationStatus.Pending, createdAtUtc = "2026-10-15T00:00:00Z")
    var create: suspend (ShopChangeDraft) -> Result<ShopChangeRequest> = { error("Unused") }
    var mine: suspend (Int, Int, ModerationStatus?) -> Result<PagedResult<ShopChangeRequest>> = { _, _, _ -> Result.success(PagedResult(emptyList(), 0, 0, 1)) }
    override suspend fun create(draft: ShopChangeDraft) = create.invoke(draft)
    override suspend fun update(id: String, draft: ShopChangeDraft): Result<ShopChangeRequest> = error("Unused")
    override suspend fun getById(id: String) = Result.success(existing.copy(id = id))
    override suspend fun getMine(page: Int, pageSize: Int, status: ModerationStatus?, shopId: String?, section: ShopChangeSection?) = mine(page, pageSize, status)
}
