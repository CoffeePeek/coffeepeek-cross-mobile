package com.coffeepeek.admin.di

import com.coffeepeek.admin.utils.ByteArrayFetcher
import com.coffeepeek.admin.utils.CustomUrlFetcher
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import org.koin.dsl.module

fun imageModule() = module {
    single<KamelConfig> {
        val urlFetcher = get<CustomUrlFetcher>()
        platformImageConfig(KamelConfig {
            takeFrom(KamelConfig.Default)
            fetcher(ByteArrayFetcher)
            fetcher(urlFetcher)
        })
    }
}
