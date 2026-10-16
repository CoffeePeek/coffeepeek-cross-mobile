package com.coffeepeek.admin.feature.profile.data

import com.coffeepeek.admin.feature.profile.domain.*
import com.coffeepeek.core.network.requestResult
import com.coffeepeek.domain.feature.feed.FeedFilters
import com.coffeepeek.domain.feature.feed.FeedRepository
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.RoasterSummary
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.ShopRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable

internal class PublicProfileRepositoryImpl(
    private val client: HttpClient,
    private val feed: FeedRepository,
    private val shops: ShopRepository? = null,
    private val roasters: RoasterRepository? = null,
) : PublicProfileRepository {
    override suspend fun getProfile(slug: String): Result<PublicProfile> = requestResult {
        validateSlug(slug)
        val response = client.get("/api/Users") {
            expectSuccess = false
            url { appendPathSegments(slug, encodeSlash = true) }
            header(HttpHeaders.CacheControl, "no-store")
        }
        response.requireSuccess("Не удалось загрузить профиль")
        val envelope = response.body<PublicProfileResponseDto>()
        val dto = checkNotNull(envelope.data) { "Профиль не найден" }
        val canonicalSlug = envelope.address?.slug?.takeIf(String::isNotBlank) ?: slug
        PublicProfile(
            slug = canonicalSlug,
            nickname = dto.userName.ifBlank { canonicalSlug },
            about = dto.about,
            avatarUrl = dto.avatarUrl?.resolveAgainst(response.call.request.url),
            stats = PublicProfileStats(dto.checkInCount, dto.addedShopsCount, dto.addedRoastersCount, dto.followersCount),
            favorites = emptyList(),
            activity = null,
            canonicalPath = envelope.address?.canonicalPath,
            isFollowing = dto.isFollowing,
        )
    }

    override suspend fun setFollowing(slug: String, isFollowing: Boolean): Result<PublicProfileFollowState> = requestResult {
        validateSlug(slug)
        val response = if (isFollowing) client.put("/api/Users") {
            expectSuccess = false
            url { appendPathSegments(slug, "follow", encodeSlash = true) }
            header(HttpHeaders.CacheControl, "no-store")
        } else client.delete("/api/Users") {
            expectSuccess = false
            url { appendPathSegments(slug, "follow", encodeSlash = true) }
            header(HttpHeaders.CacheControl, "no-store")
        }
        response.requireSuccess("Не удалось изменить подписку")
        val dto = response.body<PublicFollowDto>()
        PublicProfileFollowState(dto.isFollowing, dto.followersCount)
    }

    override suspend fun getFavorites(slug: String): Result<List<PublicProfileFavorite>> = requestResult {
        validateSlug(slug)
        val response = client.get("/api/v1/profiles") {
            expectSuccess = false
            url { appendPathSegments(slug, "favorites", encodeSlash = true) }
            header(HttpHeaders.CacheControl, "no-store")
        }
        response.requireSuccess("Не удалось загрузить избранное")
        val items = response.body<ProfileEnvelopeDto<List<PublicFavoriteDto>>>().requireData()
        val roasterCatalog = if (items.any { it.kind == "roaster" }) loadRoasterCatalog() else emptyMap()
        val permits = Semaphore(4)
        coroutineScope {
            items.mapIndexed { index, dto -> async {
                val kind = when (dto.kind) {
                    "coffee_shop" -> PublicFavoriteKind.CoffeeShop
                    "roaster" -> PublicFavoriteKind.Roaster
                    "drink" -> PublicFavoriteKind.Drink
                    else -> return@async null
                }
                val address = dto.address?.takeIf { kind != PublicFavoriteKind.Drink && it.slug.isNotBlank() }?.toDomain()
                val drinkSlug = dto.drink?.slug?.takeIf { kind == PublicFavoriteKind.Drink && it.isNotBlank() }
                val targetSlug = if (kind == PublicFavoriteKind.Drink) drinkSlug else address?.slug
                val summary = targetSlug?.let(roasterCatalog::get)?.takeIf { kind == PublicFavoriteKind.Roaster }
                val favorite = PublicProfileFavorite(
                    id = targetSlug?.let { "${dto.kind}:$it" } ?: "${dto.kind}:${dto.createdAtUtc}:${dto.name}:$index",
                    title = dto.name,
                    kind = kind,
                    imageUrl = dto.coverPhoto?.imageUrl()?.resolveAgainst(response.call.request.url) ?: summary?.photoUrl,
                    slug = targetSlug,
                    publicAddress = address,
                    drinkSlug = drinkSlug,
                    tags = summary?.tags?.map { it.name }.orEmpty(),
                    coffeeShopsCount = summary?.coffeeShopsCount,
                    coffeeProductsCount = summary?.coffeeProductsCount,
                    availableCoffeeProducts = summary?.availableCoffeeProducts,
                )
                if (targetSlug == null) favorite else permits.withPermit { hydrate(favorite) }
            } }.awaitAll().filterNotNull()
        }
    }

    private suspend fun loadRoasterCatalog(): Map<String, RoasterSummary> = try {
        roasters?.getRoasters()?.getOrNull().orEmpty().mapNotNull { summary ->
            summary.publicAddress?.slug?.let { it to summary }
        }.toMap()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyMap()
    }

    private suspend fun hydrate(favorite: PublicProfileFavorite): PublicProfileFavorite = try { when (favorite.kind) {
        PublicFavoriteKind.CoffeeShop -> shops?.getShopDetails(favorite.slug.orEmpty())?.getOrNull()?.let { details ->
            favorite.copy(
                imageUrl = favorite.imageUrl ?: details.shop.photoUrl,
                description = details.description,
                address = details.location?.address ?: details.shop.address,
                rating = details.shop.rating,
                reviewCount = details.shop.reviewCount,
                isOpen = details.shop.isOpen,
                tags = details.shop.tags,
            )
        } ?: favorite
        PublicFavoriteKind.Roaster -> roasters?.getRoaster(favorite.slug.orEmpty())?.getOrNull()?.let { details ->
            favorite.copy(imageUrl = favorite.imageUrl ?: details.photos.firstOrNull()?.fullUrl, description = details.about)
        } ?: favorite
        PublicFavoriteKind.Drink -> favorite
    } } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        favorite
    }

    override suspend fun getActivity(slug: String, year: Int, month: Int, timeZone: String): Result<PublicProfileActivity> = requestResult {
        validateSlug(slug)
        require(year in 2000..2100 && month in 1..12 && timeZone.isNotBlank())
        val response = client.get("/api/v1/profiles") {
            expectSuccess = false
            url { appendPathSegments(slug, "activity", encodeSlash = true) }
            parameter("year", year)
            parameter("month", month)
            parameter("timeZone", timeZone)
            header(HttpHeaders.CacheControl, "no-store")
        }
        response.requireSuccess("Не удалось загрузить активность")
        val dto = response.body<ProfileEnvelopeDto<PublicActivityDto>>().requireData()
        val period = "${dto.year.toString().padStart(4, '0')}-${dto.month.toString().padStart(2, '0')}"
        val counts = dto.days.filter { it.date.startsWith("$period-") }.associate {
            it.date.takeLast(2).toInt() to it.checkInCount
        }
        PublicProfileActivity(period, counts.filterValues { it > 0 }.keys, dto.timeZone, dto.totalCheckIns, counts)
    }

    override suspend fun getConnections(slug: String, kind: PublicProfileConnectionKind, page: Int, pageSize: Int): Result<PublicProfileConnectionsPage> = requestResult {
        validateSlug(slug)
        require(page in 1..10_000 && pageSize in 1..100)
        val response = client.get("/api/Users") {
            expectSuccess = false
            url { appendPathSegments(slug, if (kind == PublicProfileConnectionKind.Followers) "followers" else "following", encodeSlash = true) }
            parameter("page", page)
            parameter("pageSize", pageSize)
            header(HttpHeaders.CacheControl, "no-store")
        }
        response.requireSuccess("Не удалось загрузить пользователей")
        val dto = response.body<PublicConnectionsDto>()
        PublicProfileConnectionsPage(dto.items.map { person ->
            PublicProfileConnection(person.userName, person.avatarUrl?.resolveAgainst(response.call.request.url), person.address.slug, person.address.canonicalPath)
        }, dto.totalCount, dto.page, dto.pageSize)
    }

    override suspend fun getCheckIns(slug: String, cursor: String?): Result<PublicProfileCheckInPage> = requestResult {
        validateSlug(slug)
        val page = feed.getFeed(20, cursor, FeedFilters(authorSlug = slug)).getOrThrow()
        PublicProfileCheckInPage(page.items.map { it.checkIn }.filter {
            it.authorAddress?.slug == slug && it.visibility == CheckInVisibility.Public && it.moderationState == CheckInModerationState.Approved
        }.map(CheckIn::toPublicProfileCheckIn), page.nextCursor)
    }
}

