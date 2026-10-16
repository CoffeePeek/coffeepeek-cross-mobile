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
        val bytes = client.get(data.url).body<ByteArray>()
        val resized = BitmapUtil.prepareForDisplay(bytes)
        send(Resource.Success(ByteReadChannel(resized), source))
    }


}
