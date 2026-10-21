package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.feature.homecheckin.data.repository.createHomePreparationRepository
import com.coffeepeek.feature.homecheckin.domain.model.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import kotlin.test.*

class SandboxBackendTest {
    @Test fun delayedRequestsPreserveCancellationWithoutWriting() = runTest {
        val backend = backend()
        try {
            val repo = createHomePreparationRepository(backend.gateway, backend.uploader, SandboxBackend.BASE_URL)
            backend.scenario = SandboxScenario.Slow
            assertFailsWith<CancellationException> { withTimeout(100) { repo.create(input) } }
            backend.scenario = SandboxScenario.Ready
            assertEquals(3, repo.page(1, 20, null).getOrThrow().totalCount)
        } finally { backend.close() }
    }
    @Test fun actualShopHistoryReaderUsesLocalMineEndpointAndExclusiveUtcBounds() = runTest {
        val backend = backend()
        try {
            val repo = com.coffeepeek.feature.checkinhistory.data.repository.createCheckInHistoryRepository(backend.gateway, SandboxBackend.BASE_URL)
            val row = repo.page(1, 20).getOrThrow().items.single()
            assertEquals("Sandbox Cafe", row.shopName)
            val period = com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod("2026-10-18T07:00:00Z", "2026-10-18T08:00:00Z")
            assertEquals(listOf(row.id), repo.period(period).getOrThrow().map { it.id })
            assertTrue(repo.period(period.copy(fromInclusive = "2026-10-18T06:00:00Z", toExclusive = "2026-10-18T07:00:00Z")).getOrThrow().isEmpty())
        } finally { backend.close() }
    }
    private val png = byteArrayOf(1, 2, 3)
    private fun backend() = SandboxBackend(png, today = LocalDate.parse("2026-10-18"))
    private val input = HomePreparationInput("2026-10-18T08:00:00Z", preparedOn = "2026-10-18",
        customBeanName = "Sandbox Bean", text = "Local only", tasteScore = 4)

    @Test fun populatedMonthPreservesMultiplePreparationsPhotoLessCountsAndLocalMedia() = runTest {
        val backend = backend()
        try {
            val repo = createHomePreparationRepository(backend.gateway, backend.uploader, SandboxBackend.BASE_URL)
            assertEquals(3, repo.page(1, 20, null).getOrThrow().totalCount)
            val calendar = repo.calendar(HomePreparationDateRange("2026-10-01", "2026-10-31")).getOrThrow()
            assertEquals(2, calendar.single { it.preparedOn == "2026-10-18" }.brewCount)
            assertEquals(1, calendar.single { it.preparedOn == "2026-10-18" }.photos.size)
            assertTrue(calendar.single { it.preparedOn == "2026-10-17" }.photos.isEmpty())
            val url = repo.page(1, 20, null).getOrThrow().items.flatMap { it.photos }.single().url
            assertContentEquals(png, backend.photo(url))
            assertNull(backend.photo("https://coffeepeek.by/media/sample"))
        } finally { backend.close() }
    }

    @Test fun realRepositoryCreatesEditsAndDeletesOnlyLocalRows() = runTest {
        val backend = backend()
        try {
            backend.reset(false)
            val repo = createHomePreparationRepository(backend.gateway, backend.uploader, SandboxBackend.BASE_URL)
            val created = repo.create(input).getOrThrow()
            assertEquals("Sandbox Bean", created.beanName)
            assertEquals("Local only", repo.detail(created.id).getOrThrow().text)
            val edited = repo.update(created.id, input.copy(text = "Edited", visibility = HomePreparationVisibility.Public)).getOrThrow()
            assertEquals("Edited", edited.text)
            assertEquals(HomePreparationModeration.Pending, edited.moderation)
            assertEquals(1, repo.page(1, 20, null).getOrThrow().totalCount)
            repo.delete(created.id).getOrThrow()
            assertTrue(repo.page(1, 20, null).getOrThrow().items.isEmpty())
            assertEquals(HomePreparationIssue.NotFound, assertIs<HomePreparationFailure>(repo.detail(created.id).exceptionOrNull()).issue)
        } finally { backend.close() }
    }

    @Test fun galleryUploadUsesSeparateMockClientAndBytesStayInMemory() = runTest {
        val backend = backend()
        try {
            val repo = createHomePreparationRepository(backend.gateway, backend.uploader, SandboxBackend.BASE_URL)
            val photo = HomePreparationUpload("sample.png", "image/png", png)
            val created = repo.create(input.copy(photos = listOf(photo))).getOrThrow()
            assertEquals(1, created.photos.size)
            assertContentEquals(png, backend.photo(created.photos.single().url))
            val edited = repo.update(created.id, input).getOrThrow()
            assertEquals(created.photos, edited.photos)
        } finally { backend.close() }
    }

