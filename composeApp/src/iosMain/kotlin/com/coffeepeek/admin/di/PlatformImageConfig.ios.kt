package com.coffeepeek.admin.di

import com.coffeepeek.admin.utils.image.ImageBitmapMemoryCache
import com.coffeepeek.admin.utils.image.IosImageBitmapDecoder
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
