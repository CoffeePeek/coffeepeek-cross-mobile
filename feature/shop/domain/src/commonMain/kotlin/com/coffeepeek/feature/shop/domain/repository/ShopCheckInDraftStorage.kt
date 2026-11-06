package com.coffeepeek.feature.shop.domain.repository

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import kotlinx.coroutines.flow.Flow

data class SavedShopCheckInDraft(
    val shopId: String,
    val input: ShopCheckInCreateInput,
    val deliveryUnconfirmed: Boolean = false,
)

interface ShopCheckInDraftStorage {
    suspend fun read(accountId: String, shopId: String): SavedShopCheckInDraft?
    suspend fun save(accountId: String, draft: SavedShopCheckInDraft)
    suspend fun delete(accountId: String, shopId: String)
    fun observeLatestShop(accountId: String): Flow<String?>
}
