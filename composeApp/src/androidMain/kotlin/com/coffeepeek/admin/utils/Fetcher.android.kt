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
import com.coffeepeek.admin.utils.image.protectedPhotoRequests
import com.coffeepeek.admin.utils.image.protectedPhotoLoads
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.withPermit

actual class CustomUrlFetcher(
    private val client: HttpClient,
) : Fetcher<CustomUrl> {

    override val inputDataKClass: KClass<CustomUrl> = CustomUrl::class

    override val source: DataSource = DataSource.Network

    override val CustomUrl.isSupported: Boolean
        get() = true

    override fun fetch(
        data: CustomUrl, resourceConfig: ResourceConfig
    ): Flow<Resource<ByteReadChannel>> = channelFlow {
        suspend fun download(): ByteArray = protectedPhotoRequests.withPermit {
            val response = client.get(data.url) {
                if (isProtectedCheckInPhoto(data.url)) headers.append(HttpHeaders.CacheControl, "no-cache, no-store")
            }
            check(response.status.value == 200) { "Фото недоступно" }
            val bytes = response.body<ByteArray>()
            // Server variants are already oriented and bounded; decode only once in Kamel.
            val display = if (isProtectedCheckInPhoto(data.url) && "/variants/" in data.url) bytes
                else BitmapUtil.prepareForDisplay(bytes)
            display
        }
        val bytes = if (data.permissionScope != null) protectedPhotoLoads.read(data, ::download) else download()
        send(Resource.Success(ByteReadChannel(bytes), source))
    }


}
