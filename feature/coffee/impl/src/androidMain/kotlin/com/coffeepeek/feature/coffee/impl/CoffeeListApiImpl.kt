package com.coffeepeek.feature.coffee.impl

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
    imageContent: @Composable (url: String, description: String, modifier: Modifier, scale: ContentScale) -> Unit,
): CoffeeListEntry = CoffeeListApiImpl(repository, imageContent)

private class CoffeeListApiImpl(private val repository: CoffeeRepository,
    private val imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit) : CoffeeListEntry {
    private val instanceKey = UUID.randomUUID().toString()

    @Composable
    override fun Content(onOpenCoffee: (String) -> Unit, modifier: Modifier, bottomClearance: Dp) {
        key(this) {
            val model = viewModel(key = "coffee-list:$instanceKey") { CoffeeListViewModel(repository) }
            CoffeeListScreen(model, imageContent, bottomClearance, modifier) { event ->
                when (event) { is CoffeeListEvent.OpenCoffee -> onOpenCoffee(event.slug) }
            }
        }
    }
}
