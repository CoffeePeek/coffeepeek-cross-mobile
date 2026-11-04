package com.coffeepeek.feature.shelf.domain.model

enum class ShelfBagStatus { OnShelf, Finished }

enum class ShelfGrind { Beans, Ground }

/** What the beans were roasted for. */
enum class ShelfRoastPurpose { Espresso, Filter }

/** A protected card image; [url] is absolute and needs the owner's session to load. */
data class ShelfBagPhoto(val id: String, val url: String)

/**
 * One coffee bag of the signed-in user. [position] is the zero-based shelf index and is null in the archive.
 * [name] is the catalog snapshot or the custom name; dates are ISO strings (`roastedOn` a calendar date, the rest UTC instants).
 */
data class ShelfBag(
    val id: String,
    val status: ShelfBagStatus,
    val position: Int?,
    val photo: ShelfBagPhoto?,
    val coffeeProductId: String?,
    val coffeeProductSlug: String?,
    val name: String?,
    val roasterName: String?,
    val weightGrams: Int?,
    val grind: ShelfGrind?,
    val roastPurpose: ShelfRoastPurpose? = null,
    val roastedOn: String?,
    val addedAtUtc: String,
    val finishedAtUtc: String?,
    val rating: Int?,
    val buyAgain: Boolean?,
)
