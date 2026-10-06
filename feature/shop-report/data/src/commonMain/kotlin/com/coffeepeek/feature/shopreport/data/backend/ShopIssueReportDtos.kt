@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.shopreport.data.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
internal enum class BackendShopIssueCategory {
    OutdatedMenu,
    ShopClosed,
    IncorrectAddress,
    WrongOpeningHours,
    IncorrectPhotos,
    Other,
}

@Serializable
internal data class CreateShopIssueReportRequest(
    @SerialName("shopId") val shopId: String,
    @SerialName("category") val category: BackendShopIssueCategory,
    @SerialName("description") val description: String? = null,
)

@Serializable
internal data class ShopIssueReportResponse(
    @SerialName("isSuccess") @JsonNames("IsSuccess") val isSuccess: Boolean,
    @SerialName("message") @JsonNames("Message") val message: String = "",
    @SerialName("entityId") @JsonNames("EntityId") val entityId: String? = null,
)
