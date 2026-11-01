package com.coffeepeek.admin.utils

import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.native_photo_unavailable
import org.jetbrains.compose.resources.getString

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
import com.coffeepeek.admin.utils.image.isImmutablePhotoVariant
import com.coffeepeek.admin.utils.image.protectedPhotoRequests
import com.coffeepeek.admin.utils.image.protectedVariantBytes
import com.coffeepeek.admin.utils.image.grantOnlinePhoto
import com.coffeepeek.admin.utils.image.requirePhotoAccess
import com.coffeepeek.admin.utils.image.SystemPhotoCacheClock
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.withPermit

actual class CustomUrlFetcher(
    private val client: HttpClient,
    private val protectedClient: HttpClient = client,
) : Fetcher<CustomUrl> {
    override val inputDataKClass: KClass<CustomUrl> = CustomUrl::class
    override val source: DataSource = DataSource.Network
    override val CustomUrl.isSupported: Boolean get() = true

    override fun fetch(
        data: CustomUrl,
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
            if (response.status.value != 200) error(getString(Res.string.native_photo_unavailable))
            if (isProtectedCheckInPhoto(data.url)) grantOnlinePhoto(data, start)
            // Kamel decodes inside its sequential map collector. Keep emit synchronous:
            // buffering the raw channel would outlive execute and close the response early.
            emit(Resource.Success(response.bodyAsChannel(), source))
        } }
    }
}
