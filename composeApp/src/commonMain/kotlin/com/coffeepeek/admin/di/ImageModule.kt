package com.coffeepeek.admin.di

import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.media.createImageConfig
import io.kamel.core.config.KamelConfig
import org.koin.dsl.module

fun imageModule() = module {
    single<KamelConfig> { createImageConfig(get<ImageUrlFetcher>()) }
}
