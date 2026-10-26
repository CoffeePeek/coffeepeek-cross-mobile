package com.coffeepeek.core.media

import com.coffeepeek.core.media.image.ImageBitmapMemoryCache
import com.coffeepeek.core.media.image.IosImageBitmapDecoder
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom

internal actual fun platformImageConfig(base: KamelConfig): KamelConfig {
    val config = KamelConfig {
        takeFrom(base)
        // Register last: both URL fetchers must use the same bounded bitmap decoder.
        decoder(IosImageBitmapDecoder())
    }
    return object : KamelConfig by config {
        override val imageBitmapCache = ImageBitmapMemoryCache()
    }
}
