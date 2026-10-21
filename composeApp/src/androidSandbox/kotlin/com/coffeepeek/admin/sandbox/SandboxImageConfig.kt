package com.coffeepeek.admin.sandbox

import com.coffeepeek.admin.utils.ByteArrayFetcher
import com.coffeepeek.admin.utils.CustomUrlFetcher
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.httpUrlFetcher
import io.kamel.image.config.Default
import io.ktor.client.HttpClient

/** Retain decoders/mappers, never Default's independent real HTTP fetcher. */
internal fun sandboxImageConfig(client: HttpClient) = KamelConfig {
    val defaults = KamelConfig.Default
    imageBitmapCacheSize = 30
    defaults.decoders.forEach { decoder(it) }
    defaults.mappers.values.flatten().forEach { mapper(it) }
    fetcher(ByteArrayFetcher)
    fetcher(CustomUrlFetcher(client))
    httpUrlFetcher(client)
}
