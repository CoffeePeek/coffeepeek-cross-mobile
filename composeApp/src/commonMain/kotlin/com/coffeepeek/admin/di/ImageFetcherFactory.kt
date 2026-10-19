package com.coffeepeek.admin.di

import com.coffeepeek.admin.utils.CustomUrlFetcher
import io.ktor.client.HttpClient

expect fun createImageUrlFetcher(httpClient: HttpClient, protectedHttpClient: HttpClient): CustomUrlFetcher
