package com.coffeepeek.api.feature.feed

import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.response.CheckInDto
import com.coffeepeek.api.utils.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
data class FeedItemDto(val publishedAtUtc: String, val checkIn: CheckInDto)

@Serializable
data class FeedPageDto(val items: List<FeedItemDto>, val nextCursor: String?)

class FeedApiException(message: String, val restartPagination: Boolean) : Exception(message)

class FeedApiService(private val client: HttpClient) {
    suspend fun getFeed(
        pageSize: Int,
        cursor: String? = null,
        citySlug: String? = null,
        coffeeShopSlug: String? = null,
        authorSlug: String? = null,
    ): Result<FeedPageDto> = try {
        require(pageSize in 1..100) { "Размер страницы должен быть от 1 до 100" }
        listOf(citySlug, coffeeShopSlug, authorSlug).filterNotNull().forEach {
            require(it.length in 1..100) { "Некорректный фильтр ленты" }
        }
        val response = client.get("/api/v1/feed") {
            expectSuccess = false
            // The timeline and personalized votes must always be read afresh.
            header(HttpHeaders.CacheControl, "no-store")
            parameter("pageSize", pageSize)
            cursor?.let { parameter("cursor", it) }
            citySlug?.let { parameter("citySlug", it) }
            coffeeShopSlug?.let { parameter("coffeeShopSlug", it) }
            authorSlug?.let { parameter("authorSlug", it) }
        }
        if (!response.status.isSuccess()) {
            val message = try {
                response.body<ApiResponse<kotlinx.serialization.json.JsonElement>>().message
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            throw FeedApiException(message?.takeIf(String::isNotBlank) ?: when (response.status) {
                HttpStatusCode.BadRequest -> "Лента изменилась. Обновите её с начала"
                HttpStatusCode.NotFound -> if (listOf(citySlug, coffeeShopSlug, authorSlug).any { it != null })
                    "Выбранный город, кофейня или автор не найдены" else "Лента недоступна. Попробуйте позже"
                HttpStatusCode.Unauthorized -> "Не удалось подтвердить вход. Попробуйте войти снова"
                else -> "Не удалось загрузить ленту"
            }, restartPagination = response.status == HttpStatusCode.BadRequest && cursor != null)
        }
        val envelope = response.body<ApiResponse<FeedPageDto>>()
        if (!envelope.isSuccess || envelope.data == null) throw ApiException(envelope.message)
        Result.success(envelope.data)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
