@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.coffeepeek.admin.di.shop

import com.coffeepeek.domain.model.Session
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.shop.domain.model.ShopCoffeeDetails
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopFavoriteIntentTest {
    private class Favorites : FavoritesRepository {
        val saved = mutableListOf<String>()
        val removed = mutableListOf<String>()
        override fun observe() = flowOf(Result.success(emptyList<FavoriteShop>()))
        override suspend fun read() = Result.success(emptyList<FavoriteShop>())
        override suspend fun save(shop: FavoriteShop): Result<Unit> { saved += shop.id; return Result.success(Unit) }
        override suspend fun remove(shopId: String): Result<Unit> { removed += shopId; return Result.success(Unit) }
        override suspend fun clear() = Result.success(Unit)
    }

    private val details = object : ShopDetailsRepository {
        override suspend fun getDetails(shopId: String) = Result.success(ShopDetails(
            ShopOverview(shopId, "Coffee", null, null, null, null, null, 0, true, emptyList()),
            menu = null, schedules = emptyList(), coffee = ShopCoffeeDetails(), contact = null,
            features = emptyList(), reviews = emptyList(), userCheckIns = emptyList()))
    }

    @Test fun guestFavoriteIsAppliedOnceWhenTheNextSessionSignsIn() = runTest {
        val sessions = CheckInTestSessions(null)
        val favorites = Favorites()
        val applied = mutableListOf<Pair<String, Boolean>>()
        val intent = ShopFavoriteIntent(sessions, details, favorites, backgroundScope) { id, favorite -> applied += id to favorite }
        runCurrent()
        intent.remember("shop-1", true)
        runCurrent()
        assertEquals(emptyList(), favorites.saved)
        sessions.applySession(Session("token", userId = "owner"))
        runCurrent()
        assertEquals(listOf("shop-1"), favorites.saved)
        assertEquals(listOf("shop-1" to true), applied)
        sessions.applySession(Session("renewed", userId = "owner"))
        runCurrent()
        assertEquals(listOf("shop-1"), favorites.saved)
    }

    @Test fun nothingIsAppliedWithoutAnIntent() = runTest {
        val sessions = CheckInTestSessions(null)
        val favorites = Favorites()
        ShopFavoriteIntent(sessions, details, favorites, backgroundScope)
        runCurrent()
        sessions.applySession(Session("token", userId = "owner"))
        runCurrent()
        assertEquals(emptyList(), favorites.saved + favorites.removed)
    }
}
