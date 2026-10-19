package com.coffeepeek.feature.homecheckin.data.repository

import com.coffeepeek.feature.homecheckin.data.backend.*
import com.coffeepeek.feature.homecheckin.data.mapper.*
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.domain.usecase.homePreparationFieldErrors
import io.ktor.client.HttpClient
import io.ktor.http.Url
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Clients are owned by the caller. Upload client must be separate and credential-free;
 * application composition also supplies the account lease before exposing the repository.
 */
fun createHomePreparationRepository(
    client: HttpClient,
    uploadClient: HttpClient,
    apiBaseUrl: String,
): HomePreparationRepository {
    require(client !== uploadClient) { "Home uploads require a separate credential-free client" }
    require(apiBaseUrl.startsWith("https://") || apiBaseUrl.startsWith("http://"))
    val base = Url(apiBaseUrl)
    require(base.protocol.name in setOf("https", "http") && base.host.isNotBlank())
    return RemoteHomePreparationRepository(HomePreparationBackend(client),
        HomePreparationUploadBackend(client, uploadClient), apiBaseUrl)
}

private class RemoteHomePreparationRepository(
    private val backend: HomePreparationBackend,
    private val uploads: HomePreparationUploadBackend,
    private val apiBaseUrl: String,
) : HomePreparationRepository {
    override suspend fun create(input: HomePreparationInput): Result<HomePreparation> = homeRequestResult {
        validateInput(input, editing = false)
        val photos = uploads.upload(input.photos)
        val cutout = input.drinkPhoto?.let { uploads.upload(listOf(it)).single() }
        backend.create(input.toCreateRequest(photos, cutout)).toDomain(apiBaseUrl)
    }

    override suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation> = homeRequestResult {
        validateInput(input, editing = true)
        backend.update(id, input.toEditRequest()).toDomain(apiBaseUrl)
    }

    override suspend fun detail(id: String): Result<HomePreparation> = homeRequestResult {
        val detail = backend.detail(id)
        if (detail.id != id) throw HomePreparationFailure(HomePreparationIssue.InvalidResponse)
        detail.toDomain(apiBaseUrl)
    }

    override suspend fun delete(id: String): Result<Unit> = homeRequestResult { backend.delete(id) }

    override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> =
        homeRequestResult {
            if (page !in 1..10000 || pageSize !in 1..100) throw HomePreparationFailure(HomePreparationIssue.InvalidInput)
            val response = backend.page(page, pageSize, range?.from, range?.to)
            HomePreparationPage(response.items.map { it.toDomain(apiBaseUrl) }, response.totalCount, page, pageSize)
        }

    override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> = homeRequestResult {
        val days = backend.calendar(range.from, range.to).map { HomePreparationDay(it.preparedOn, it.brewCount) }
        // The calendar endpoint supplies counts only. Preserve the current month-photo
        // behavior using bounded, date-filtered owner pages, never per-entry detail calls.
        val entries = mutableListOf<HomePreparation>()
        if (days.any { it.brewCount > 0 }) {
            var page = 1
            do {
                currentCoroutineContext().ensureActive()
                if (page > 10000) throw HomePreparationFailure(HomePreparationIssue.InvalidResponse)
                val result = backend.page(page, 100, range.from, range.to)
                if (result.totalCount < 0 || result.items.size > 100)
                    throw HomePreparationFailure(HomePreparationIssue.InvalidResponse)
                if (result.items.isEmpty() && result.totalCount > (page - 1).toLong() * 100)
                    throw HomePreparationFailure(HomePreparationIssue.InvalidResponse)
                entries += result.items.map { it.toDomain(apiBaseUrl) }
                val hasMore = page.toLong() * 100 < result.totalCount
                page++
            } while (hasMore)
        }
        currentCoroutineContext().ensureActive()
        val photos = entries.distinctBy(HomePreparation::id).filter { it.preparedOn in range.from..range.to }
            .groupBy(HomePreparation::preparedOn).mapValues { (_, rows) -> rows.mapNotNull { row ->
                row.drinkPhotoUrl?.takeIf(String::isNotBlank)?.let { row.drinkPhotoThumbnailUrl ?: it }
                    ?.takeIf(String::isNotBlank)?.let { HomePreparationCalendarPhoto(it, true, row.preparedAtUtc) }
                    ?: row.photos.firstNotNullOfOrNull { photo -> (photo.calendarUrl ?: photo.url).takeIf(String::isNotBlank) }
                        ?.let { HomePreparationCalendarPhoto(it, false, row.preparedAtUtc) }
            } }
        days.map { it.copy(photos = photos[it.preparedOn].orEmpty()) }
    }
}

private fun validateInput(input: HomePreparationInput, editing: Boolean) {
    val errors = homePreparationFieldErrors(input, editing)
    if (errors.isNotEmpty()) throw HomePreparationFailure(HomePreparationIssue.InvalidInput, errors)
}
