package com.coffeepeek.feature.coffee.impl.ui.compose.model

internal sealed interface CoffeeDetailAction {
    data object Initialize : CoffeeDetailAction
    data object Retry : CoffeeDetailAction
    data object RetrySimilar : CoffeeDetailAction
    data object Back : CoffeeDetailAction
    data object OpenPhotos : CoffeeDetailAction
    data object ClosePhotos : CoffeeDetailAction
    data object ToggleDescription : CoffeeDetailAction
    data object OpenRoaster : CoffeeDetailAction
    data class OpenSimilar(val slug: String) : CoffeeDetailAction
    data class OpenSeller(val offerIndex: Int) : CoffeeDetailAction
    data object Share : CoffeeDetailAction
}
