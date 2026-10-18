package com.coffeepeek.admin.utils

import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.ResourceConfig
import io.kamel.core.fetcher.Fetcher
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.reflect.KClass
import com.coffeepeek.admin.utils.image.isProtectedCheckInPhoto
import com.coffeepeek.admin.utils.image.protectedPhotoRequests
import com.coffeepeek.admin.utils.image.protectedPhotoLoads
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.withPermit

actual class CustomUrlFetcher(
    private val client: HttpClient,
) : Fetcher<CustomUrl> {
    override val inputDataKClass: KClass<CustomUrl> = CustomUrl::class
    override val source: DataSource = DataSource.Network
    override val CustomUrl.isSupported: Boolean get() = true

    override fun fetch(
        data: CustomUrl,
        resourceConfig: ResourceConfig,
    ): Flow<Resource<ByteReadChannel>> = flow {
        if (data.permissionScope != null && "/variants/" in data.url) {
            val bytes = protectedPhotoLoads.read(data) { protectedPhotoRequests.withPermit {
                val response = client.get(data.url) { headers.append(HttpHeaders.CacheControl, "no-cache, no-store") }
                check(response.status.value == 200) { "Фото недоступно" }
                response.body<ByteArray>()
            } }
            emit(Resource.Success(ByteReadChannel(bytes), source))
            return@flow
        }
        protectedPhotoRequests.withPermit { client.prepareGet(data.url) {
            if (isProtectedCheckInPhoto(data.url)) headers.append(HttpHeaders.CacheControl, "no-cache, no-store")
        }.execute { response ->
            check(response.status.value == 200) { "Фото недоступно" }
            // Kamel decodes inside its sequential map collector. Keep emit synchronous:
            // buffering the raw channel would outlive execute and close the response early.
            emit(Resource.Success(response.bodyAsChannel(), source))
        } }
    }
}
