package com.coffeepeek.feature.ownership.data.backend

import com.coffeepeek.feature.ownership.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*

internal class OwnershipBackend(private val client: HttpClient) {
    suspend fun claims(page: Int): OwnershipPageDto<ClaimDto> = client.get("/api/owner/ownership-claims") {
        privateRead()
        parameter("page", page)
        parameter("pageSize", 20)
    }.data()
    suspend fun claim(id: String): ClaimDto = client.get("/api/owner/ownership-claims/${segment(id)}") { privateRead() }.data()
    suspend fun submit(input: ClaimSubmission): ClaimDto = client.post("/api/owner/ownership-claims") {
        contentType(ContentType.Application.Json)
        setBody(ClaimRequest(input.kind.name, input.slug, input.contact, input.evidence))
    }.data()
    suspend fun shops(): OwnerShopsDto = client.get(path(BusinessKind.CoffeeShop)) { privateRead() }.data()
    suspend fun roasters(page: Int): OwnershipPageDto<OwnerSummaryDto> = client.get(path(BusinessKind.Roaster)) {
        privateRead()
        parameter("page", page)
        parameter("pageSize", 20)
    }.data()
    suspend fun profile(kind: BusinessKind, id: String): ProfileDto = client.get("${path(kind)}/${segment(id)}") { privateRead() }.data()
    suspend fun write(profile: BusinessProfile, photos: List<BusinessPhoto>?) {
        client.put("${path(profile.kind)}/${segment(profile.id)}") {
            header(HttpHeaders.IfMatch, "\"${profile.revision}\"")
            contentType(ContentType.Application.Json)
            if (profile.kind == BusinessKind.CoffeeShop) setBody(profile.shopBody()) else setBody(profile.roasterBody(photos))
        }.data<kotlinx.serialization.json.JsonObject>()
    }
    suspend fun addPhotos(id: String, photos: List<BusinessPhoto>): ProfileDto = client.post("${path(BusinessKind.CoffeeShop)}/${segment(id)}/photos") {
        contentType(ContentType.Application.Json)
        setBody(PhotosRequest(photos.map { it.metadata() }))
    }.data()
    suspend fun removePhotos(id: String, ids: List<String>): ProfileDto = client.delete("${path(BusinessKind.CoffeeShop)}/${segment(id)}/photos") {
        contentType(ContentType.Application.Json)
        setBody(PhotoIdsRequest(ids))
    }.data()
    suspend fun orderPhotos(id: String, ids: List<String>): ProfileDto = client.put("${path(BusinessKind.CoffeeShop)}/${segment(id)}/photos/order") {
        contentType(ContentType.Application.Json)
        setBody(PhotoIdsRequest(ids))
    }.data()
}
private fun HttpRequestBuilder.privateRead() { header(HttpHeaders.CacheControl, "no-store") }
private fun segment(value: String): String = value.encodeURLPathPart()
private fun path(kind: BusinessKind) = "/api/owner/${if (kind == BusinessKind.CoffeeShop) "coffee-shops" else "roasters"}"
private suspend inline fun <reified T> HttpResponse.data(): T {
    if (!status.isSuccess()) throw problem(status.value)
    val envelope = body<OwnershipEnvelope<T>>()
    if (!envelope.isSuccess || envelope.data == null) throw OwnershipFailure(OwnershipProblem.Unavailable)
    return envelope.data
}
internal fun problem(status: Int): OwnershipFailure = OwnershipFailure(when (status) {
    401, 403 -> OwnershipProblem.SignedOut
    404 -> OwnershipProblem.NotFound
    409, 428 -> OwnershipProblem.Conflict
    400, 422 -> OwnershipProblem.Validation
    else -> OwnershipProblem.Unavailable
})
