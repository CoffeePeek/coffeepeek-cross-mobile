package com.coffeepeek.feature.shopreport.data.mapper

import com.coffeepeek.feature.shopreport.data.backend.BackendShopIssueCategory
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory

internal fun ShopIssueCategory.toBackend(): BackendShopIssueCategory = when (this) {
    ShopIssueCategory.OutdatedMenu -> BackendShopIssueCategory.OutdatedMenu
    ShopIssueCategory.ShopClosed -> BackendShopIssueCategory.ShopClosed
    ShopIssueCategory.IncorrectAddress -> BackendShopIssueCategory.IncorrectAddress
    ShopIssueCategory.WrongOpeningHours -> BackendShopIssueCategory.WrongOpeningHours
    ShopIssueCategory.IncorrectPhotos -> BackendShopIssueCategory.IncorrectPhotos
    ShopIssueCategory.Other -> BackendShopIssueCategory.Other
}
