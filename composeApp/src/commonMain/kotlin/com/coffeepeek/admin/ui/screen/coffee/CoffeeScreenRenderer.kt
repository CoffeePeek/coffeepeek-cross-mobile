package com.coffeepeek.admin.ui.screen.coffee

import androidx.compose.runtime.Composable

/** Platform selection only; both platforms bind feature entries, while legacy UI remains a fallback. */
interface CoffeeScreenRenderer {
    @Composable
    fun List()

    @Composable
    fun Detail(slug: String)
}
