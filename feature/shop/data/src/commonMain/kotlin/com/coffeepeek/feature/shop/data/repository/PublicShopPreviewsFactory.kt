package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import com.coffeepeek.feature.shop.data.backend.ShopDetailsBackend
import com.coffeepeek.feature.shop.data.mapper.toPublicPreview
import io.ktor.client.HttpClient

/** Uses the existing client/details endpoint; never fetches menu catalogs or personal enrichment. */
fun createPublicShopPreviews(client: HttpClient, utcOffsetMinutes: () -> Int): PublicShopPreviews {
    val backend = ShopDetailsBackend(client)
    return PublicShopPreviews { slug -> requestResult { backend.load(slug).getOrThrow().toPublicPreview(utcOffsetMinutes()) } }
}
