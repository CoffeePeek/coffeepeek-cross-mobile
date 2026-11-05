package com.coffeepeek.feature.profile.data.backend

import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.domain.ProfileAddress
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
internal data class OwnProfileDto(
    val userName: String, val email: String, val about: String? = null, val avatarUrl: String? = null,
    val checkInCount: Int = 0, val addedShopsCount: Int = 0, val addedRoastersCount: Int = 0,
    val followersCount: Int = 0, val address: OwnProfileAddressDto? = null,
) {
    fun toProfile() = OwnProfile(userName, email, about, avatarUrl, checkInCount, addedShopsCount,
        addedRoastersCount, followersCount, address?.toProfile())
}

@Serializable
internal data class OwnProfileAddressDto(val slug: String, val canonicalPath: String, val revision: Int = 0, val isAlias: Boolean = false) {
    fun toProfile() = ProfileAddress(slug, canonicalPath, revision, isAlias)
}

@Serializable internal data class UsernameRequest(val username: String)
@Serializable internal data class AboutRequest(val about: String)
@Serializable internal data class UsernameResponse(val username: String, val address: OwnProfileAddressDto? = null)

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable internal data class DeletionResponse(@JsonNames("ResendAvailableAtUtc") val resendAvailableAtUtc: String = "")

@Serializable internal data class AvatarUploadRequest(val sizeBytes: Int, val fileName: String, val contentType: String)
@Serializable internal data class AvatarUploadTarget(val uploadUrl: String, val storageKey: String)
@Serializable internal data class UploadedAvatar(val fileName: String, val contentType: String, val storageKey: String, val size: Long)
@Serializable internal data class AvatarRequest(val uploadedPhoto: UploadedAvatar)
