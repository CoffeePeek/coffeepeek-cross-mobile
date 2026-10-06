package com.coffeepeek.api.model.response

import com.coffeepeek.api.model.DataResponse
import com.coffeepeek.api.model.PublicAddressDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val address: PublicAddressDto? = null,
    @SerialName("userName")       val userName: String,
    @SerialName("email")          val email: String,
    @SerialName("about")          val about: String? = null,
    @SerialName("avatarUrl")      val avatarUrl: String? = null,
    @SerialName("reviewCount")    val reviewCount: Int = 0,
    @SerialName("checkInCount")   val checkInCount: Int = 0,
    @SerialName("addedShopsCount") val addedShopsCount: Int = 0,
) : DataResponse()

@Serializable
data class PublicUserProfileDto(
    @SerialName("userName") val userName: String = "",
    @SerialName("createdAtUtc") val createdAtUtc: String = "",
    @SerialName("about") val about: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("reviewCount") val reviewCount: Int = 0,
    @SerialName("checkInCount") val checkInCount: Int = 0,
    @SerialName("addedShopsCount") val addedShopsCount: Int = 0,
) : DataResponse()

@Serializable
data class PublicUserProfileResponseDto(
    val data: PublicUserProfileDto,
    val address: PublicAddressDto,
)

@Serializable
data class UsernameUpdateDto(
    val username: String,
    val address: PublicAddressDto? = null,
)
