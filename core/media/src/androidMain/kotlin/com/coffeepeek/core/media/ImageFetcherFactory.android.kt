package com.coffeepeek.core.media

import com.coffeepeek.core.media.ImageUrlFetcher
import io.ktor.client.HttpClient

actual fun createImageUrlFetcher(httpClient: HttpClient, protectedHttpClient: HttpClient): ImageUrlFetcher =
    ImageUrlFetcher(client = httpClient, protectedClient = protectedHttpClient)
