package com.coffeepeek.admin.di

import io.kamel.core.config.KamelConfig

/** Platform decoding and memory policy for the application's shared image loader. */
internal expect fun platformImageConfig(base: KamelConfig): KamelConfig
