@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.homecheckin.data

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.homecheckin.domain.HomeBeanStats
import com.coffeepeek.feature.homecheckin.domain.HomeCalendarDay
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInInput
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPage
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPhoto
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInRepository
import com.coffeepeek.feature.homecheckin.domain.HomeMethodStats
import com.coffeepeek.feature.homecheckin.domain.HomePhotoUpload
import com.coffeepeek.feature.homecheckin.domain.validateHomeCheckIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val PATH = "/api/v1/check-ins/home"

/** Data details stay here. [client] is the application's authenticated gateway client. */
class HomeCheckInRepositoryImpl(
    private val client: HttpClient,
    private val uploadClient: HttpClient,
    private val baseUrl: String,
) : HomeCheckInRepository {
    override suspend fun create(input: HomeCheckInInput): Result<HomeCheckIn> = requestResult {
        validateHomeCheckIn(input)?.let { throw IllegalArgumentException(it) }
        val originals = upload(input.photos)
        val cutout = input.drinkPhoto?.let { upload(listOf(it)).single() }
        client.post(PATH) { contentType(ContentType.Application.Json); setBody(input.toCreateRequest(originals, cutout)) }
            .data<HomeDto>().toDomain()
    }

    override suspend fun history(page: Int, pageSize: Int, from: String?, to: String?): Result<HomeCheckInPage> = requestResult {
        val number = page.coerceIn(1, 10000)
        val size = pageSize.coerceIn(1, 100)
        val result = client.get(PATH) {
            parameter("pageNumber", number)
            parameter("pageSize", size)
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
        }.data<HomePageDto>()
        HomeCheckInPage(result.items.map { it.toDomain() }, result.totalCount, number, size)
    }

    override suspend fun calendar(from: String, to: String): Result<List<HomeCalendarDay>> = requestResult {
        client.get("$PATH/calendar") { parameter("from", from); parameter("to", to) }
            .data<List<HomeCalendarDto>>().map { HomeCalendarDay(it.preparedOn, it.brewCount) }
    }

    override suspend fun detail(id: String): Result<HomeCheckIn> = requestResult {
        client.get(entryPath(id)).data<HomeDto>().toDomain()
    }

    override suspend fun update(id: String, input: HomeCheckInInput): Result<HomeCheckIn> = requestResult {
        validateHomeCheckIn(input, editing = true)?.let { throw IllegalArgumentException(it) }
        client.put(entryPath(id)) { contentType(ContentType.Application.Json); setBody(input.toEditRequest()) }
            .data<HomeDto>().toDomain()
    }

    override suspend fun delete(id: String): Result<Unit> = requestResult {
        val response = client.delete(entryPath(id))
        response.successEnvelope()
    }

    override suspend fun beanStats(): Result<List<HomeBeanStats>> = requestResult {
        client.get("$PATH/stats/beans").data<List<HomeBeanStatsDto>>().map {
            HomeBeanStats(it.coffeeProductId, it.beanName, it.brewCount, it.dosedBrewCount,
                it.totalKnownDoseGrams.toString(), it.ratedBrewCount, it.averageTasteScore)
        }
    }

    override suspend fun methodStats(): Result<List<HomeMethodStats>> = requestResult {
        client.get("$PATH/stats/methods").data<List<HomeMethodStatsDto>>().map {
            HomeMethodStats(it.brewMethodCode, it.brewCount, it.dosedBrewCount,
                it.totalKnownDoseGrams.toString(), it.ratedBrewCount, it.averageTasteScore)
        }
    }

    override suspend fun photo(url: String): Result<ByteArray> = requestResult {
        require(url.startsWith("/api/v1/check-ins/") && "/photos/" in url && !url.contains("..")) {
            "Некорректная ссылка на фото"
        }
        val response = client.get(url)
        if (!response.status.isSuccess()) throw IllegalStateException("Фото недоступно")
        response.body()
    }

    private suspend fun upload(photos: List<HomePhotoUpload>): List<UploadedPhotoDto> {
        if (photos.isEmpty()) return emptyList()
        photos.forEach {
            require(it.fileName.isNotBlank() && it.bytes.isNotEmpty() && it.contentType.startsWith("image/")) {
                "Некорректное фото"
            }
        }
        val requests = photos.map { PhotoUploadRequestDto(it.bytes.size, it.fileName, it.contentType) }
        val reservations = client.post("/api/Photos/check-in") {
            contentType(ContentType.Application.Json)
            setBody(requests)
        }.data<List<PhotoUploadUrlDto>>()
        check(reservations.size == photos.size) { "Сервер вернул неполный список URL для фото" }
        return reservations.zip(photos).map { (reservation, photo) ->
            require(reservation.uploadUrl.startsWith("https://") || reservation.uploadUrl.startsWith("http://")) {
                "Некорректный URL загрузки"
            }
            val response = uploadClient.put(reservation.uploadUrl) {
                headers {
                    remove(HttpHeaders.Accept)
                    remove(HttpHeaders.AcceptCharset)
                    append(HttpHeaders.ContentType, photo.contentType)
                    append("x-amz-tagging", "is_permanent=False")
                }
                setBody(photo.bytes)
            }
            check(response.status.isSuccess()) { "Не удалось загрузить фото" }
            UploadedPhotoDto(photo.fileName, photo.contentType, reservation.storageKey, photo.bytes.size.toLong())
        }
    }

    private fun entryPath(id: String): String {
        require(id.matches(Regex("^[0-9a-fA-F-]{36}$"))) { "Некорректный ID записи" }
        return "$PATH/$id"
    }

    private fun HomeDto.toDomain(): HomeCheckIn = HomeCheckIn(
        id = id, preparedAtUtc = preparedAtUtc, preparedOn = preparedOn,
        createdAtUtc = createdAtUtc, text = text, brewMethodCode = brewMethodCode,
        coffeeProductId = coffeeProductId, beanName = beanName,
        doseGrams = doseGrams?.toString(), brewDurationSeconds = brewDurationSeconds,
        tasteScore = tasteScore, drinkName = drink.displayName(),
        drinkSlug = drink.slug(), customDrinkName = drink.customName(),
        photos = photos.map { HomeCheckInPhoto(it.id, it.url.absoluteUrl()) },
        drinkPhotoUrl = drinkPhoto.urlOrNull()?.absoluteUrl(),
    )

    private fun String.absoluteUrl(): String = when {
        startsWith("https://") -> this
        startsWith("/") -> baseUrl.trimEnd('/') + this
        else -> baseUrl.trimEnd('/') + "/" + this
    }
}

