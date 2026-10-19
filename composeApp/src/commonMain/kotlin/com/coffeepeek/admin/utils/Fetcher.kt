package com.coffeepeek.admin.utils

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


data class CustomUrl(
    val url: String,
    val permissionScope: Any? = null,
    val displayPermission: com.coffeepeek.admin.utils.image.PhotoDisplayPermission? = null,
)

expect class CustomUrlFetcher : Fetcher<CustomUrl>
