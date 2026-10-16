package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.BrewMethodOption
import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.admin.feature.checkin.api.DrinkPhotoPreparation
import com.coffeepeek.admin.feature.checkin.ui.DrinkPhotoPreparationController
import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.feature.homecheckin.domain.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.*

class HomeCheckInEditorTest {
    private val method = BrewMethodOption("hario-v60", "Hario V60")
    private val catalog = BrewMethodCatalog { Result.success(listOf(method)) }

    @Test
    fun newEntryStartsAtFiveAndEditKeepsItsActualScoreAndCustomDrink() = runBlocking {
        val vm = HomeCheckInViewModel(EditorRepository(), EditorCoffeePicker(), catalog)
        try {
            vm.create()
            assertEquals(5, vm.state.value.draft.tasteScore)
            assertTrue(homeCheckInFieldErrors(vm.state.value.draft).isEmpty(), "Generated timestamps must satisfy the backend contract")
            withTimeout(2_000) { vm.state.first { it.brewMethods.isNotEmpty() } }
            assertEquals(listOf(method), vm.state.value.brewMethods)
            vm.open("entry", editOnLoad = true)
            withTimeout(2_000) { vm.state.first { it.draft.tasteScore == 2 } }
            assertEquals("other", vm.state.value.draft.drinkSlug)
            assertEquals("Мой кофе", vm.state.value.draft.customDrinkName)
        } finally { vm.close() }
    }

    @Test
    fun catalogLoadsWithoutQueryAndTypingOnlyRequestsTheLatestSearch() = runBlocking {
        val requests = Channel<String>(Channel.UNLIMITED)
        val picker = EditorCoffeePicker { query ->
            requests.send(query)
            Result.success(listOf(CoffeePickerItem(query.ifBlank { "initial" }, "Coffee", "Roaster")))
        }
        val vm = HomeCheckInViewModel(EditorRepository(), picker, catalog)
        try {
            vm.create()
            vm.searchCatalog()
            assertEquals("", withTimeout(2_000) { requests.receive() })
            withTimeout(2_000) { vm.state.first { !it.catalogLoading } }
            assertEquals("initial", vm.state.value.catalogItems.single().slug)
            vm.setCatalogQuery("э")
            vm.setCatalogQuery("эф")
            vm.setCatalogQuery("эфиопия")
            assertTrue(requests.tryReceive().isFailure, "Typing must wait for the debounce")
            assertEquals("эфиопия", withTimeout(2_000) { requests.receive() })
            withTimeout(2_000) { vm.state.first { !it.catalogLoading } }
            assertEquals("эфиопия", vm.state.value.catalogItems.single().slug)
            assertTrue(requests.tryReceive().isFailure)
            vm.setCatalogQuery("")
            assertEquals("", withTimeout(2_000) { requests.receive() })
        } finally { vm.close(); requests.close() }
    }

    @Test
    fun lateCancelledSearchCannotReplaceCurrentResults() = runBlocking {
        val oldStarted = CompletableDeferred<Unit>()
        val oldResult = CompletableDeferred<Unit>()
        val oldFinished = CompletableDeferred<Unit>()
        val picker = EditorCoffeePicker { query ->
            if (query == "old") withContext(NonCancellable) {
                oldStarted.complete(Unit)
                oldResult.await()
                oldFinished.complete(Unit)
            }
            Result.success(listOf(CoffeePickerItem(query, query, "Roaster")))
        }
        val vm = HomeCheckInViewModel(EditorRepository(), picker, catalog)
        try {
            vm.setCatalogQuery("old")
            withTimeout(2_000) { oldStarted.await() }
            vm.setCatalogQuery("new")
            withTimeout(2_000) { vm.state.first { !it.catalogLoading && it.catalogItems.singleOrNull()?.slug == "new" } }
            oldResult.complete(Unit)
            withTimeout(2_000) { oldFinished.await() }
            assertEquals("new", vm.state.value.catalogItems.single().slug)
        } finally { oldResult.complete(Unit); vm.close() }
    }

    @Test
    fun durationConvertsMinutesAndSecondsWithoutRounding() {
        assertEquals(195, homeBrewDurationSeconds("3", "15"))
        assertEquals(30, homeBrewDurationSeconds("", "30"))
        assertEquals(180, homeBrewDurationSeconds("3", ""))
        assertEquals(604800, homeBrewDurationSeconds("10080", "0"))
        assertNull(homeBrewDurationSeconds("", ""))
    }

