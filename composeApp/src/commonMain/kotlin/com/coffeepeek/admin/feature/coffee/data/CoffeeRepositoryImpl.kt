package com.coffeepeek.admin.feature.coffee.data

import com.coffeepeek.admin.feature.coffee.domain.Coffee
import com.coffeepeek.admin.feature.coffee.domain.CoffeeAvailability
import com.coffeepeek.admin.feature.coffee.domain.CoffeeDetails
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilterGroup
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilterOption
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilters
import com.coffeepeek.admin.feature.coffee.domain.CoffeeOffer
import com.coffeepeek.admin.feature.coffee.domain.CoffeePage
import com.coffeepeek.admin.feature.coffee.domain.CoffeeRepository
import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val filterCodes = setOf("brew", "caffeine", "roast", "acidity", "processing", "fermentation", "taste", "composition")

internal class CoffeeRepositoryImpl(private val client: HttpClient) : CoffeeRepository {
    override suspend fun getDetails(slug: String): Result<CoffeeDetails> = requestResult {
        require(slug.isNotBlank() && slug.length <= 200)
        val response = client.get("/api/v1/coffees") { url { appendPathSegments(slug, encodeSlash = true) } }
        check(response.status.isSuccess()) { "Coffee details failed: ${response.status.value}" }
        response.body<CoffeeResponseDto<CoffeeDto>>().requireData().toDetails()
    }

    override suspend fun search(query: String, filters: CoffeeFilters, page: Int): Result<CoffeePage> = requestResult {
        require(query.length <= 100 && page in 1..100_000)
        val response = client.post("/api/v1/coffees/search") {
            contentType(ContentType.Application.Json)
            setBody(CoffeeSearchRequestDto(query.trim(), filters.toJson(), page))
        }
        check(response.status.isSuccess()) { "Coffee search failed: ${response.status.value}" }
        val data = response.body<CoffeeResponseDto<CoffeePageDto>>().requireData()
        CoffeePage(data.items.map { it.toDomain() }, data.currentPage, data.totalPages)
    }

    override suspend fun getFilterGroups(): Result<List<CoffeeFilterGroup>> = requestResult {
        val response = client.get("/api/v1/catalogs/coffee-filter-values")
        check(response.status.isSuccess()) { "Coffee filters failed: ${response.status.value}" }
        response.body<CoffeeResponseDto<List<CoffeeFilterGroupDto>>>().requireData()
            .filter { it.code in filterCodes }
            .map { group ->
                CoffeeFilterGroup(group.code, group.name, group.values.sortedBy { it.sortOrder }.map {
                    CoffeeFilterOption(it.code, it.name)
                })
            }
    }
}

private fun CoffeeFilters.toJson(): JsonObject = buildJsonObject {
    values.forEach { (code, selected) ->
        require(code in filterCodes)
        if (selected.isNotEmpty()) put(code, JsonArray(selected.sorted().map(::JsonPrimitive)))
    }
    put("availableOnly", availableOnly)
}

@Serializable
private data class CoffeeSearchRequestDto(val q: String, val filters: JsonObject, val page: Int, val pageSize: Int = 20)

@Serializable
private data class CoffeeResponseDto<T>(val isSuccess: Boolean, val data: T? = null, val message: String? = null) {
    fun requireData(): T {
        check(isSuccess) { message ?: "Coffee request failed" }
        return checkNotNull(data) { "Coffee response has no data" }
    }
}

@Serializable
private data class CoffeePageDto(val items: List<CoffeeDto>, val currentPage: Int, val totalPages: Int)

