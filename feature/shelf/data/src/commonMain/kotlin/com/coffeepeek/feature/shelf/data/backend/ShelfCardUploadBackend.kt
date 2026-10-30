package com.coffeepeek.feature.shelf.data.backend

import com.coffeepeek.core.network.requirePublicUploadUrl
import com.coffeepeek.feature.shelf.domain.model.ShelfBagCard
import com.coffeepeek.feature.shelf.domain.model.ShelfFailure
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/** Presigned card upload through Media; [uploader] is separate and credential-free. Any failure is [ShelfIssue.PhotoUpload]. */
internal class ShelfCardUploadBackend(private val gateway: HttpClient, private val uploader: HttpClient) {
    suspend fun upload(card: ShelfBagCard): UploadedCardDto = shelfRequestResult {
        if (card.bytes.isEmpty() || card.fileName.isBlank() || !card.contentType.startsWith("image/")) throw ShelfFailure(ShelfIssue.PhotoUpload)
        val target = gateway.post("/api/Photos/shelf-bag") {
            contentType(ContentType.Application.Json)
            setBody(listOf(CardUploadRequestDto(card.bytes.size, card.fileName, card.contentType)))
        }.shelfData<List<CardUploadUrlDto>>().singleOrNull()?.takeIf { it.storageKey.isNotBlank() } ?: throw ShelfFailure(ShelfIssue.InvalidResponse)
        requirePublicUploadUrl(target.uploadUrl)
        val response = uploader.put(target.uploadUrl) {
            headers {
                remove(HttpHeaders.Accept); remove(HttpHeaders.AcceptCharset)
                append(HttpHeaders.ContentType, card.contentType)
                append("x-amz-tagging", "is_permanent=False")
            }
            setBody(card.bytes)
        }
        if (!response.status.isSuccess()) throw ShelfFailure(ShelfIssue.PhotoUpload)
        UploadedCardDto(card.fileName, card.contentType, target.storageKey, card.bytes.size.toLong())
    }.getOrElse { failure ->
        val safe = failure as ShelfFailure
        // A session problem must still surface as such; everything else is an upload problem the user can retry.
        throw if (safe.issue in setOf(ShelfIssue.Unauthorized, ShelfIssue.Offline, ShelfIssue.Timeout)) safe else ShelfFailure(ShelfIssue.PhotoUpload, safe.statusCode)
    }
}
