package com.coffeepeek.admin.di.favorites

import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import org.junit.Test
import org.koin.core.context.GlobalContext
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/** Resolve the real Android bindings without opening UI, changing a session or sending a mutation. */
class ServerFavoritesWiringTest {
    @Test fun androidUsesTheServerAdaptersAndDoesNotReplaceTheServerWriterWithRoom() {
        val graph = GlobalContext.get()
        assertIs<ServerFavoriteShopsRepository>(graph.get<FavoritesRepository>())
    }
}
