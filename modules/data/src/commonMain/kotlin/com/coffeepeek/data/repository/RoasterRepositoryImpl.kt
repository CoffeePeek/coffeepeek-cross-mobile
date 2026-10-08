package com.coffeepeek.data.repository

import com.coffeepeek.api.model.request.CreateRoasterSubmissionReq
import com.coffeepeek.api.service.RoasterApiService
import com.coffeepeek.api.model.response.shop.RoasterSummaryDto
import com.coffeepeek.data.mapper.toDomain
import com.coffeepeek.data.mapper.toDto
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.CreateRoasterInput
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.RoasterContact
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.model.RoasterLocation
import com.coffeepeek.domain.model.RoasterPhoto
import com.coffeepeek.domain.model.RoasterShop
import com.coffeepeek.domain.model.RoasterSubmission
import com.coffeepeek.domain.model.RoasterSubmissionResult
import com.coffeepeek.domain.model.RoasterSummary
import com.coffeepeek.domain.repository.PhotoRepository
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.room.repository.SettingRepository
import com.coffeepeek.room.repository.readSerializable
import com.coffeepeek.room.repository.saveSerializable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class RoasterRepositoryImpl(
    private val roasterApiService: RoasterApiService,
    private val photoRepository: PhotoRepository,
    private val fileUrlResolver: FileUrlResolver,
    private val settings: SettingRepository,
    cacheNamespace: String,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : RoasterRepository {

    private val catalogMutex = Mutex()
    private var cachedCatalog: List<RoasterSummary>? = null
    private val catalogCacheKey = "roaster_catalog_v1:${cacheNamespace.trimEnd('/')}"

    override suspend fun getRoasters(): Result<List<RoasterSummary>> = catalogMutex.withLock {
        cachedCatalog?.let { return@withLock Result.success(it) }
        val saved = try {
            settings.readSerializable<CachedRoasterCatalog>(catalogCacheKey)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        try {
            val catalog = if (saved != null && nowMs() - saved.savedAtMs in 0 until ROASTER_CACHE_TTL_MS) {
                saved.items
            } else {
                val loaded = roasterApiService.getRoasters().getOrThrow()
                currentCoroutineContext().ensureActive()
                try {
                    settings.saveSerializable(catalogCacheKey, CachedRoasterCatalog(nowMs(), loaded))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // A disk-cache write failure must not hide successfully loaded public data.
                }
                loaded
            }
            Result.success(cacheCatalog(catalog))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (saved != null) Result.success(cacheCatalog(saved.items)) else Result.failure(error)
        }
    }

    private fun cacheCatalog(items: List<RoasterSummaryDto>): List<RoasterSummary> {
        val knownSlugs = mutableSetOf<String>()
        return items.map { it.toDomain(fileUrlResolver) }
            .filter { item -> item.publicAddress?.slug?.let(knownSlugs::add) ?: true }
            .also { cachedCatalog = it }
    }

    override suspend fun getRoaster(id: String): Result<RoasterDetails> =
        roasterApiService.getRoaster(id).map { dto ->
            RoasterDetails(
                id = dto.address.slug,
                publicAddress = dto.address.toDomain(),
                name = dto.name,
                about = dto.about,
                location = dto.location
                    ?.takeIf { it.address.isNotBlank() }
                    ?.let { RoasterLocation(it.address, it.latitude, it.longitude) },
                contact = dto.contact?.let {
                    RoasterContact(
                        instagramLink = it.instagramLink?.takeIf(String::isNotBlank),
                        siteLink = it.siteLink?.takeIf(String::isNotBlank),
                    )
                },
                photos = dto.photos
                    .sortedBy { it.sortIndex }
                    .mapNotNull { photo ->
                        val url = fileUrlResolver.resolve(photo.storageKey, photo.fullUrl)
                            ?: return@mapNotNull null
                        RoasterPhoto(
                            id = photo.id,
                            fileName = photo.fileName,
                            storageKey = photo.storageKey,
                            fullUrl = url,
                            sortIndex = photo.sortIndex,
                        )
                    },
                shops = dto.shops.map { RoasterShop(it.address.slug, it.name, it.coverPhoto?.fullUrl, it.address.toDomain()) },
            )
        }

    override suspend fun submitRoaster(
        input: CreateRoasterInput,
    ): Result<RoasterSubmissionResult> = runCatching {
        val uploadedPhotos = photoRepository.uploadRoasterPhotos(input.photos).getOrThrow()
        val result = roasterApiService.submitRoaster(
            CreateRoasterSubmissionReq(
                name = input.name.trim(),
                about = input.about?.trim()?.takeIf { it.isNotBlank() },
                cityId = input.cityId,
                address = input.address?.trim()?.takeIf { it.isNotBlank() },
                instagramLink = input.instagramLink?.trim()?.takeIf { it.isNotBlank() },
                siteLink = input.siteLink?.trim()?.takeIf { it.isNotBlank() },
                photos = uploadedPhotos.toUploadedPhotoReqs().takeIf { it.isNotEmpty() },
            ),
        ).getOrThrow()
        RoasterSubmissionResult(
            roasterId = result.data.roasterId,
            status = result.data.status,
            isAddressValidated = result.data.isAddressValidated,
            message = result.message.orEmpty(),
        )
    }

    override suspend fun getMyRoasterSubmissions(
        status: ModerationStatus,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<RoasterSubmission>> =
        roasterApiService.getMyModerationRoasters(status.toDto(), page, pageSize).map { response ->
            PagedResult(
                items = response.items.map {
                    RoasterSubmission(
                        id = it.id,
                        name = it.name,
                        about = it.about,
                        status = it.moderationStatus.toDomain(),
                        rejectedReason = it.rejectedReason,
                    )
                },
                totalCount = response.totalItems,
                totalPages = response.totalPages,
                currentPage = response.currentPage,
            )
        }
}

private const val ROASTER_CACHE_TTL_MS = 24 * 60 * 60 * 1_000L

@Serializable
private data class CachedRoasterCatalog(val savedAtMs: Long, val items: List<RoasterSummaryDto>)
