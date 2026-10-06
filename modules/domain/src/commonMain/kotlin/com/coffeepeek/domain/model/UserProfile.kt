package com.coffeepeek.domain.model

data class UserProfile(
    val userName: String,
    val email: String,
    val about: String?,
    val avatarUrl: String?,
    val reviewCount: Int,
    val checkInCount: Int,
    val addedShopsCount: Int,
    val address: PublicAddress? = null,
)