private fun HttpResponse.requireSuccess(message: String) {
    if (status == HttpStatusCode.Unauthorized) throw PublicProfileAuthenticationRequired()
    if (status == HttpStatusCode.NotFound) throw PublicProfileUnavailable()
    check(status.isSuccess()) { message }
}

private fun validateSlug(slug: String) { require(slug.isNotBlank() && slug.length <= 100) { "Некорректный адрес профиля" } }

private fun CheckIn.toPublicProfileCheckIn(): PublicProfileCheckIn {
    val recipe = homePreparation
    val drink = customDrinkName?.takeIf(String::isNotBlank) ?: drinkNameRu?.takeIf(String::isNotBlank) ?: drinkNameEn?.takeIf(String::isNotBlank)
    return PublicProfileCheckIn(
        id, shopAddress?.slug?.takeIf(String::isNotBlank)?.takeUnless { isHome },
        if (isHome) "Дома" else shopName.ifBlank { "Кофейня" },
        if (isHome) recipe?.beanName?.takeIf(String::isNotBlank) ?: "Кофе дома" else drink ?: "Чекин в кофейне",
        if (isHome) listOfNotNull(recipe?.brewMethodCode, recipe?.doseGrams?.let { "$it г" }, recipe?.brewDurationSeconds?.let { "$it с" }).joinToString(" · ").takeIf(String::isNotBlank) else drink,
        if (isHome) recipe?.tasteScore?.toDouble() else rating?.average,
        note,
        if (isHome) recipe?.preparedOn?.takeIf(String::isNotBlank) ?: visitedAt.ifBlank { createdAt } else visitedAt.ifBlank { createdAt },
        photoUrls,
        source = this,
    )
}

