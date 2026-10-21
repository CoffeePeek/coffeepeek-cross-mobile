package com.coffeepeek.feature.coffee.impl

import com.coffeepeek.feature.coffee.api.CoffeeAnalytics
import com.coffeepeek.feature.coffee.api.CoffeeScreenAnalytics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.coffee.api.CoffeeDetailEntry
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import com.coffeepeek.feature.coffee.impl.ui.CoffeeDetailViewModel
import com.coffeepeek.feature.coffee.impl.ui.compose.CoffeeDetailScreen
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeDetailEvent
import java.util.UUID

/** Caller owns gallery/platform ports and destroys the destination's ViewModelStore on removal. */
fun createCoffeeDetailEntry(repository: CoffeeRepository,
    imageContent: @Composable (url: String, description: String, modifier: Modifier, scale: ContentScale) -> Unit,
    galleryContent: @Composable (urls: List<String>, initialIndex: Int, onDismiss: () -> Unit) -> Unit,
    formatCheckedAt: (utc: String) -> String,
    analytics: CoffeeAnalytics = CoffeeAnalytics.None,
    screenAnalytics: CoffeeScreenAnalytics = CoffeeScreenAnalytics.None,
): CoffeeDetailEntry = CoffeeDetailApiImpl(repository, imageContent, galleryContent, formatCheckedAt, analytics, screenAnalytics)

private class CoffeeDetailApiImpl(private val repository: CoffeeRepository,
    private val imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit,
    private val galleryContent: @Composable (List<String>, Int, () -> Unit) -> Unit,
    private val formatCheckedAt: (String) -> String,
    private val analytics: CoffeeAnalytics,
    private val screenAnalytics: CoffeeScreenAnalytics,
) : CoffeeDetailEntry {
    private val instanceKey = UUID.randomUUID().toString()

    @Composable
    override fun Content(slug: String, onBack: () -> Unit, onOpenCoffee: (String) -> Unit, onOpenRoaster: (String) -> Unit,
        onOpenSeller: (String) -> Unit, onShare: (String, String) -> Unit, modifier: Modifier) {
        key(this, slug) {
            val model = viewModel(key = "coffee-detail:$instanceKey:$slug") { CoffeeDetailViewModel(slug, repository, analytics.detail()) }
            CoffeeDetailScreen(model, imageContent, galleryContent, formatCheckedAt, modifier, screenAnalytics) { event ->
                when (event) {
                    CoffeeDetailEvent.Back -> onBack()
                    is CoffeeDetailEvent.OpenCoffee -> onOpenCoffee(event.slug)
                    is CoffeeDetailEvent.OpenRoaster -> onOpenRoaster(event.slug)
                    is CoffeeDetailEvent.OpenSeller -> onOpenSeller(event.url)
                    is CoffeeDetailEvent.Share -> onShare(event.name, event.canonicalPath)
                }
            }
        }
    }
}
