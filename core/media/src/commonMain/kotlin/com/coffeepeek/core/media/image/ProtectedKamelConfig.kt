package com.coffeepeek.core.media.image

import io.kamel.core.cache.Cache
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom

/** Explicit overrides also bypass a platform's custom decoded cache implementation. */
internal fun protectedKamelConfig(base: KamelConfig): KamelConfig {
    val config = KamelConfig { takeFrom(base) }
    return object : KamelConfig by config {
        override val imageBitmapCache = base.imageBitmapCache.disabled()
        override val imageVectorCache = base.imageVectorCache.disabled()
        override val svgCache = base.svgCache.disabled()
        override val animatedImageCache = base.animatedImageCache.disabled()
    }
}

private fun <K, V> Cache<K, V>.disabled(): Cache<K, V> = object : Cache<K, V> {
    override val maxSize = 0
    override val size = 0
    override fun get(key: K): V? = null
    override fun set(key: K, value: V) = Unit
    override fun remove(key: K) = false
    override fun clear() = Unit
}
