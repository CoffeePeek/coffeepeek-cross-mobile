package com.coffeepeek.feature.shelf.impl

import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay

class ShelfNativeItem internal constructor(item: ShelfBagItem, today: ShelfDay) {
    val id = item.id
    val title = item.bag.name
    val roaster = item.bag.roasterName
    val grams = item.bag.weightGrams
    val grind = item.bag.grind?.name
    val purpose = item.bag.roastPurpose?.name
    val roastedOn = item.roastedOn?.iso
    val addedOn = item.addedOn?.iso
    val finishedOn = item.finishedOn?.iso
    val days = item.daysOnShelf(today)
    val roastDays = item.daysSinceRoast(today)
    val rating = item.bag.rating
    val buyAgain = item.bag.buyAgain
    val hasPhoto = item.bag.photo != null
    val hasCoffee = !item.bag.coffeeProductSlug.isNullOrBlank()
}
