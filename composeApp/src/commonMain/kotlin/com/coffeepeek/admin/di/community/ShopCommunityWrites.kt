package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunitySavedPosts
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class ShopCommunityWrites(private val source: ShopCheckInRepository,
    private val account: CommunityAccount, private val saved: CommunitySavedPosts) : ShopCheckInRepository by source {
    override suspend fun create(input: ShopCheckInCreateInput): Result<String> {
        val key = account.currentAccountKey()
        val result = source.create(input)
        currentCoroutineContext().ensureActive()
        if (key != null && key == account.currentAccountKey() && input.visibility == ShopCheckInVisibility.Public)
            result.onSuccess { saved.saved(key, it) }
        return result
    }
}
