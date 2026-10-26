package com.coffeepeek.feature.leaderboard.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.leaderboard.api.LeaderboardEntry
import com.coffeepeek.feature.leaderboard.api.LeaderboardImage
import com.coffeepeek.feature.leaderboard.domain.repository.LeaderboardRepository
import com.coffeepeek.feature.leaderboard.impl.ui.LeaderboardViewModel
import com.coffeepeek.feature.leaderboard.impl.ui.compose.LeaderboardScreen
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

/** Each context emission invalidates personalized data; false suspends reads until privacy context is ready. */
fun createLeaderboardEntry(repository: LeaderboardRepository, contextChanges: Flow<Boolean>, image: LeaderboardImage): LeaderboardEntry =
    LeaderboardApiImpl(repository, contextChanges, image)

private class LeaderboardApiImpl(
    private val repository: LeaderboardRepository,
    private val contextChanges: Flow<Boolean>,
    private val image: LeaderboardImage,
) : LeaderboardEntry {
    private val instanceKey = Uuid.random().toString()

    @Composable
    override fun Content(refreshKey: Long, onOpenProfile: (String, String, String?) -> Unit,
        overlay: @Composable (content: @Composable () -> Unit) -> Unit, modifier: Modifier) {
        val vm = viewModel(key = "leaderboard:$instanceKey") { LeaderboardViewModel(repository, contextChanges) }
        LeaderboardScreen(vm, refreshKey, image, onOpenProfile, overlay, modifier)
    }
}
