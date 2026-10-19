package com.coffeepeek.feature.coffee.impl

import com.coffeepeek.feature.coffee.api.CoffeeAnalytics
import com.coffeepeek.feature.coffee.api.CoffeeScreenAnalytics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.coffee.api.CoffeeListEntry
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import com.coffeepeek.feature.coffee.impl.ui.CoffeeListViewModel
import com.coffeepeek.feature.coffee.impl.ui.compose.CoffeeListScreen
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeListEvent
import java.util.UUID

/** Construct once in composition. Image loader owns failure fallback; no network client is created here. */
fun createCoffeeListEntry(repository: CoffeeRepository,
    analytics: CoffeeAnalytics = CoffeeAnalytics.None,
    screenAnalytics: CoffeeScreenAnalytics = CoffeeScreenAnalytics.None,
    imageContent: @Composable (url: String, description: String, modifier: Modifier, scale: ContentScale) -> Unit,
): CoffeeListEntry = CoffeeListApiImpl(repository, imageContent, analytics, screenAnalytics)

private class CoffeeListApiImpl(private val repository: CoffeeRepository,
    private val imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit,
    private val analytics: CoffeeAnalytics, private val screenAnalytics: CoffeeScreenAnalytics) : CoffeeListEntry {
    private val instanceKey = UUID.randomUUID().toString()

    @Composable
    override fun Content(onOpenCoffee: (String) -> Unit, modifier: Modifier, bottomClearance: Dp) {
        key(this) {
            val model = viewModel(key = "coffee-list:$instanceKey") { CoffeeListViewModel(repository, analytics.list()) }
            CoffeeListScreen(model, imageContent, bottomClearance, modifier, screenAnalytics) { event ->
                when (event) { is CoffeeListEvent.OpenCoffee -> onOpenCoffee(event.slug) }
            }
        }
    }
}
