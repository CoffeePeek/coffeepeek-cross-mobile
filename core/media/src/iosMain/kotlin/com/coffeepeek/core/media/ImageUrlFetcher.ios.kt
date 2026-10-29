package com.coffeepeek.core.media

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
import com.coffeepeek.core.media.image.isProtectedCheckInPhoto
import com.coffeepeek.core.media.image.isImmutablePhotoVariant
import com.coffeepeek.core.media.image.protectedPhotoRequests
import com.coffeepeek.core.media.image.protectedVariantBytes
import com.coffeepeek.core.media.image.grantOnlinePhoto
import com.coffeepeek.core.media.image.requirePhotoAccess
import com.coffeepeek.core.media.image.SystemPhotoCacheClock
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.withPermit

actual class ImageUrlFetcher(
    private val client: HttpClient,
    private val protectedClient: HttpClient = client,
) : Fetcher<ImageUrl> {
    actual override val inputDataKClass: KClass<ImageUrl> = ImageUrl::class
    actual override val source: DataSource = DataSource.Network
    actual override val ImageUrl.isSupported: Boolean get() = true

    actual override fun fetch(
        data: ImageUrl,
        resourceConfig: ResourceConfig,
    ): Flow<Resource<ByteReadChannel>> = flow {
        if (data.permissionScope != null && isImmutablePhotoVariant(data.url)) {
            val bytes = protectedVariantBytes(protectedClient, data)
            emit(Resource.Success(ByteReadChannel(bytes), source))
            return@flow
        }
        protectedPhotoRequests.withPermit {
            val start = SystemPhotoCacheClock.monotonicMillis()
            val selectedClient = if (isProtectedCheckInPhoto(data.url)) protectedClient else client
            selectedClient.prepareGet(data.url) {
            if (isProtectedCheckInPhoto(data.url)) headers.append(HttpHeaders.CacheControl, "no-cache, no-store")
        }.execute { response ->
            if (isProtectedCheckInPhoto(data.url)) requirePhotoAccess(response.status.value)
            check(response.status.value == 200) { "Фото недоступно" }
            if (isProtectedCheckInPhoto(data.url)) grantOnlinePhoto(data, start)
            // Kamel decodes inside its sequential map collector. Keep emit synchronous:
            // buffering the raw channel would outlive execute and close the response early.
            emit(Resource.Success(response.bodyAsChannel(), source))
        } }
    }
}
