package com.coffeepeek.feature.discovery.domain.model

/** Where a shop is, in degrees. */
data class Coordinates(val latitude: Double, val longitude: Double) {
    val isValid: Boolean get() = latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0
}

enum class ShopKind {
    Specialty, CoffeeBar, Cafe;

    companion object {
        /** The server sends a name or a number; anything else is the default kind, as the legacy catalogue did. */
        fun fromApi(raw: String?): ShopKind = when (raw?.trim()?.lowercase()) {
            "1", "specialty" -> Specialty
            "3", "cafe" -> Cafe
            else -> CoffeeBar
        }
    }

    val apiName: String get() = when (this) { Specialty -> "Specialty"; CoffeeBar -> "CoffeeBar"; Cafe -> "Cafe" }
}

/** Legacy read model has four levels; current search groups Expensive and Luxury into its highest band. */
enum class PriceLevel(val level: Int) {
    Cheap(1), Moderate(2), Expensive(3), Luxury(4);

    companion object {
        fun fromApi(raw: String?): PriceLevel? = when (raw?.trim()?.lowercase()) {
            "1", "cheap", "$" -> Cheap
            "2", "moderate", "$$" -> Moderate
            "3", "expensive", "$$$" -> Expensive
            "4", "luxury", "$$$$" -> Luxury
            else -> null
        }

        fun ofLevel(level: Int?): PriceLevel? = entries.firstOrNull { it.level == level }
    }
}

/** A shop as the search lists it and the cards show it. [id] is the public slug. */
data class ShopSummary(
    val id: String,
    val title: String,
    val rating: Double? = null,
    val checkInCount: Int = 0,
    val address: String? = null,
    val priceLevel: PriceLevel? = null,
    val photoUrl: String? = null,
    val isOpen: Boolean = false,
    val isNew: Boolean = false,
    val isVisited: Boolean = false,
    val isFavorite: Boolean = false,
    /** At most three: the shop's tags, else its brew methods and beans. */
    val tags: List<String> = emptyList(),
    val brewMethods: List<String> = emptyList(),
    val roasterLogos: List<String> = emptyList(),
    val kind: ShopKind = ShopKind.CoffeeBar,
    val coordinates: Coordinates? = null,
    /** From the server on the nearby search; the list otherwise measures it from the user's position. */
    val distanceMeters: Double? = null,
)

data class Paged<T>(val items: List<T>, val totalCount: Int, val totalPages: Int, val currentPage: Int) {
    val hasMore: Boolean get() = currentPage < totalPages
}
