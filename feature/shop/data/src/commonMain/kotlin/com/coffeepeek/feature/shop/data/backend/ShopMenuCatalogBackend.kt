package com.coffeepeek.feature.shop.data.backend

import com.coffeepeek.core.network.requestResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Public catalog, cached for this repository lifetime only after a successful read. */
internal class ShopMenuCatalogBackend(private val client: HttpClient) {
    private val mutex = Mutex()
    private var cached: List<ShopMenuDrinkDto>? = null

    suspend fun load(): Result<List<ShopMenuDrinkDto>> = requestResult {
        mutex.withLock {
            cached ?: run {
                val response = client.get("/api/menu/drinks")
                val body = response.body<ShopMenuCatalogResponse>()
                val data = body.data
                check(response.status.isSuccess() && body.isSuccess && data != null) {
                    "Menu catalog request was rejected"
                }
                data.drinks.sortedBy { it.sortOrder }.also { cached = it }
            }
        }
    }
}

@Serializable
private data class ShopMenuCatalogResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("data") val data: ShopMenuCatalogDto? = null,
)

@Serializable
private data class ShopMenuCatalogDto(@SerialName("drinks") val drinks: List<ShopMenuDrinkDto>)

@Serializable
internal data class ShopMenuDrinkDto(
    @SerialName("slug") val slug: String,
    @SerialName("nameRu") val nameRu: String = "",
    @SerialName("nameEn") val nameEn: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("sortOrder") val sortOrder: Int = 0,
)
