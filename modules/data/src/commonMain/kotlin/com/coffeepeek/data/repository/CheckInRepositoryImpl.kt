package com.coffeepeek.data.repository

import com.coffeepeek.api.model.request.CheckInVisibilityReq
import com.coffeepeek.api.model.request.CreateCheckInReq
import com.coffeepeek.api.model.request.UpdateCheckInReq
import com.coffeepeek.api.model.response.shop.RatingDto
import com.coffeepeek.api.service.CheckInApiService
import com.coffeepeek.data.mapper.toDomain
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.CreateCheckInInput
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.ReviewRating
import com.coffeepeek.domain.model.UpdateCheckInInput
import com.coffeepeek.domain.model.validateCheckInContent
import com.coffeepeek.domain.repository.CheckInRepository
import com.coffeepeek.domain.repository.CheckInHelpfulVote
import com.coffeepeek.domain.repository.PhotoRepository
import kotlinx.coroutines.CancellationException

class CheckInRepositoryImpl(
    private val checkInApiService: CheckInApiService,
    private val photoRepository: PhotoRepository,
    private val fileUrlResolver: FileUrlResolver,
) : CheckInRepository {

    override suspend fun createCheckIn(input: CreateCheckInInput): Result<Unit> = try {
        require(input.shopSlug.isNotBlank()) { "Выберите кофейню" }
        validateCheckInContent(input.text, input.rating, input.drinkSlug, input.customDrinkName)?.let { error(it) }
        require(input.photos.size <= 5) { "Можно добавить до 5 фото" }
        val uploaded = photoRepository.uploadCheckInPhotos(input.photos).getOrThrow()
        checkInApiService.createCheckIn(CreateCheckInReq(
            coffeeShopSlug = input.shopSlug,
            text = input.text.trim(),
            rating = input.rating.toDto(),
            visibility = input.visibility.name,
            visitedAt = input.visitedAtIso,
            drinkSlug = input.drinkSlug,
            customDrinkName = input.customDrinkName?.trim(),
            photos = uploaded.toUploadedPhotoReqs(),
        )).getOrThrow()
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun getMyCheckIns(page: Int, pageSize: Int): Result<PagedResult<CheckIn>> {
        val size = pageSize.coerceIn(1, 100)
        val number = page.coerceAtLeast(1)
        return checkInApiService.getMyCheckIns(number, size).map { response ->
            PagedResult(
                items = response.items.map { it.toDomain(fileUrlResolver) },
                totalCount = response.totalCount,
                totalPages = pageCount(response.totalCount, size),
                currentPage = number,
            )
        }
    }

    override suspend fun getMyCheckIns(from: String, to: String, pageSize: Int): Result<List<CheckIn>> = try {
        val size = pageSize.coerceIn(1, 100)
        val first = checkInApiService.getMyCheckIns(1, size, from, to).getOrThrow()
        val items = first.items.toMutableList()
        for (page in 2..pageCount(first.totalCount, size)) {
            val next = checkInApiService.getMyCheckIns(page, size, from, to).getOrThrow()
            if (next.items.isEmpty()) break
            items += next.items
        }
        Result.success(items.distinctBy { it.id }.map { it.toDomain(fileUrlResolver) })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun updateCheckIn(id: String, input: UpdateCheckInInput): Result<CheckIn> {
        validateCheckInContent(input.text, input.rating, input.drinkSlug, input.customDrinkName)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        return checkInApiService.updateCheckIn(id, UpdateCheckInReq(
            text = input.text.trim(),
            rating = input.rating.toDto(),
            drinkSlug = input.drinkSlug,
            customDrinkName = input.customDrinkName?.trim(),
        )).map { it.toDomain(fileUrlResolver) }
    }

    override suspend fun setVisibility(id: String, visibility: CheckInVisibility): Result<CheckIn> =
        checkInApiService.setVisibility(id, CheckInVisibilityReq(visibility.name)).map { it.toDomain(fileUrlResolver) }

    override suspend fun setHelpful(id: String, helpful: Boolean): Result<CheckInHelpfulVote> =
        checkInApiService.setHelpful(id, helpful).map { CheckInHelpfulVote(it.isHelpful, it.helpfulCount) }

    override suspend fun report(id: String, text: String): Result<Unit> = checkInApiService.report(id, text)
}

private fun ReviewRating.toDto() = RatingDto(place = place, service = service, coffee = coffee)
private fun pageCount(total: Int, size: Int): Int = ((total.coerceAtLeast(0).toLong() + size - 1) / size).toInt()
