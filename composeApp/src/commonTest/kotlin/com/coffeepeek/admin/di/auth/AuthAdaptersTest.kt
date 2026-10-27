package com.coffeepeek.admin.di.auth

import com.coffeepeek.data.session.UserSessionCleaner
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthAdaptersTest {
    @Test fun clearingLocalUserDataSignsTheSessionOutClearsTheOwnersFavoritesAndTheCaches() = runBlocking<Unit> {
        val log = mutableListOf<String>()
        val sessions = object : SessionRepository {
            private val state = MutableStateFlow<Session?>(Session("token", "refresh", "owner"))
            override fun peekSession() = state.value
            override fun applySession(session: Session?) { state.value = session }
            override fun isActiveSession(session: Session?) = session != null
            override suspend fun getSession() = state.value
            override suspend fun persistSession(session: Session?) = applySession(session)
            override suspend fun saveSession(session: Session?) { log += "save:${session?.userId}"; applySession(session) }
            override suspend fun warmCache() = Unit
            override fun observeSession() = state
            override suspend fun isLoggedIn() = state.value != null
        }
        val favorites = object : FavoriteRepository {
            override suspend fun getFavoriteIds(): Set<String> = error("Unused")
            override suspend fun isFavorite(shopId: String): Boolean = error("Unused")
            override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> = error("Unused")
            override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> = error("Unused")
            override suspend fun removeFavorite(shopId: String): Result<Unit> = error("Unused")
            override suspend fun clearAll(): Unit = error("Unused")
            override suspend fun clearForUser(userId: String?) { log += "favorites:$userId" }
        }
        val local = UserSessionLocalUserData(UserSessionCleaner(sessions, favorites, "", "", clearCaches = { log += "caches" }))
        local.clear()
        assertEquals(listOf("save:null", "favorites:owner", "caches"), log)
        assertNull(sessions.peekSession())
    }
}
