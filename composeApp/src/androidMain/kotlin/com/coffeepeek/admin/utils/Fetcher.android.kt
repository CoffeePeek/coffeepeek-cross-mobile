package com.coffeepeek.admin.utils

import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.ResourceConfig
import io.kamel.core.fetcher.Fetcher
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlin.reflect.KClass
import com.coffeepeek.admin.utils.image.isProtectedCheckInPhoto
import com.coffeepeek.admin.utils.image.isImmutablePhotoVariant
import com.coffeepeek.admin.utils.image.protectedPhotoRequests
import com.coffeepeek.admin.utils.image.protectedVariantBytes
import com.coffeepeek.admin.utils.image.grantOnlinePhoto
import com.coffeepeek.admin.utils.image.SystemPhotoCacheClock
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.withPermit

actual class CustomUrlFetcher(
    private val client: HttpClient,
    private val protectedClient: HttpClient = client,
) : Fetcher<CustomUrl> {

    override val inputDataKClass: KClass<CustomUrl> = CustomUrl::class

    override val source: DataSource = DataSource.Network

    override val CustomUrl.isSupported: Boolean
        get() = true

    override fun fetch(
        data: CustomUrl, resourceConfig: ResourceConfig
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
            check(response.status.value == 200) { "Фото недоступно" }
            val bytes = response.body<ByteArray>()
            if (isProtectedCheckInPhoto(data.url)) grantOnlinePhoto(data, start)
            // Server variants are already oriented and bounded; decode only once in Kamel.
            val display = if (isProtectedCheckInPhoto(data.url) && "/variants/" in data.url) bytes
                else BitmapUtil.prepareForDisplay(bytes)
            display
        }
        val bytes = download()
        send(Resource.Success(ByteReadChannel(bytes), source))
    }


}
