package com.coffeepeek.feature.discovery.domain.rules

import com.coffeepeek.feature.discovery.domain.model.CatalogEntry

private val venueTypeNames = setOf("specialty", "кофейня", "кафе", "cafe", "coffee shop", "coffee-shop", "coffeeshop")
private val venueTypeSlugs = setOf("specialty", "cafe", "coffee-shop", "coffeeshop", "kofeynya", "coffeehouse", "coffee-house")

/** A tag that names the kind of the place (Specialty, Кофейня, Кафе) rather than something the place has: the kind has its own filter. */
fun CatalogEntry.isVenueType(): Boolean = name.trim().lowercase() in venueTypeNames || id.trim().lowercase() in venueTypeSlugs

/** The tags the filter sheet offers: what a shop has, without the kinds of places. */
fun amenityTags(tags: List<CatalogEntry>): List<CatalogEntry> = tags.filterNot { it.isVenueType() }
