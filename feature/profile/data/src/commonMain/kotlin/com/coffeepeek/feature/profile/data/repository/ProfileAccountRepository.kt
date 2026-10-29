package com.coffeepeek.feature.profile.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.profile.api.AccountDeletionPort
import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.api.ProfileDeletionRequest
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.data.backend.OwnProfileBackend
import com.coffeepeek.feature.profile.data.backend.ProfileAvatarUploadBackend
import com.coffeepeek.feature.profile.data.backend.usernameFailure
import com.coffeepeek.feature.profile.api.UsernameChangeFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProfileAccountRepository(
    private val backend: OwnProfileBackend,
    private val uploads: ProfileAvatarUploadBackend,
    private val sessions: SessionReader,
    scope: CoroutineScope,
) : ProfileAccountPort, ProfileEditPort, AccountDeletionPort {
    private data class Cache(val owner: String?, val generation: Long = 0, val profile: OwnProfile? = null)
    private val cache = MutableStateFlow(Cache(activeAccount()))

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            sessions.observeSession().map(::key).distinctUntilChanged().collect { synchronize(it) }
        }
    }

    override fun activeAccount() = key(sessions.peekSession())
    override fun observeActiveAccount() = sessions.observeSession().map(::key).distinctUntilChanged()
    override fun cachedOwnProfile() = synchronize(activeAccount()).profile
    override fun observeOwnProfile() = combine(cache, sessions.observeSession()) { stored, session ->
        stored.profile.takeIf { stored.owner == key(session) }
    }.distinctUntilChanged()

    override suspend fun loadOwnProfile(forceRefresh: Boolean) = requestResult {
        val lease = begin()
        if (!forceRefresh) lease.profile?.let { return@requestResult it }
        load(lease)
    }

    override suspend fun updateUsername(username: String): Result<Unit> {
        val result = requestResult {
            val lease = begin()
            val updated = backend.username(username)
            commit(lease) { it?.copy(userName = updated.username, address = updated.address?.toProfile()) }
        }
        return result.fold({ Result.success(Unit) }, {
            if (it is CancellationException) throw it
            Result.failure(it as? UsernameChangeFailure ?: usernameFailure(0, it.message))
        })
    }

    override suspend fun updateAbout(about: String) = requestResult {
        val lease = begin()
        backend.about(about)
        commit(lease) { it?.copy(about = about) }
    }

    override suspend fun updateAvatar(image: ProfileAvatarImage) = requestResult {
        val lease = begin()
        val photo = uploads.upload(image) { requireCurrent(lease) }
        backend.avatar(photo)
        requireCurrent(lease)
        load(lease)
        Unit
    }

    override suspend fun existingRequest() = requestResult {
        val lease = begin()
        backend.existingDeletion().also { requireCurrent(lease) }?.let { ProfileDeletionRequest(it.resendAvailableAtUtc) }
    }

    override suspend fun request() = requestResult {
        val lease = begin()
        val result = backend.requestDeletion()
        requireCurrent(lease)
        ProfileDeletionRequest(result.resendAvailableAtUtc)
    }

    private suspend fun load(lease: Cache): OwnProfile {
        requireCurrent(lease)
        val profile = backend.load().toProfile()
        currentCoroutineContext().ensureActive()
        requireCurrent(lease)
        // A read started before a successful edit must not roll that edit back.
        cache.compareAndSet(lease, lease.copy(profile = profile))
        requireCurrent(lease)
        return cache.value.profile ?: profile
    }

    private suspend fun commit(lease: Cache, transform: (OwnProfile?) -> OwnProfile?) {
        currentCoroutineContext().ensureActive()
        requireCurrent(lease)
        cache.update { current ->
            check(current.owner == lease.owner && current.generation == lease.generation) { "Сессия изменилась" }
            current.copy(profile = transform(current.profile))
        }
    }

    private fun begin(): Cache = synchronize(activeAccount()).also { check(it.owner != null) { "Сессия завершена" } }

    private fun requireCurrent(lease: Cache) {
        val current = synchronize(activeAccount())
        check(current.owner == lease.owner && current.generation == lease.generation) { "Сессия изменилась" }
    }

    private fun synchronize(owner: String?): Cache {
        cache.update { if (it.owner == owner) it else Cache(owner, it.generation + 1) }
        return cache.value
    }

    private fun key(session: Session?) = session?.takeIf(sessions::isActiveSession)?.let { it.userId ?: it.accessToken }
}
