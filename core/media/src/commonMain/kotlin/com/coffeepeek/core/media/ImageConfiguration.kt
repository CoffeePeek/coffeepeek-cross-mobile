package com.coffeepeek.core.media

import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default

/** One platform decoder/cache policy; the root supplies the already configured URL fetcher. */
fun createImageConfig(fetcher: ImageUrlFetcher): KamelConfig = platformImageConfig(KamelConfig {
    takeFrom(KamelConfig.Default)
    fetcher(ByteArrayFetcher)
    fetcher(fetcher)
})
