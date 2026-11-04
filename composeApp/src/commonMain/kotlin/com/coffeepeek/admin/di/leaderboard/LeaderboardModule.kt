package com.coffeepeek.admin.di.leaderboard

import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.leaderboard.api.LeaderboardEntry
import com.coffeepeek.feature.leaderboard.data.repository.createLeaderboardRepository
import com.coffeepeek.feature.leaderboard.domain.repository.LeaderboardRepository
import com.coffeepeek.feature.leaderboard.impl.createLeaderboardEntry
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.dsl.module

internal fun leaderboardModule() = module {
    single<LeaderboardRepository> { createLeaderboardRepository(get<CoffeePeekClient>().client) }
    single<LeaderboardEntry> {
        createLeaderboardEntry(get(), leaderboardContextChanges(get(), get())) { url, modifier ->
            CpImage(data = url, modifier = modifier)
        }
    }
}

internal fun leaderboardContextChanges(sessions: SessionReader, authors: AuthorVisibility) = combine(
    sessions.observeSession().map { session ->
        session?.takeIf(sessions::isActiveSession)?.let { it.userId ?: it.accessToken }
    }.distinctUntilChanged(),
    authors.visibility,
) { _, snapshot -> snapshot.ready }
