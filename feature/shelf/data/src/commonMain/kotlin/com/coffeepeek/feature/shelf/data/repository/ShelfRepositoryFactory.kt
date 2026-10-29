package com.coffeepeek.feature.shelf.data.repository

import com.coffeepeek.feature.shelf.data.backend.ShelfBackend
import com.coffeepeek.feature.shelf.data.backend.ShelfCardUploadBackend
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import io.ktor.client.HttpClient
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow

/** Who owns the shelf right now. Application composition adapts its session; the shelf never reads sessions itself. */
interface ShelfAccount {
    /** The signed-in user ID, or null for a guest. */
    val owner: String?
    /** Emits the current owner first, then every change. */
    val owners: Flow<String?>
}

/**
 * [client] carries the owner's session; [uploadClient] must be a separate credential-free client for presigned uploads.
 * The caller owns both clients.
 */
fun createShelfRepository(client: HttpClient, uploadClient: HttpClient, apiBaseUrl: String, account: ShelfAccount): ShelfRepository {
    require(client !== uploadClient) { "Shelf uploads require a separate credential-free client" }
    val base = Url(apiBaseUrl)
    require(base.protocol.name in setOf("https", "http") && base.host.isNotBlank())
    return RemoteShelfRepository(ShelfBackend(client), ShelfCardUploadBackend(client, uploadClient), apiBaseUrl, account)
}
