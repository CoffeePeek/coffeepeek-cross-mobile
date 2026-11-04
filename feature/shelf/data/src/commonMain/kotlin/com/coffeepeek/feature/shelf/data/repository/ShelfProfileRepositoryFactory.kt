package com.coffeepeek.feature.shelf.data.repository

import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder

/** [publicClient] has no account credentials; the application pins owner requests before sending. */
fun createShelfProfileRepository(
    client: HttpClient,
    publicClient: HttpClient,
    apiBaseUrl: String,
    account: ShelfAccount,
    pinRequest: (HttpRequestBuilder, String) -> Unit,
): ShelfProfileRepository {
    require(client !== publicClient)
    return HttpShelfProfileRepository(client, publicClient, apiBaseUrl, account, pinRequest)
}
