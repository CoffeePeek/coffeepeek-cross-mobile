package com.coffeepeek.data.repository

import com.coffeepeek.api.utils.requestResult
import com.coffeepeek.api.model.request.UploadedPhotoReq
import com.coffeepeek.api.model.response.AccountDeletionRequestDto
import com.coffeepeek.api.model.response.UserProfileDto
import com.coffeepeek.api.service.UserApiService
import com.coffeepeek.data.mapper.toDomain
import com.coffeepeek.domain.model.AccountDeletionRequest
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.UserProfile
import com.coffeepeek.domain.repository.PhotoRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserRepositoryImpl(
    private val userApiService: UserApiService,
    private val photoRepository: PhotoRepository,
    private val sessionRepository: SessionRepository,
    private val scope: CoroutineScope,
) : UserRepository {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    override fun observeProfile(): StateFlow<UserProfile?> = _profile.asStateFlow()

    private var cachedForUserId: String? = null
    private var cachedForGeneration: Long? = null
    private val publicAvatarCache = mutableMapOf<String, String?>()

    init {
        scope.launch {
            sessionRepository.observeSession()
                .map { session ->
                    when {
                        !sessionRepository.isActiveSession(session) -> null
                        else -> session?.userId
                    }
                }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId == null) {
                        clearProfile()
                    } else if (userId != cachedForUserId) {
                        clearProfile()
                        cachedForUserId = userId
                    }
                }
        }
    }

    override suspend fun refreshProfile(): Result<UserProfile> =
        fetchAndCacheProfile()

    override suspend fun getMe(): Result<UserProfile> {
        cachedProfile()?.let { return Result.success(it) }
        return refreshProfile()
    }

    override fun cachedProfile(): UserProfile? {
        val session = sessionRepository.peekSession()
        return _profile.value.takeIf { sessionRepository.isActiveSession(session) && session?.userId != null &&
            session.userId == cachedForUserId && sessionRepository.accountGeneration() == cachedForGeneration }
    }

    override suspend fun getPublicAvatarUrl(userId: String): Result<String?> {
        if (userId.isBlank()) return Result.success(null)
        if (publicAvatarCache.containsKey(userId)) {
            return Result.success(publicAvatarCache[userId])
        }
        return userApiService.getUser(userId).map { profile ->
            profile.avatarUrl.also { publicAvatarCache[userId] = it }
        }
    }

    override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> =
        userApiService.requestAccountDeletion().map { it.toDomain() }

    override suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?> =
        userApiService.getAccountDeletionRequest().map { it?.toDomain() }

    override suspend fun updateUsername(username: String): Result<Unit> =
        forOwnerOfTheRequest({ userApiService.updateUsername(username) }) { updated ->
            _profile.update { profile -> profile?.copy(userName = updated.username, address = updated.address?.toDomain()) }
            publicAvatarCache.clear()
        }

    override suspend fun updateAbout(about: String): Result<Unit> =
        forOwnerOfTheRequest({ userApiService.updateAbout(about) }) {
            _profile.update { profile -> profile?.copy(about = about) }
        }

    /**
     * Runs a write for the account that is signed in now and applies its answer to the cached profile only if that account is still the signed-in one:
     * a late answer must not change the profile that was loaded for another account. The write itself already happened on the server for its own account.
     */
    private suspend fun <T> forOwnerOfTheRequest(request: suspend () -> Result<T>, apply: (T) -> Unit): Result<Unit> {
        val owner = sessionRepository.peekSession()
        val generation = sessionRepository.accountGeneration()
        return request().fold(
            onSuccess = { value ->
                val current = sessionRepository.peekSession()
                if (!sessionRepository.isActiveSession(current) || current?.userId != owner?.userId ||
                    sessionRepository.accountGeneration() != generation) {
                    Result.failure(IllegalStateException("Сессия изменилась"))
                } else {
                    apply(value)
                    Result.success(Unit)
                }
            },
            onFailure = { Result.failure(it) },
        )
    }

    override suspend fun updateAvatar(photo: PendingPhotoUpload): Result<Unit> = requestResult {
        val meta = photoRepository.uploadAvatar(photo)
            .getOrElse { error("Не удалось загрузить фото: ${it.message ?: "неизвестная ошибка"}") }
        userApiService.updateAvatar(
            UploadedPhotoReq(
                fileName = meta.fileName,
                contentType = meta.contentType,
                storageKey = meta.storageKey,
                size = meta.size,
            ),
        ).getOrElse { error("Не удалось сохранить аватар: ${it.message ?: "неизвестная ошибка"}") }
        fetchAndCacheProfile().getOrThrow()
        Unit
    }

    private suspend fun fetchAndCacheProfile(): Result<UserProfile> {
        val session = sessionRepository.peekSession()
        val generation = sessionRepository.accountGeneration()
        if (!sessionRepository.isActiveSession(session)) {
            clearProfile()
            return Result.failure(IllegalStateException("Сессия завершена"))
        }
        return userApiService.getMe().fold(
            onSuccess = { cacheProfile(it, session?.userId, generation) },
            onFailure = { Result.failure(it) })
    }

    private fun cacheProfile(dto: UserProfileDto, owner: String?, generation: Long): Result<UserProfile> {
        val current = sessionRepository.peekSession()
        if (current?.userId != owner || !sessionRepository.isActiveSession(current) ||
            sessionRepository.accountGeneration() != generation) {
            return Result.failure(IllegalStateException("Сессия изменилась"))
        }
        val profile = dto.toUserProfile()
        cachedForUserId = owner
        cachedForGeneration = generation
        _profile.value = profile
        return Result.success(profile)
    }

    private fun clearProfile() {
        cachedForUserId = null
        cachedForGeneration = null
        _profile.value = null
    }
}

private fun UserProfileDto.toUserProfile() = UserProfile(
    address = address?.toDomain(),
    userName = userName,
    email = email,
    about = about,
    avatarUrl = avatarUrl,
    reviewCount = reviewCount,
    checkInCount = checkInCount,
    addedShopsCount = addedShopsCount,
    addedRoastersCount = addedRoastersCount,
    followersCount = followersCount,
)

private fun AccountDeletionRequestDto.toDomain() = AccountDeletionRequest(
    requestId = requestId,
    status = status,
    expiresAtUtc = expiresAtUtc,
    resendAvailableAtUtc = resendAvailableAtUtc,
)
