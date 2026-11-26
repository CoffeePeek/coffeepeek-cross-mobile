package com.coffeepeek.admin.di.profile

import com.coffeepeek.feature.community.api.CommunitySavedPosts
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.FailureKind
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileCheckInLike
import com.coffeepeek.feature.profile.api.ProfileCheckInLikes
import com.coffeepeek.feature.profile.domain.PublicProfileAuthenticationRequired
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class AppProfileCheckInLikes(
    private val repository: CommunityRepository,
    private val accounts: ProfileAccountPort,
    private val savedPosts: CommunitySavedPosts,
) : ProfileCheckInLikes {
    override suspend fun setHelpful(id: String, helpful: Boolean): Result<ProfileCheckInLike> {
        val account = accounts.activeAccount() ?: return Result.failure(PublicProfileAuthenticationRequired())
        val scope = accounts.cacheScope()
        val result = repository.setHelpful(id, helpful)
        currentCoroutineContext().ensureActive()
        if (account != accounts.activeAccount() || scope != accounts.cacheScope()) throw CancellationException("Session changed")
        return result.fold(
            onSuccess = {
                savedPosts.updated(account)
                Result.success(ProfileCheckInLike(it.isHelpful, it.helpfulCount))
            },
            onFailure = {
                if (it is CancellationException) throw it
                val failure = if (it is CommunityFailure && it.kind == FailureKind.SignInRequired) PublicProfileAuthenticationRequired() else it
                Result.failure(failure)
            },
        )
    }
}