private suspend inline fun <reified T> HttpResponse.data(): T {
    val envelope = body<HomeEnvelope<T>>()
    if (!status.isSuccess() || !envelope.isSuccess || envelope.data == null)
        throw IllegalStateException(envelope.message ?: "Не удалось выполнить запрос")
    return envelope.data
}

private suspend fun HttpResponse.successEnvelope() {
    val envelope = body<HomeEnvelope<JsonElement>>()
    if (!status.isSuccess() || !envelope.isSuccess)
        throw IllegalStateException(envelope.message ?: "Не удалось удалить запись")
}

private fun HomeCheckInInput.toEditRequest() = HomeEditDto(preparedAt, preparedOn, brewMethodCode, coffeeProductSlug, coffeeProductId,
    customBeanName?.trim(), doseGrams?.toDouble(), brewDurationSeconds, tasteScore,
    text?.trim(), drinkSlug, customDrinkName?.trim())

private fun HomeCheckInInput.toCreateRequest(photos: List<UploadedPhotoDto>, drinkPhoto: UploadedPhotoDto?) =
    HomeCreateDto(preparedAt, preparedOn, brewMethodCode, coffeeProductSlug, coffeeProductId,
        customBeanName?.trim(), doseGrams?.toDouble(), brewDurationSeconds, tasteScore,
        text?.trim(), drinkSlug, customDrinkName?.trim(), photos, drinkPhoto)

