package com.coffeepeek.core.media

import io.kamel.core.config.KamelConfig
import com.coffeepeek.core.media.image.ImageBitmapMemoryCache

internal actual fun platformImageConfig(base: KamelConfig): KamelConfig = object : KamelConfig by base {
    override val imageBitmapCache = ImageBitmapMemoryCache()
}
