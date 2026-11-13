package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunitySavedPosts
import com.coffeepeek.feature.community.api.SavedCommunityPost
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationInput
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationVisibility
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationModeration
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class HomeCommunityWrites(private val source: HomePreparationRepository,
    private val account: CommunityAccount, private val saved: CommunitySavedPosts) : HomePreparationRepository by source {
    override suspend fun create(input: HomePreparationInput) = write { source.create(input) }
    override suspend fun update(id: String, input: HomePreparationInput) = write { source.update(id, input) }

    private suspend fun write(operation: suspend () -> Result<HomePreparation>): Result<HomePreparation> {
        val key = account.currentAccountKey()
        val result = operation()
        currentCoroutineContext().ensureActive()
        if (key != null && key == account.currentAccountKey()) result.onSuccess { reconcile(key, it) }
        return result
    }

    private fun reconcile(key: String, entry: HomePreparation) {
        val visible = entry.visibility == HomePreparationVisibility.Public &&
            entry.moderation in setOf(HomePreparationModeration.Pending, HomePreparationModeration.Approved)
        if (visible) saved.saved(key, entry.id)
        else saved.acknowledge(listOf(SavedCommunityPost(key, entry.id)))
    }

    override suspend fun delete(id: String): Result<Unit> {
        val key = account.currentAccountKey()
        val result = source.delete(id)
        currentCoroutineContext().ensureActive()
        if (key != null && key == account.currentAccountKey() && result.isSuccess)
            saved.acknowledge(listOf(SavedCommunityPost(key, id)))
        return result
    }
}
