package com.coffeepeek.data.repository

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
        val session = sessionRepository.peekSession()
        if (sessionRepository.isActiveSession(session) && session?.userId == cachedForUserId) {
            _profile.value?.let { return Result.success(it) }
        }
        return refreshProfile()
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
        userApiService.updateUsername(username).map { updated ->
            _profile.update { profile -> profile?.copy(userName = updated.username, address = updated.address?.toDomain()) }
            publicAvatarCache.clear()
            Unit
        }

    override suspend fun updateAbout(about: String): Result<Unit> =
        userApiService.updateAbout(about).onSuccess {
            _profile.update { profile -> profile?.copy(about = about) }
        }

    override suspend fun updateAvatar(photo: PendingPhotoUpload): Result<Unit> = runCatching {
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
        if (!sessionRepository.isActiveSession(session)) {
            clearProfile()
            return Result.failure(IllegalStateException("Сессия завершена"))
        }
        return userApiService.getMe().fold(
            onSuccess = { dto ->
                val current = sessionRepository.peekSession()
                if (current?.userId != session?.userId || !sessionRepository.isActiveSession(current)) {
                    Result.failure(IllegalStateException("Сессия изменилась"))
                } else {
                    val profile = dto.toUserProfile()
                    cachedForUserId = session?.userId
                    _profile.value = profile
                    Result.success(profile)
                }
            },
            onFailure = { Result.failure(it) },
        )
    }

    private fun clearProfile() {
        cachedForUserId = null
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
)

private fun AccountDeletionRequestDto.toDomain() = AccountDeletionRequest(
    requestId = requestId,
    status = status,
    expiresAtUtc = expiresAtUtc,
    resendAvailableAtUtc = resendAvailableAtUtc,
)
