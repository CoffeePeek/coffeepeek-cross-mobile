package com.coffeepeek.feature.coffee.impl.ui.compose.model

internal sealed interface CoffeeDetailEvent {
    data object Back : CoffeeDetailEvent
    data class OpenRoaster(val slug: String) : CoffeeDetailEvent
    data class OpenCoffee(val slug: String) : CoffeeDetailEvent
    data class OpenSeller(val url: String) : CoffeeDetailEvent
    data class Share(val name: String, val canonicalPath: String) : CoffeeDetailEvent
}