@Serializable
private data class CoffeeDto(
    val address: CoffeeAddressDto,
    val name: String,
    val roaster: CoffeeRoasterDto,
    val coverPhoto: CoffeePhotoDto? = null,
    val countries: List<CoffeeCountryDto> = emptyList(),
    val classification: CoffeeClassificationDto? = null,
    val matchingOffers: List<CoffeeOfferDto> = emptyList(),
    val description: String? = null,
    val photos: List<CoffeePhotoDto> = emptyList(),
    val offers: List<CoffeeOfferDto> = emptyList(),
    val productKind: String? = null,
    val productForm: String? = null,
    val tasteDescriptors: List<String> = emptyList(),
    val catalogCheckedAtUtc: String? = null,
) {
    fun toDomain() = Coffee(
        slug = address.slug,
        name = name,
        photoUrl = coverPhoto?.cardUrl ?: photos.firstOrNull()?.cardUrl,
        roasterName = roaster.name,
        roasterPhotoUrl = roaster.coverPhoto?.thumbnailUrl,
        countries = countries.map { it.nameRu ?: it.nameEn ?: it.code },
        tasteCodes = classification?.tasteGroups.orEmpty(),
        offers = matchingOffers.ifEmpty { offers }.map { it.toDomain() },
    )

    fun toDetails() = CoffeeDetails(
        coffee = toDomain(),
        canonicalPath = address.canonicalPath?.takeIf { it.startsWith("/coffees/") } ?: "/coffees/${address.slug}",
        description = description?.takeIf(String::isNotBlank),
        photos = photos.mapNotNull { it.detailUrl }.ifEmpty { listOfNotNull(coverPhoto?.detailUrl) },
        roasterSlug = roaster.address?.slug,
        productKind = productKind,
        productForm = productForm,
        roastLevel = classification?.roastLevel,
        acidity = classification?.acidity,
        tasteDescriptors = tasteDescriptors,
        checkedAtUtc = catalogCheckedAtUtc,
    )
}

@Serializable
private data class CoffeeAddressDto(val slug: String, val canonicalPath: String? = null)

@Serializable
private data class CoffeeRoasterDto(val name: String, val coverPhoto: CoffeePhotoDto? = null, val address: CoffeeAddressDto? = null)

@Serializable
private data class CoffeePhotoDto(val fullUrl: String? = null, val urls: CoffeePhotoUrlsDto? = null) {
    val cardUrl: String? get() = urls?.card ?: fullUrl
    val thumbnailUrl: String? get() = urls?.thumbnail ?: fullUrl
    val detailUrl: String? get() = urls?.detail ?: fullUrl
}

@Serializable
private data class CoffeePhotoUrlsDto(val card: String? = null, val thumbnail: String? = null, val detail: String? = null)

@Serializable
private data class CoffeeCountryDto(val code: String, val nameRu: String? = null, val nameEn: String? = null)

@Serializable
private data class CoffeeClassificationDto(
    val tasteGroups: List<String> = emptyList(),
    val roastLevel: String? = null,
    val acidity: String? = null,
)

@Serializable
private data class CoffeeOfferDto(
    val price: Double? = null,
    val currency: String? = null,
    val weightGrams: Int? = null,
    val offerKey: String? = null,
    val availability: String? = null,
    val sellerName: String? = null,
    val sourceUrl: String? = null,
    val grind: String? = null,
    val brewPurpose: String? = null,
    val availabilityScope: String? = null,
    val checkedAtUtc: String? = null,
) {
    fun toDomain() = CoffeeOffer(
        price = price?.takeIf { it.isFinite() && it >= 0 },
        currency = currency,
        weightGrams = weightGrams?.takeIf { it > 0 },
        key = offerKey,
        availability = CoffeeAvailability.entries.firstOrNull { it.name == availability } ?: CoffeeAvailability.Unknown,
        sellerName = sellerName,
        sourceUrl = sourceUrl?.takeIf { value ->
            runCatching { Url(value) }.getOrNull()?.let { it.protocol in setOf(URLProtocol.HTTP, URLProtocol.HTTPS) && it.host.isNotBlank() } == true
        },
        grind = grind,
        brewPurpose = brewPurpose,
        availabilityScope = availabilityScope,
        checkedAtUtc = checkedAtUtc,
    )
}

@Serializable
private data class CoffeeFilterGroupDto(val code: String, val name: String, val values: List<CoffeeFilterValueDto>)

@Serializable
private data class CoffeeFilterValueDto(val code: String, val name: String, val sortOrder: Int = 0)
