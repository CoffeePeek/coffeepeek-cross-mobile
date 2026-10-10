package com.coffeepeek.feature.shopreport.domain.model

/** Business reasons accepted by the shop issue reporting workflow. */
enum class ShopIssueCategory {
    OutdatedMenu,
    ShopClosed,
    IncorrectAddress,
    WrongOpeningHours,
    IncorrectPhotos,
    Other,
}
