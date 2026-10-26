package com.coffeepeek.core.media

import io.kamel.core.DataSource
import io.kamel.core.Resource
import io.kamel.core.config.ResourceConfig
import io.kamel.core.fetcher.Fetcher
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.reflect.KClass

val ByteArrayFetcher = object : Fetcher<ByteArray> {

    override val inputDataKClass: KClass<ByteArray> = ByteArray::class

    override val source: DataSource = DataSource.Disk

    override val ByteArray.isSupported: Boolean
        get() = true

    override fun fetch(
        data: ByteArray,
        resourceConfig: ResourceConfig
    ): Flow<Resource<ByteReadChannel>> = flow {
        emit(Resource.Success(ByteReadChannel(data), source))
    }
}


data class ImageUrl(
    val url: String,
    val permissionScope: Any? = null,
    val displayPermission: com.coffeepeek.core.media.image.PhotoDisplayPermission? = null,
) {
    // Kamel selects SVG/XML decoders from the resource's string URL.
    override fun toString(): String = url
}

expect class ImageUrlFetcher : Fetcher<ImageUrl>
