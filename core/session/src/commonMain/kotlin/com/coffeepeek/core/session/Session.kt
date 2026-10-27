package com.coffeepeek.core.session

data class Session(
    val accessToken: String,
    val refreshToken: String? = null,
    val userId: String? = null,
)
