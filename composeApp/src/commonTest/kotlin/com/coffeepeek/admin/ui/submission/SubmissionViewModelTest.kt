package com.coffeepeek.admin.ui.submission

import com.coffeepeek.admin.ui.screen.shopchange.ShopChangeEditorViewModel
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.ShopChangeRequestRepository
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.*

class SubmissionViewModelTest {
    @Test fun createdAndUpdatedShopChangesRemainVisibleUntilTheUserLeaves() = runBlocking {
        for (existingId in listOf("", "existing-request")) {
            val requests = Channel<Pair<String?, ShopChangeDraft>>(Channel.UNLIMITED)
            val responses = Channel<Result<ShopChangeRequest>>(Channel.UNLIMITED)
            val vm = readyChange(SubmissionChanges().apply {
                submit = { id, draft -> requests.send(id to draft); responses.receive() }
            }, existingId)
            try {
                vm.submit()
                assertTrue(vm.state.value.isSubmitting)
                repeat(10) { vm.submit() }
                val first = withTimeout(5_000) { requests.receive() }
                assertEquals(existingId.ifBlank { null }, first.first)
                assertEquals("New description", first.second.description)
                assertTrue(requests.tryReceive().isFailure)
                responses.send(Result.failure(IllegalStateException("Connection lost")))
                withTimeout(5_000) { vm.state.first { it.submissionError != null } }
                assertEquals("New description", vm.state.value.description)
                assertFalse(vm.state.value.isSubmitting)
                vm.submit()
                assertNull(vm.state.value.submissionError)
                assertEquals(first, withTimeout(5_000) { requests.receive() })
                responses.send(Result.success(submittedChange))
                withTimeout(5_000) { vm.state.first { it.submittedRequest != null } }
                assertEquals(submittedChange, vm.state.value.submittedRequest)
                assertFalse(vm.state.value.isSubmitting)
                assertNull(vm.state.value.submissionError)
                assertFalse(vm.state.value.canSubmit)
                vm.submit()
                assertTrue(requests.tryReceive().isFailure)
            } finally { vm.close() }
        }
    }

    @Test fun menuPhotosCanBeSubmittedWithoutEditingDrinksAndWithoutLegacyVolumes() = runBlocking {
        val menu = ShopMenu(
            items = listOf(ShopMenuItem("latte", "Латте", category = "coffee", availability = "present", price = 6.5, volumeMl = 300)),
            photos = listOf(ShopMenuPhoto("photo", "https://example.com/menu.jpg")),
        )
        val uploaded = UploadedPhotoMeta("uploaded.jpg", "image/jpeg", "menu/uploaded", 1)
        for (existingId in listOf("", "existing-menu-request")) {
            val requests = Channel<ShopChangeDraft>(Channel.UNLIMITED)
            val changes = SubmissionChanges().apply {
                existing = submittedChange.copy(section = ShopChangeSection.Menu, payload = ShopChangePayload(menu = ShopChangeMenu(
                    items = listOf(ShopChangeMenuItem("latte", MenuItemAvailability.Present, price = 7.5, volumeMl = 250)),
                    retainedPhotoIds = listOf("photo"), newPhotos = listOf(uploaded),
                )))
                submit = { _, draft -> requests.send(draft); Result.success(existing) }
            }
            val vm = ShopChangeEditorViewModel("shop", ShopChangeSection.Menu, existingId, SubmissionShops(menu), changes)
            try {
                withTimeout(5_000) { vm.state.first { !it.isLoading } }
                vm.addMenuPhotos(listOf(PickedImage(byteArrayOf(1), "new-menu.jpg")))
                assertTrue(vm.state.value.canSubmit)
                vm.submit()
                val draft = withTimeout(5_000) { requests.receive() }
                assertEquals(listOf("photo"), draft.retainedMenuPhotoIds)
                assertEquals("new-menu.jpg", draft.newMenuPhotos.single().fileName)
                assertEquals(if (existingId.isBlank()) emptyList() else listOf(uploaded), draft.alreadyUploadedMenuPhotos)
                val drink = requireNotNull(draft.menuItems).single()
                assertEquals("latte", drink.slug)
                assertEquals(MenuItemAvailability.Present, drink.availability)
                assertEquals(if (existingId.isBlank()) 6.5 else 7.5, drink.price)
                assertNull(drink.volumeMl)
            } finally { vm.close() }
        }
    }

    @Test fun aThrownRepositoryErrorStopsTheLoaderOfTheChangeEditor() = runBlocking {
        val change = readyChange(SubmissionChanges().apply { submit = { _, _ -> error("Upload failed") } })
        try {
            change.submit()
            withTimeout(5_000) { change.state.first { it.submissionError != null } }
            assertFalse(change.state.value.isSubmitting)
        } finally { change.close() }
    }

    private suspend fun readyChange(repository: ShopChangeRequestRepository, existingId: String = ""): ShopChangeEditorViewModel {
        val vm = ShopChangeEditorViewModel("shop", ShopChangeSection.Description, existingId, SubmissionShops(), repository)
        withTimeout(5_000) { vm.state.first { !it.isLoading } }
        vm.onDescriptionChange("New description")
        return vm
    }
}

private val submittedChange = ShopChangeRequest("request", "shop", ShopChangeSection.Description,
    ShopChangePayload(description = "New description"), ModerationStatus.Pending, createdAtUtc = "2026-10-15T00:00:00Z")

private class SubmissionShops(private val menu: ShopMenu? = null) : ShopRepository {
    var create: suspend (CreateShopInput) -> Result<Unit> = { error("Unused") }
    override suspend fun createShop(input: CreateShopInput) = create(input)
    override suspend fun getCatalogs() = Result.success(ShopCatalogs())
    override suspend fun getMenuDrinks() = Result.success(menu?.items.orEmpty().mapIndexed { index, item ->
        CoffeeDrinkDefinition(item.slug, item.nameRu, item.nameEn, item.category, index)
    })
    override suspend fun getShopDetails(id: String) = Result.success(CoffeeShopDetails(
        CoffeeShop("shop", "Coffee Shop", null, cityName = "City", priceRange = null, photoUrl = null), menu = menu))
    override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("Unused")
    override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("Unused")
    override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("Unused")
    override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("Unused")
}

private class SubmissionChanges : ShopChangeRequestRepository {
    var existing = submittedChange
    var submit: suspend (String?, ShopChangeDraft) -> Result<ShopChangeRequest> = { _, _ -> error("Unused") }
    override suspend fun create(draft: ShopChangeDraft) = submit(null, draft)
    override suspend fun update(id: String, draft: ShopChangeDraft) = submit(id, draft)
    override suspend fun getById(id: String) = Result.success(existing.copy(id = id))
    override suspend fun getMine(page: Int, pageSize: Int, status: ModerationStatus?, shopId: String?, section: ShopChangeSection?): Result<PagedResult<ShopChangeRequest>> = error("Unused")
}
