package com.coffeepeek.data.repository

import com.coffeepeek.domain.model.ConsumedDrinkOption

import com.coffeepeek.api.model.request.CreateShopContactReq
import com.coffeepeek.api.model.request.CreateShopReq
import com.coffeepeek.api.model.request.ScheduleIntervalReq
import com.coffeepeek.api.model.request.ScheduleReq
import com.coffeepeek.api.service.ShopApiService
import com.coffeepeek.data.mapper.ShopMapper.parseShopType
import com.coffeepeek.data.mapper.ShopMapper.toDomain
import com.coffeepeek.data.mapper.toDomain
import com.coffeepeek.data.mapper.toDto
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.data.time.localSchedulesToUtc
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.City
import com.coffeepeek.domain.model.CoffeeDrinkDefinition
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.CoffeeShopType
import com.coffeepeek.domain.model.CreateShopInput
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapContent
import com.coffeepeek.domain.model.MapShop
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.ShopCatalogs
import com.coffeepeek.domain.model.ShopFilters
import com.coffeepeek.domain.model.ShopMenu
import com.coffeepeek.domain.model.ShopSubmission
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.PhotoRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.domain.repository.RoasterRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ShopRepositoryImpl(
    private val shopApiService: ShopApiService,
    private val photoRepository: PhotoRepository,
    private val favoriteRepository: FavoriteRepository,
    private val fileUrlResolver: FileUrlResolver,
    private val roasterRepository: RoasterRepository,
) : ShopRepository {

    private var cachedCatalogs: ShopCatalogs? = null
    private val catalogsMutex = Mutex()
    private var catalogsLoad: Deferred<Result<ShopCatalogs>>? = null
    private var cachedMenuDrinks: List<CoffeeDrinkDefinition>? = null
    private val menuDrinksMutex = Mutex()

    override suspend fun getCatalogs(): Result<ShopCatalogs> = coroutineScope {
        cachedCatalogs?.let { return@coroutineScope Result.success(it) }
        val load = catalogsMutex.withLock {
            cachedCatalogs?.let { return@coroutineScope Result.success(it) }
            catalogsLoad ?: async { fetchCatalogsFromApi() }.also { catalogsLoad = it }
        }
        load.await().also { result ->
            catalogsMutex.withLock {
                if (catalogsLoad === load) catalogsLoad = null
            }
        }
    }

    private suspend fun fetchCatalogsFromApi(): Result<ShopCatalogs> = runCatching {
        coroutineScope {
            val cities      = async { shopApiService.getCities().getOrThrow() }
            val beans       = async { shopApiService.getBeans().getOrThrow() }
            val equipment   = async { shopApiService.getEquipment().getOrThrow() }
            val roasters    = async { roasterRepository.getRoasters().getOrThrow() }
            val brewMethods = async { shopApiService.getBrewMethods().getOrThrow() }
            val shopTags    = async { shopApiService.getShopTags().getOrThrow() }
            ShopCatalogs(
                cities      = cities.await().map { City(it.address.slug, it.name, it.address.toDomain()) },
                beans       = beans.await().map { CatalogItem(it.key, it.name.orEmpty(), it.key) },
                equipment   = equipment.await().map { CatalogItem(it.key, it.name.orEmpty(), it.key) },
                roasters    = roasters.await().filter { it.publicAddress != null }.map { it.toCatalogItem() },
                brewMethods = brewMethods.await().map { CatalogItem(it.key, it.name.orEmpty(), it.key) },
                shopTags    = shopTags.await().map { CatalogItem(it.key, it.name.orEmpty(), it.key) },
            ).also { cachedCatalogs = it }
        }
    }

    override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> =
        shopApiService.searchShops(
            query = filters.query,
            cityId = filters.cityId,
            type = filters.coffeeFocus?.let(CoffeeShopType::toApi),
            roasterIds = filters.roasterIds.takeIf { it.isNotEmpty() },
            equipmentIds = filters.equipmentIds.takeIf { it.isNotEmpty() },
            beanIds = filters.beanIds.takeIf { it.isNotEmpty() },
            brewMethodIds = filters.brewMethodIds.takeIf { it.isNotEmpty() },
            tagIds = filters.tagIds.takeIf { it.isNotEmpty() },
            priceRange = filters.priceRange.toApiPriceRange(),
            minRating = filters.minRating,
            page = filters.page,
            pageSize = filters.pageSize,
        ).map { dto ->
            val favoriteIds = favoriteRepository.getFavoriteIds()
            PagedResult(
                items = dto.coffeeShops.map { shop ->
                    shop.toDomain().copy(isFavorite = shop.address.slug in favoriteIds)
                },
                totalCount = dto.totalItems,
                totalPages = dto.totalPages,
                currentPage = dto.currentPage,
            )
        }

    override suspend fun getConsumedDrinks() = shopApiService.getConsumedDrinks().map { options ->
        options.map { ConsumedDrinkOption(it.slug, it.nameRu, it.nameEn) }
    }

    override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> {
        cachedMenuDrinks?.let { return Result.success(it) }
        return menuDrinksMutex.withLock {
            cachedMenuDrinks?.let { return@withLock Result.success(it) }
            shopApiService.getMenuDrinks().map { drinks ->
                drinks.map {
                    CoffeeDrinkDefinition(
                        slug = it.slug,
                        nameRu = it.nameRu,
                        nameEn = it.nameEn,
                        category = it.category,
                        sortOrder = it.sortOrder,
                    )
                }.sortedBy { it.sortOrder }.also { cachedMenuDrinks = it }
            }
        }
    }

    override suspend fun getShopDetails(id: String): Result<CoffeeShopDetails> = runCatching {
        coroutineScope {
            val detailsDeferred = async { shopApiService.getShopDetails(id).getOrThrow() }
            val drinksDeferred = async { getMenuDrinks().getOrNull() }
            val domain = detailsDeferred.await().toDomain(fileUrlResolver)
            val drinks = drinksDeferred.await()
            domain.copy(
                shop = domain.shop.copy(isFavorite = favoriteRepository.isFavorite(domain.shop.id)),
                menu = domain.menu?.alignedWithCatalog(drinks),
            )
        }
    }

    override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> =
        coroutineScope {
            val visibleZoom = zoom.toInt().coerceIn(0, 22)
            val shopsRequest = async { requestMap(bounds, 22, filters) }
            val zonesRequest = if (visibleZoom == 22) null else async { requestMap(bounds, visibleZoom, filters) }
            val zonesResponse = zonesRequest?.await()?.getOrNull()

            shopsRequest.await().map { response ->
                val mapped = response.shops.map { dto ->
                    MapShop(
                        id = dto.address.slug,
                        publicAddress = dto.address.toDomain(),
                        title = dto.title?.takeIf { it.isNotBlank() } ?: "Кофейня",
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        type = parseShopType(dto.type),
                        primaryZoneId = dto.primaryZone?.slug,
                    )
                }
                val focus = filters.coffeeFocus
                MapContent(
                    shops = if (focus.isNullOrBlank()) mapped else mapped.filter { it.type == focus },
                    clusters = emptyList(),
                    zones = (zonesResponse ?: response).zones.map { zone ->
                        MapCoffeeZone(
                            id = zone.address.slug,
                            publicAddress = zone.address.toDomain(),
                            name = zone.name,
                            description = zone.description,
                            color = zone.color,
                            latitude = zone.latitude,
                            longitude = zone.longitude,
                            radiusMeters = zone.radiusMeters,
                            shopCount = zone.shopCount,
                            polygon = zone.polygon.map { it.latitude to it.longitude },
                        )
                    },
                    isTruncated = response.isTruncated,
                )
            }
        }

    private suspend fun requestMap(bounds: MapBounds, zoom: Int, filters: ShopFilters) =
        shopApiService.getShopsInBounds(
            minLat = bounds.minLat,
            minLon = bounds.minLon,
            maxLat = bounds.maxLat,
            maxLon = bounds.maxLon,
            zoom = zoom,
            cityId = filters.cityId,
            type = filters.coffeeFocus?.let(CoffeeShopType::toApi),
            roasterIds = filters.roasterIds.takeIf { it.isNotEmpty() },
            equipmentIds = filters.equipmentIds.takeIf { it.isNotEmpty() },
            beanIds = filters.beanIds.takeIf { it.isNotEmpty() },
            brewMethodIds = filters.brewMethodIds.takeIf { it.isNotEmpty() },
            tagIds = filters.tagIds.takeIf { it.isNotEmpty() },
            priceRange = filters.priceRange.toApiPriceRange(),
            minRating = filters.minRating,
        )

    override suspend fun getMyShopSubmissions(
        status: ModerationStatus,
        page: Int,
        pageSize: Int,
    ): Result<PagedResult<ShopSubmission>> =
        shopApiService.getMyModerationShops(status.toDto(), page, pageSize).map { response ->
            PagedResult(
                items = response.moderationShops.map {
                    ShopSubmission(
                        id = it.id,
                        name = it.name,
                        address = it.address,
                        status = it.moderationStatus.toDomain(),
                        rejectedReason = it.rejectedReason,
                        publishedShopId = it.publishedShop?.slug,
                    )
                },
                totalCount = response.totalItems,
                totalPages = response.totalPages,
                currentPage = response.currentPage,
            )
        }

    override suspend fun createShop(input: CreateShopInput): Result<Unit> = runCatching {
        val (uploadedPhotos, uploadedMenuPhotos) = coroutineScope {
            val shopPhotos = async { photoRepository.uploadShopPhotos(input.photos).getOrThrow() }
            val menuPhotos = async { photoRepository.uploadMenuPhotos(input.menuPhotos.take(4)).getOrThrow() }
            shopPhotos.await() to menuPhotos.await()
        }

        shopApiService.createShop(
            CreateShopReq(
                name        = input.name,
                address     = input.address,
                cityId      = input.cityId,
                latitude    = input.latitude,
                longitude   = input.longitude,
                description = input.description?.takeIf { it.isNotBlank() },
                shopContact = if (listOf(input.phone, input.email, input.website, input.instagram).any { !it.isNullOrBlank() }) {
                    CreateShopContactReq(
                        phoneNumber   = input.phone?.takeIf { it.isNotBlank() },
                        email         = input.email?.takeIf { it.isNotBlank() },
                        siteLink      = input.website?.takeIf { it.isNotBlank() },
                        instagramLink = input.instagram?.takeIf { it.isNotBlank() },
                    )
                } else null,
                schedules = localSchedulesToUtc(input.schedules).takeIf { it.isNotEmpty() }?.map { schedule ->
                    ScheduleReq(
                        dayOfWeek = schedule.dayOfWeek.toApiDayOfWeek(),
                        isClosed = schedule.isClosed,
                        intervals = schedule.intervals.takeIf { it.isNotEmpty() }?.map { interval ->
                            ScheduleIntervalReq(
                                openTime = interval.openTime,
                                closeTime = interval.closeTime,
                            )
                        },
                    )
                },
                shopPhotos = uploadedPhotos.toUploadedPhotoReqs().takeIf { it.isNotEmpty() },
                menuPhotos = uploadedMenuPhotos.toUploadedPhotoReqs().takeIf { it.isNotEmpty() },
                priceRange = input.priceRange.toApiPriceRange(),
                equipmentIds  = input.equipmentIds.takeIf { it.isNotEmpty() },
                coffeeBeanIds = input.coffeeBeanIds.takeIf { it.isNotEmpty() },
                roasterIds    = input.roasterIds.takeIf { it.isNotEmpty() },
                brewMethodIds = input.brewMethodIds.takeIf { it.isNotEmpty() },
            )
        ).getOrThrow()
    }
}

private fun ShopMenu.alignedWithCatalog(drinks: List<CoffeeDrinkDefinition>?): ShopMenu {
    if (drinks.isNullOrEmpty()) return this
    val bySlug = drinks.associateBy { it.slug }
    val order = drinks.mapIndexed { index, drink -> drink.slug to index }.toMap()
    return copy(
        items = items
            .map { item ->
                val def = bySlug[item.slug] ?: return@map item
                item.copy(
                    nameRu = item.nameRu.ifBlank { def.nameRu },
                    nameEn = item.nameEn.ifBlank { def.nameEn },
                    category = item.category.ifBlank { def.category },
                )
            }
            .sortedBy { order[it.slug] ?: Int.MAX_VALUE },
    )
}

private fun Int?.toApiPriceRange(): String? = when (this) {
    null -> null
    1 -> "Cheap"
    2 -> "Moderate"
    3 -> "Expensive"
    4 -> "Luxury"
    else -> null
}

private fun Int.toApiDayOfWeek(): String = when (this) {
    0 -> "Sunday"
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    else -> "Monday"
}
