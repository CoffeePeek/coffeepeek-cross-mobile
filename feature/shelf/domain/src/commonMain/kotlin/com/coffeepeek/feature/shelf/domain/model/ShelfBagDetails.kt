package com.coffeepeek.feature.shelf.domain.model

/** Which coffee a bag holds; the same selector rules as a home check-in's bean. */
sealed interface ShelfBean {
    data object Unknown : ShelfBean
    /** A currently published catalog product. */
    data class Catalog(val slug: String) : ShelfBean
    /** Keeps the bag's existing product and its name snapshot, even after a catalog rename or archive. */
    data class Kept(val coffeeProductId: String) : ShelfBean
    data class Custom(val name: String) : ShelfBean
}

/** Editable details. Saving replaces them all: a null field is cleared. */
data class ShelfBagDetails(
    val bean: ShelfBean = ShelfBean.Unknown,
    val roasterName: String? = null,
    val weightGrams: Int? = null,
    val grind: ShelfGrind? = null,
    val roastPurpose: ShelfRoastPurpose? = null,
    /** ISO calendar date. */
    val roastedOn: String? = null,
)

/** Already processed card bytes to upload; the original photo never leaves the device. */
class ShelfBagCard(val bytes: ByteArray, val contentType: String, val fileName: String)
