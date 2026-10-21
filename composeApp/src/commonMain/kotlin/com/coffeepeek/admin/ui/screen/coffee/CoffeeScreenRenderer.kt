package com.coffeepeek.admin.ui.screen.coffee

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.feature.coffee.ui.CoffeeDetailScreen
import com.coffeepeek.admin.feature.coffee.ui.CoffeeListScreen

/** Platform selection only; shared/iOS keep the legacy catalogue UI, Android composes the migrated entries. */
interface CoffeeScreenRenderer {
    @Composable
    fun List()

    @Composable
    fun Detail(slug: String)
}

internal object LegacyCoffeeScreenRenderer : CoffeeScreenRenderer {
    @Composable
    override fun List() = CoffeeListScreen()

    @Composable
    override fun Detail(slug: String) = CoffeeDetailScreen(slug)
}
