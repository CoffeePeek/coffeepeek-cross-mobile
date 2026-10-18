package com.coffeepeek.feature.coffee.impl.ui.compose.model

internal sealed interface CoffeeListEvent {
    data class OpenCoffee(val slug: String) : CoffeeListEvent
}
