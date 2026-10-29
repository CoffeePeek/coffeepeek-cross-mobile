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
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Construct once in composition. Image loader owns failure fallback; no network client is created here. */
fun createCoffeeListEntry(repository: CoffeeRepository,
    analytics: CoffeeAnalytics = CoffeeAnalytics.None,
    screenAnalytics: CoffeeScreenAnalytics = CoffeeScreenAnalytics.None,
    imageContent: @Composable (url: String, description: String, modifier: Modifier, scale: ContentScale) -> Unit,
    /** Optional app-owned error presentation (brand mascot); the built-in text error is used when null. */
    loadError: (@Composable (onRetry: () -> Unit) -> Unit)? = null,
): CoffeeListEntry = CoffeeListApiImpl(repository, imageContent, analytics, screenAnalytics, loadError)

private class CoffeeListApiImpl(private val repository: CoffeeRepository,
    private val imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit,
    private val analytics: CoffeeAnalytics, private val screenAnalytics: CoffeeScreenAnalytics,
    private val loadError: (@Composable (onRetry: () -> Unit) -> Unit)?) : CoffeeListEntry {
    @OptIn(ExperimentalUuidApi::class)
    private val instanceKey = Uuid.random().toString()

    @Composable
    override fun Content(
        onOpenCoffee: (String) -> Unit, modifier: Modifier, bottomClearance: Dp,
        scrollToTopRequests: Flow<Unit>, animateScrollToTop: Boolean, header: @Composable () -> Unit,
    ) {
        key(this) {
            val model = viewModel(key = "coffee-list:$instanceKey") { CoffeeListViewModel(repository, analytics.list()) }
            CoffeeListScreen(model, imageContent, bottomClearance, modifier, screenAnalytics, loadError,
                scrollToTopRequests, animateScrollToTop, header) { event ->
                when (event) { is CoffeeListEvent.OpenCoffee -> onOpenCoffee(event.slug) }
            }
        }
    }
}
