package com.coffeepeek.feature.roaster.api

/** Fills thumbnails for a roaster's shops that arrived without one; the Shop feature owns where they come from. */
fun interface RoasterShopPhotos {
    /** A usable photo URL for the shop, or null. Failures must be reported as null, never thrown. */
    suspend fun firstPhoto(shopSlug: String): String?

    companion object {
        val None = RoasterShopPhotos { null }
    }
}