    @Test
    fun saveCollectsErrorsBeforeRequestAndEditsOnlyRevalidateChangedFields() {
        val repository = EditorRepository()
        val vm = HomeCheckInViewModel(repository, EditorCoffeePicker(), catalog)
        try {
            vm.create()
            vm.updateDraft { it.copy(doseGrams = "0", brewDurationSeconds = 0, brewMethodCode = "INVALID") }
            vm.save()
            assertEquals(setOf(HomeCheckInField.Dose, HomeCheckInField.Duration, HomeCheckInField.Method), vm.state.value.fieldErrors.keys)
            assertEquals(0, repository.saves)
            vm.updateDraft { it.copy(doseGrams = "18.001") }
            assertEquals("Укажите не более двух знаков после запятой", vm.state.value.fieldErrors[HomeCheckInField.Dose])
            vm.updateDraft { it.copy(doseGrams = "18") }
            assertEquals(setOf(HomeCheckInField.Duration, HomeCheckInField.Method), vm.state.value.fieldErrors.keys)
            vm.updateDraft { it.copy(text = "Заметка") }
            assertEquals(2, vm.state.value.fieldErrors.size)
        } finally { vm.close() }
    }

    @Test
    fun serverFieldsSurviveUnrelatedEditsAndClearWhenAProductIsSelected() = runBlocking {
        val serverError = HomeCheckInFailure("Проверьте поля", mapOf(
            HomeCheckInField.Bean to "Выберите зерно заново",
            HomeCheckInField.Method to "Выберите доступный способ заваривания из списка",
        ))
        val vm = HomeCheckInViewModel(EditorRepository(serverError), EditorCoffeePicker(), catalog)
        try {
            vm.create()
            vm.save()
            withTimeout(2_000) { vm.state.first { it.fieldErrors.isNotEmpty() } }
            vm.updateDraft { it.copy(text = "Примечание") }
            assertEquals(serverError.fieldErrors, vm.state.value.fieldErrors)
            vm.selectCatalogProduct(CoffeePickerItem("bean", "Зерно", "Обжарщик"))
            assertEquals(setOf(HomeCheckInField.Method), vm.state.value.fieldErrors.keys)
            vm.updateDraft { it.copy(brewMethodCode = "hario-v60") }
            assertTrue(vm.state.value.fieldErrors.isEmpty())
            assertNull(vm.state.value.formError)
        } finally { vm.close() }
    }

    @Test
    fun unexpectedExceptionsNeverLeakIntoTheFormAndTheDraftIsRetained() = runBlocking {
        val vm = HomeCheckInViewModel(EditorRepository(IllegalStateException("SQL internal exception")), EditorCoffeePicker(), catalog)
        try {
            vm.create()
            vm.updateDraft { it.copy(doseGrams = "18", text = "Мой кофе") }
            val draft = vm.state.value.draft
            vm.save()
            withTimeout(2_000) { vm.state.first { it.formError != null } }
            assertEquals("Не удалось сохранить чекин. Попробуйте ещё раз.", vm.state.value.formError)
            assertEquals(draft, vm.state.value.draft)
            assertFalse(vm.state.value.saving)
            assertTrue(vm.state.value.editing)
        } finally { vm.close() }
    }

    @Test
    fun summaryUsesRussianPluralForms() {
        assertEquals("Проверьте заполненные поля (1 ошибка).", homeValidationSummary(1))
        assertEquals("Проверьте заполненные поля (3 ошибки).", homeValidationSummary(3))
        assertEquals("Проверьте заполненные поля (5 ошибок).", homeValidationSummary(5))
        assertEquals("Проверьте заполненные поля (11 ошибок).", homeValidationSummary(11))
    }

