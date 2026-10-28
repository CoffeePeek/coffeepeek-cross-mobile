package com.coffeepeek.feature.shelf.impl.ui

import com.coffeepeek.feature.shelf.api.ShelfBrewLog
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagBrews
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.data.forBag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The open bag card's check-ins; opening another bag or closing the card drops the earlier load. */
internal class ShelfBrewLoader(private val log: ShelfBrewLog) {
    private var job: Job? = null

    fun load(scope: CoroutineScope, item: ShelfBagItem, onLoaded: (ShelfBagBrews) -> Unit) {
        cancel()
        job = scope.launch { onLoaded(log.forBag(item)) }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
