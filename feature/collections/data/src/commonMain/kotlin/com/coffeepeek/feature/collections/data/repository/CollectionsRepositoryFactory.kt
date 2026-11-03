package com.coffeepeek.feature.collections.data.repository

import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder

/** Plain client must not attach account credentials to public collection/media requests. */
fun createCollectionsRepository(client: HttpClient, publicClient: HttpClient, apiBaseUrl: String,
    account: CollectionsAccount, pinRequest: (HttpRequestBuilder, String) -> Unit = { _, _ -> }): CollectionsRepository =
    HttpCollectionsRepository(client, publicClient, apiBaseUrl, account, pinRequest)