private fun JsonElement?.displayName(): String? = (this as? JsonObject)?.let {
    listOf("nameRu", "name", "customName", "customDrinkName").firstNotNullOfOrNull { key ->
        (it[key] as? JsonPrimitive)?.contentOrNull
    }
}

private fun JsonElement?.slug(): String? = ((this as? JsonObject)?.get("slug") as? JsonPrimitive)?.contentOrNull
private fun JsonElement?.customName(): String? = (this as? JsonObject)?.let {
    listOf("customName", "customDrinkName").firstNotNullOfOrNull { key ->
        (it[key] as? JsonPrimitive)?.contentOrNull
    }
}

private fun JsonElement?.urlOrNull(): String? = when (this) {
    is JsonPrimitive -> contentOrNull
    is JsonObject -> listOf("url", "fullUrl").firstNotNullOfOrNull { key ->
        (this[key] as? JsonPrimitive)?.contentOrNull
    }
    else -> null
}

@Serializable private data class HomeEnvelope<T>(val isSuccess: Boolean, val message: String? = null, val data: T? = null)
@Serializable private data class HomePageDto(val items: List<HomeDto> = emptyList(), val totalCount: Int = 0)
@Serializable private data class HomeCalendarDto(val preparedOn: String, val brewCount: Int)
@Serializable private data class HomePhotoDto(val id: String = "", @JsonNames("fullUrl") val url: String = "")
@Serializable private data class HomeDto(
    val id: String,
    val preparedAtUtc: String,
    val preparedOn: String,
    val createdAtUtc: String = "",
    val text: String? = null,
    val brewMethodCode: String? = null,
    val coffeeProductId: String? = null,
    val beanName: String? = null,
    val doseGrams: Double? = null,
    val brewDurationSeconds: Int? = null,
    val tasteScore: Int? = null,
    val drink: JsonElement? = null,
    val photos: List<HomePhotoDto> = emptyList(),
    val drinkPhoto: JsonElement? = null,
)
@Serializable private data class HomeEditDto(
    val preparedAt: String,
    val preparedOn: String?,
    val brewMethodCode: String?,
    val coffeeProductSlug: String?,
    val coffeeProductId: String?,
    val customBeanName: String?,
    val doseGrams: Double?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
    val text: String?,
    val drinkSlug: String?,
    val customDrinkName: String?,
)
@Serializable private data class HomeCreateDto(
    val preparedAt: String,
    val preparedOn: String?,
    val brewMethodCode: String?,
    val coffeeProductSlug: String?,
    val coffeeProductId: String?,
    val customBeanName: String?,
    val doseGrams: Double?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
    val text: String?,
    val drinkSlug: String?,
    val customDrinkName: String?,
    val photos: List<UploadedPhotoDto>,
    val drinkPhoto: UploadedPhotoDto?,
)
@Serializable private data class PhotoUploadRequestDto(val sizeBytes: Int, val fileName: String, val contentType: String)
@Serializable private data class PhotoUploadUrlDto(val uploadUrl: String, val storageKey: String)
@Serializable private data class UploadedPhotoDto(val fileName: String, val contentType: String, val storageKey: String, val size: Long)
@Serializable private data class HomeBeanStatsDto(
    val coffeeProductId: String? = null, val beanName: String? = null,
    val brewCount: Int, val dosedBrewCount: Int, val totalKnownDoseGrams: Double,
    val ratedBrewCount: Int, val averageTasteScore: Double? = null,
)
@Serializable private data class HomeMethodStatsDto(
    val brewMethodCode: String? = null, val brewCount: Int, val dosedBrewCount: Int,
    val totalKnownDoseGrams: Double, val ratedBrewCount: Int, val averageTasteScore: Double? = null,
)
