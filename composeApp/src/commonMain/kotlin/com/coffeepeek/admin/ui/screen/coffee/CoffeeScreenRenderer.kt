package com.coffeepeek.admin.ui.screen.coffee

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Platform selection only; both platforms bind feature entries, while legacy UI remains a fallback. */
interface CoffeeScreenRenderer {
    @Composable
    fun List(scrollToTopRequests: Flow<Unit> = emptyFlow())

    @Composable
    fun Detail(slug: String)
}
