package com.coffeepeek.feature.roaster.domain.model

data class RoasterDetails(
    val id: String,
    val address: RoasterAddress,
    val name: String,
    val about: String?,
    val location: RoasterLocation?,
    val contact: RoasterContact?,
    val photos: List<RoasterPhoto>,
    val shops: List<RoasterShop>,
)

data class RoasterLocation(val address: String, val latitude: Double?, val longitude: Double?)

data class RoasterContact(val instagramLink: String?, val siteLink: String?)

data class RoasterPhoto(val id: String, val url: String, val sortIndex: Int)

data class RoasterShop(val slug: String, val name: String, val photoUrl: String?, val address: RoasterAddress)