    @Test
    fun homeCreationWaitsForCutoutAndPreservesOriginalAndOtherFields() = runBlocking {
        val repository = EditorRepository()
        val release = CompletableDeferred<Unit>()
        val original = HomePhotoUpload("home.jpg", "image/jpeg", byteArrayOf(1, 2))
        val cutout = PendingPhotoUpload("cup.png", "image/png", byteArrayOf(3, 4))
        val factory = DrinkPhotoPreparation.Factory { scope, changed ->
            DrinkPhotoPreparationController(scope, DrinkPhotoExtractor { release.await(); listOf(cutout) }, changed)
        }
        val vm = HomeCheckInViewModel(repository, EditorCoffeePicker(), catalog, factory)
        try {
            vm.create()
            vm.updateDraft { it.copy(photos = listOf(original), text = "Ягодный кофе") }
            assertTrue(vm.state.value.drinkPhotoState.processing)
            vm.save()
            assertEquals(0, repository.saves)
            vm.updateDraft { it.copy(doseGrams = "15") }
            release.complete(Unit)
            withTimeout(2_000) { vm.state.first { it.drinkPhotoState.selected == cutout } }
            vm.save()
            withTimeout(2_000) { vm.state.first { !it.saving && it.selected != null } }
            val input = assertNotNull(repository.submitted)
            assertEquals("Ягодный кофе", input.text)
            assertEquals("15", input.doseGrams)
            assertEquals(listOf(original), input.photos)
            assertContentEquals(cutout.bytes, assertNotNull(input.drinkPhoto).bytes)
            assertEquals("image/png", input.drinkPhoto?.contentType)
        } finally { release.complete(Unit); vm.close() }
    }

    @Test
    fun editingExistingHomePreservesServerPhotosWithoutExtractionOrReplacementUploads() = runBlocking {
        val repository = EditorRepository().apply { entry = entry.copy(
            photos = listOf(HomeCheckInPhoto("photo", "/full.jpg")), drinkPhotoUrl = "/cup.png") }
        var extractions = 0
        val factory = DrinkPhotoPreparation.Factory { scope, changed ->
            DrinkPhotoPreparationController(scope, DrinkPhotoExtractor { extractions++; emptyList() }, changed)
        }
        val vm = HomeCheckInViewModel(repository, EditorCoffeePicker(), catalog, factory)
        try {
            vm.open("entry", editOnLoad = true)
            withTimeout(2_000) { vm.state.first { it.editing } }
            vm.updateDraft { it.copy(text = "Новая заметка") }
            vm.save()
            withTimeout(2_000) { vm.state.first { !it.saving && !it.editing } }
            assertEquals(0, extractions)
            val input = assertNotNull(repository.updated)
            assertTrue(input.photos.isEmpty())
            assertNull(input.drinkPhoto)
            assertEquals("/cup.png", vm.state.value.selected?.drinkPhotoUrl)
            assertEquals(repository.entry.photos, vm.state.value.selected?.photos)
        } finally { vm.close() }
    }
}

private class EditorCoffeePicker(
    private val search: suspend (String) -> Result<List<CoffeePickerItem>> = { Result.success(emptyList()) },
) : CoffeePicker {
    override suspend fun search(query: String) = search.invoke(query)
}

private class EditorRepository(private val saveFailure: Throwable? = null) : HomeCheckInRepository {
    var saves = 0
    var submitted: HomeCheckInInput? = null
    var updated: HomeCheckInInput? = null
    var entry = HomeCheckIn(
        id = "entry", preparedAtUtc = "2026-10-13T08:00:00Z", preparedOn = "2026-10-13", createdAtUtc = "2026-10-13T08:00:00Z",
        text = null, brewMethodCode = "hario-v60", coffeeProductId = null, beanName = null,
        doseGrams = null, brewDurationSeconds = 195, tasteScore = 2, drinkName = "Мой кофе",
        drinkSlug = null, customDrinkName = "Мой кофе", photos = emptyList(), drinkPhotoUrl = null,
    )
    override suspend fun history(page: Int, pageSize: Int, from: String?, to: String?) = Result.success(HomeCheckInPage(emptyList(), 0, page, pageSize))
    override suspend fun calendar(from: String, to: String) = Result.success(emptyList<HomeCalendarDay>())
    override suspend fun detail(id: String) = Result.success(entry)
    override suspend fun create(input: HomeCheckInInput): Result<HomeCheckIn> {
        saves++
        submitted = input
        return saveFailure?.let { Result.failure(it) } ?: Result.success(entry)
    }
    override suspend fun update(id: String, input: HomeCheckInInput): Result<HomeCheckIn> {
        updated = input
        return Result.success(entry)
    }
    override suspend fun delete(id: String) = Result.success(Unit)
    override suspend fun beanStats() = Result.success(emptyList<HomeBeanStats>())
    override suspend fun methodStats() = Result.success(emptyList<HomeMethodStats>())
    override suspend fun photo(url: String) = Result.success(byteArrayOf())
}
