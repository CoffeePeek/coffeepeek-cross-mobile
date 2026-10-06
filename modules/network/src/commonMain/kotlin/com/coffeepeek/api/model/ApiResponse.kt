@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class ApiResponse<T>(
    @SerialName("isSuccess") @JsonNames("IsSuccess") val isSuccess: Boolean = false,
    @SerialName("message") @JsonNames("Message") val message: String? = null,
    @SerialName("data") @JsonNames("Data") val data: T? = null,
    @SerialName("entityId") @JsonNames("EntityId") val entityId: String? = null,
)