private fun String.resolveAgainst(base: Url): String? = takeIf(String::isNotBlank)?.let { reference ->
    URLBuilder(base).apply {
        // Pagination/filter parameters belong to the API request, never to its image URLs.
        parameters.clear()
        fragment = ""
        takeFrom(reference)
    }.buildString()
}

@Serializable private data class PublicProfileResponseDto(val data: PublicProfileDto?, val address: ProfileAddressDto? = null)
@Serializable private data class PublicProfileDto(
    val userName: String, val about: String? = null, val avatarUrl: String? = null,
    val checkInCount: Int, val addedShopsCount: Int, val addedRoastersCount: Int, val followersCount: Int, val isFollowing: Boolean,
)
@Serializable private data class ProfileAddressDto(val slug: String, val canonicalPath: String, val revision: Int = 0, val isAlias: Boolean = false) {
    fun toDomain() = PublicAddress(slug, canonicalPath, revision, isAlias)
}
@Serializable private data class ProfileEnvelopeDto<T>(val data: T? = null, val isSuccess: Boolean? = null, val message: String? = null) {
    fun requireData(): T { check(isSuccess != false) { message ?: "Не удалось загрузить данные" }; return checkNotNull(data) { message ?: "Ответ сервера не содержит данных" } }
}
@Serializable private data class PublicFollowDto(val followersCount: Int, val isFollowing: Boolean)
@Serializable private data class PublicFavoriteDto(val kind: String, val name: String, val createdAtUtc: String, val coverPhoto: PublicFavoritePhotoDto? = null, val address: ProfileAddressDto? = null, val drink: PublicDrinkDto? = null)
@Serializable private data class PublicDrinkDto(val slug: String)
@Serializable private data class PublicFavoritePhotoDto(val fullUrl: String? = null, val urls: PublicFavoritePhotoUrlsDto? = null) {
    fun imageUrl(): String? = urls?.card?.takeIf(String::isNotBlank) ?: fullUrl?.takeIf(String::isNotBlank)
}
@Serializable private data class PublicFavoritePhotoUrlsDto(val card: String? = null)
@Serializable private data class PublicActivityDto(val year: Int, val month: Int, val timeZone: String, val activeDays: Int, val totalCheckIns: Int, val days: List<PublicActivityDayDto>)
@Serializable private data class PublicActivityDayDto(val date: String, val checkInCount: Int)
@Serializable private data class PublicConnectionsDto(val items: List<PublicConnectionDto>, val totalCount: Int, val page: Int, val pageSize: Int)
@Serializable private data class PublicConnectionDto(val userName: String, val avatarUrl: String? = null, val address: ProfileAddressDto)
