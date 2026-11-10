package com.coffeepeek.admin.ui.screen.coffee

import androidx.compose.runtime.Composable

/** Platform selection only; both platforms bind feature entries. */
interface CoffeeScreenRenderer {
    @Composable
    fun Detail(slug: String)
}
