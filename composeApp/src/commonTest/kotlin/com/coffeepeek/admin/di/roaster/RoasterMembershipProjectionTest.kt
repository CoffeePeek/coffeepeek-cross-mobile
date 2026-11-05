package com.coffeepeek.admin.di.roaster

import com.coffeepeek.admin.di.collections.FavoriteCollectionsPrompt
import com.coffeepeek.admin.di.favorites.CatalogItem
import com.coffeepeek.admin.di.favorites.RoasterFavorites
import com.coffeepeek.feature.favorites.domain.model.*
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoasterMembershipProjectionTest {
    @Test fun confirmedRoasterKeysDoNotWaitForCatalogueOrFavoriteMetadata() = runTest {
        val shop = FavoriteTarget(FavoriteKind.CoffeeShop, "same")
        val roaster = FavoriteTarget(FavoriteKind.Roaster, "same")
        val server = Server(setOf(shop, roaster))
        val adapter = RoasterFavoritesAdapter(catalogue, backgroundScope, prompt, server)
        assertEquals(setOf("same"), adapter.observeKeys().first())
        server.memberships.value = Result.success(setOf(shop))
        assertTrue(adapter.observeKeys().first().isEmpty())
    }

    private val catalogue = object : RoasterFavorites {
        override fun observeFavorites() = flow<List<CatalogItem>> { error("Catalogue metadata must not block heart updates") }
        override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean) = error("No write")
    }
    private val prompt = object : FavoriteCollectionsPrompt {
        override fun shop(slug: String) = Unit
        override fun roaster(slug: String) = Unit
    }

    private class Server(initial: Set<FavoriteTarget>) : AccountFavorites {
        val memberships = MutableStateFlow(Result.success(initial))
        override fun observeMemberships() = memberships
        override fun observeFavorites() = flow<Result<List<FavoriteEntry>>> { error("No metadata read") }
        override suspend fun getFavorites(refresh: Boolean) = error("No network read")
        override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean) = error("No write")
        override fun clearCache() = Unit
    }
}
