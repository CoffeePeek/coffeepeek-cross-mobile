package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.api.ShelfCoffeeOption
import com.coffeepeek.feature.shelf.api.ShelfPhoto
import com.coffeepeek.feature.shelf.domain.model.ShelfGrind
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay

internal sealed interface ShelfAddAction {
    data object DownloadScanner : ShelfAddAction
    data object OpenCamera : ShelfAddAction
    /** Back from the camera: to the scan it was opened from, or out of the flow. */
    data object CloseCamera : ShelfAddAction
    /** The card has finished peeling off the photo. */
    data object RevealFinished : ShelfAddAction
    data class PhotoPicked(val photo: ShelfPhoto) : ShelfAddAction
    data object PhotoFailed : ShelfAddAction
    data class QueryChanged(val query: String) : ShelfAddAction
    data class CoffeeChosen(val option: ShelfCoffeeOption) : ShelfAddAction
    data class RoasterChanged(val roaster: String) : ShelfAddAction
    data class WeightChosen(val grams: Int?) : ShelfAddAction
    data class GrindChosen(val grind: ShelfGrind?) : ShelfAddAction
    data class RoastDateChosen(val day: ShelfDay?) : ShelfAddAction
    /** [later] is "Заполню позже": the bag goes on the shelf with its card only. */
    data class Save(val later: Boolean) : ShelfAddAction
    data object ConfirmDuplicate : ShelfAddAction
    data object CancelDuplicate : ShelfAddAction
    data object DismissMessage : ShelfAddAction
    data object Close : ShelfAddAction
}
