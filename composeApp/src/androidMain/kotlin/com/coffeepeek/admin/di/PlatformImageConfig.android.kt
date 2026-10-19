package com.coffeepeek.admin.di

import io.kamel.core.config.KamelConfig
import com.coffeepeek.admin.utils.image.ImageBitmapMemoryCache

internal actual fun platformImageConfig(base: KamelConfig): KamelConfig = object : KamelConfig by base {
    override val imageBitmapCache = ImageBitmapMemoryCache()
}
