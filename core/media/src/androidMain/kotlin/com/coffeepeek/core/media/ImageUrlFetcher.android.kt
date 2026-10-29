package com.coffeepeek.core.media

import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.ResourceConfig
import io.kamel.core.fetcher.Fetcher
import io.kamel.image.getDataSourceEnding
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlin.reflect.KClass
import com.coffeepeek.core.media.image.isProtectedCheckInPhoto
import com.coffeepeek.core.media.image.isImmutablePhotoVariant
import com.coffeepeek.core.media.image.protectedPhotoRequests
import com.coffeepeek.core.media.image.retryOnPhotoTimeout
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

    actual override val ImageUrl.isSupported: Boolean
        get() = true

    actual override fun fetch(
        data: ImageUrl, resourceConfig: ResourceConfig
    ): Flow<Resource<ByteReadChannel>> = channelFlow {
        if (data.permissionScope != null && isImmutablePhotoVariant(data.url)) {
            send(Resource.Success(ByteReadChannel(protectedVariantBytes(protectedClient, data)), source))
            return@channelFlow
        }
        suspend fun download(): ByteArray = protectedPhotoRequests.withPermit {
            val start = SystemPhotoCacheClock.monotonicMillis()
            val selectedClient = if (isProtectedCheckInPhoto(data.url)) protectedClient else client
            val response = selectedClient.get(data.url) {
                if (isProtectedCheckInPhoto(data.url)) headers.append(HttpHeaders.CacheControl, "no-cache, no-store")
            }
            if (isProtectedCheckInPhoto(data.url)) requirePhotoAccess(response.status.value)
            check(response.status.value == 200) { "Фото недоступно" }
            val bytes = response.body<ByteArray>()
            if (isProtectedCheckInPhoto(data.url)) grantOnlinePhoto(data, start)
            // Vector/animated assets and bounded server variants must reach their own decoders unchanged.
            val display = if (getDataSourceEnding(data) in setOf("svg", "xml", "gif") ||
                isProtectedCheckInPhoto(data.url) && "/variants/" in data.url) bytes
                else AndroidImageCodec.prepareForDisplay(bytes)
            display
        }
        val bytes = retryOnPhotoTimeout(::download)
        send(Resource.Success(ByteReadChannel(bytes), source))
    }


}
