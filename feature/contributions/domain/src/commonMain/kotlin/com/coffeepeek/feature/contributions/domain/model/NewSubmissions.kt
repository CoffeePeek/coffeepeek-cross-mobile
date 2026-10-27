package com.coffeepeek.feature.contributions.domain.model

enum class PriceRange { Cheap, Moderate, Expensive, Luxury }

data class TimeInterval(val open: String, val close: String)

/** [day] is 0 for Sunday through 6 for Saturday. */
data class DayHours(val day: Int, val closed: Boolean, val intervals: List<TimeInterval> = emptyList())

data class NewShop(
    val name: String,
    val address: String,
    val cityId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null,
    val priceRange: PriceRange? = null,
    val contacts: Contacts = Contacts(),
    val equipmentIds: List<String> = emptyList(),
    val beanIds: List<String> = emptyList(),
    val roasterIds: List<String> = emptyList(),
    val brewMethodIds: List<String> = emptyList(),
    val photos: List<PendingPhoto> = emptyList(),
    val menuPhotos: List<PendingPhoto> = emptyList(),
    /** The opening hours in the device's local time; the repository sends them in UTC. */
    val week: List<DayHours> = emptyList(),
)

data class NewRoaster(
    val name: String,
    val about: String? = null,
    val cityId: String? = null,
    val address: String? = null,
    val instagramLink: String? = null,
    val siteLink: String? = null,
    val photos: List<PendingPhoto> = emptyList(),
)

data class RoasterSubmitted(val roasterId: String, val status: String, val isAddressValidated: Boolean, val message: String)
