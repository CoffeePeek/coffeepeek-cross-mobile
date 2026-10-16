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
        client.prepareGet(data.url).execute { response ->
            // Kamel decodes inside its sequential map collector. Keep emit synchronous:
            // buffering the raw channel would outlive execute and close the response early.
            emit(Resource.Success(response.bodyAsChannel(), source))
        }
    }
}