    @Test fun clearingCustomDrinkRemovesItFromTheSavedRecord() = runTest {
        val backend = backend()
        try {
            val repo = createHomePreparationRepository(backend.gateway, backend.uploader, SandboxBackend.BASE_URL)
            val created = repo.create(input.copy(drinkSlug = "other", customDrinkName = "Фильтр")).getOrThrow()
            assertEquals("Фильтр", created.drinkName)
            val edited = repo.update(created.id, input).getOrThrow()
            assertNull(edited.drinkName)
            assertNull(edited.drinkSlug)
            assertNull(repo.detail(created.id).getOrThrow().drinkName)
        } finally { backend.close() }
    }

    @Test fun editingOnlyTheNoteKeepsTheSelectedCatalogBeanName() = runTest {
        val backend = backend()
        try {
            val repo = createHomePreparationRepository(backend.gateway, backend.uploader, SandboxBackend.BASE_URL)
            val created = repo.create(input.copy(customBeanName = null, coffeeProductSlug = "sandbox-ethiopia")).getOrThrow()
            assertEquals("Sandbox Ethiopia", created.beanName)
            val edited = repo.update(created.id, input.copy(customBeanName = null,
                coffeeProductId = created.coffeeProductId, text = "Edited note")).getOrThrow()
            assertEquals(created.coffeeProductId, edited.coffeeProductId)
            assertEquals(created.beanName, edited.beanName)
            assertEquals(created.beanName, repo.detail(created.id).getOrThrow().beanName)
        } finally { backend.close() }
    }

    @Test fun readAndWriteFailureScenariosNeverChangeRows() = runTest {
        val backend = backend()
        try {
            val repo = createHomePreparationRepository(backend.gateway, backend.uploader, SandboxBackend.BASE_URL)
            for ((mode, issue) in listOf(SandboxScenario.Offline to HomePreparationIssue.Offline,
                SandboxScenario.ServerError to HomePreparationIssue.Unavailable)) {
                backend.scenario = mode
                assertEquals(issue, assertIs<HomePreparationFailure>(repo.page(1, 20, null).exceptionOrNull()).issue)
            }
            backend.scenario = SandboxScenario.SaveError
            assertTrue(repo.create(input).isFailure)
            val original = repo.page(1, 20, null).getOrThrow().items.first()
            assertTrue(repo.delete(original.id).isFailure)
            assertTrue(repo.update(original.id, input).isFailure)
            assertEquals(3, repo.page(1, 20, null).getOrThrow().totalCount)
        } finally { backend.close() }
    }

    @Test fun failedUploadCannotCreateEntryAndExplicitRetrySucceedsOnce() = runTest {
        val backend = backend()
        try {
            val repo = createHomePreparationRepository(backend.gateway, backend.uploader, SandboxBackend.BASE_URL)
            val value = input.copy(photos = listOf(HomePreparationUpload("sample.png", "image/png", png)))
            backend.scenario = SandboxScenario.UploadError
            assertTrue(repo.create(value).isFailure)
            assertEquals(3, repo.page(1, 20, null).getOrThrow().totalCount)
            backend.scenario = SandboxScenario.Ready
            repo.create(value).getOrThrow()
            assertEquals(4, repo.page(1, 20, null).getOrThrow().totalCount)
        } finally { backend.close() }
    }

    @Test fun unknownEndpointsAndForeignHostsFailClosedInsteadOfDelegatingToNetwork(): Unit = runTest {
        val backend = backend()
        try {
            for (url in listOf("${SandboxBackend.BASE_URL}/api/not-implemented", "https://coffeepeek.by/api/v1/check-ins/home")) {
                assertFailsWith<ClientRequestException> { backend.gateway.get(url) }
            }
            assertFailsWith<ClientRequestException> {
                backend.uploader.put("https://coffeepeek.by/upload/key") { setBody(png) }
            }
            assertFailsWith<ClientRequestException> {
                backend.uploader.put("${SandboxBackend.BASE_URL}/upload/unissued") { setBody(png) }
            }
        } finally { backend.close() }
    }

    @Test fun resetAndByteOwnershipAreIsolatedBetweenSandboxInstances() = runTest {
        val first = backend(); val second = backend()
        try {
            first.reset(false)
            val repo = createHomePreparationRepository(second.gateway, second.uploader, SandboxBackend.BASE_URL)
            assertEquals(3, repo.page(1, 20, null).getOrThrow().totalCount)
            val bytes = requireNotNull(second.photo("${SandboxBackend.BASE_URL}/media/sample"))
            bytes[0] = 99
            assertContentEquals(png, second.photo("${SandboxBackend.BASE_URL}/media/sample"))
        } finally { first.close(); second.close() }
    }
}
