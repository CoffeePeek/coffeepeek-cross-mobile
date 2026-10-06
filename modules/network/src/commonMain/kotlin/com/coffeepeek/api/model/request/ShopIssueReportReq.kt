package com.coffeepeek.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ShopIssueCategory {
    OutdatedMenu,
    ShopClosed,
    IncorrectAddress,
    WrongOpeningHours,
    IncorrectPhotos,
    Other,
}

@Serializable
data class CreateShopIssueReportReq(
    @SerialName("shop") val shopId: String,
    @SerialName("category") val category: ShopIssueCategory,
    @SerialName("description") val description: String? = null,
)
