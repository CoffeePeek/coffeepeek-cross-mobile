package com.coffeepeek.api.model.response

import kotlinx.serialization.Serializable

@Serializable
data class AppleLoginChallengeResp(val challengeId: String, val nonce: String)
