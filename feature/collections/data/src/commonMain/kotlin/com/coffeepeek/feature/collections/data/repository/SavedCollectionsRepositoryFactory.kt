package com.coffeepeek.feature.collections.data.repository

import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder

/** Public client must not attach credentials, including to presigned object-storage uploads. */
fun createSavedCollectionsRepository(client: HttpClient, publicClient: HttpClient, apiBaseUrl: String,
    account: CollectionsAccount, pinRequest: (HttpRequestBuilder, String) -> Unit = { _, _ -> }): SavedCollectionsRepository =
    HttpSavedCollectionsRepository(client, publicClient, apiBaseUrl, account, pinRequest)
