package com.coffeepeek.api.model.request

import kotlinx.serialization.Serializable

@Serializable
data class AppleLoginReq(val challengeId: String, val idToken: String)
