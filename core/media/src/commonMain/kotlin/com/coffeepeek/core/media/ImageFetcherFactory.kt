package com.coffeepeek.core.media

import com.coffeepeek.core.media.ImageUrlFetcher
import io.ktor.client.HttpClient

expect fun createImageUrlFetcher(httpClient: HttpClient, protectedHttpClient: HttpClient): ImageUrlFetcher
