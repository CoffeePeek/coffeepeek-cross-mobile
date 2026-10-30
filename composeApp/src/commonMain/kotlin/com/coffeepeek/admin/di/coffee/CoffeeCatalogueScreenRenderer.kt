package com.coffeepeek.admin.di.coffee

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.platformReduceMotion
import com.coffeepeek.admin.ui.screen.coffee.CoffeeScreenRenderer
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.feature.coffee.api.CoffeeDetailEntry
import com.coffeepeek.feature.coffee.api.CoffeeListEntry
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import com.coffeepeek.feature.coffee.impl.createCoffeeDetailEntry
import com.coffeepeek.admin.ui.component.LoadErrorContent
import com.coffeepeek.feature.coffee.impl.createCoffeeListEntry
import kotlinx.coroutines.flow.Flow

/**
 * Routes to the migrated catalogue on both platforms; root navigation, browser, share and photo viewer stay app-owned.
 * With [shelf] (builds where the shelf is enabled) the tab switches between the shelf and the catalogue.
 */
internal class CoffeeCatalogueScreenRenderer(
    private val list: CoffeeListEntry,
    private val detail: CoffeeDetailEntry,
    private val shelf: CoffeeShelfTab? = null,
) : CoffeeScreenRenderer {
    @Composable
    override fun List(scrollToTopRequests: Flow<Unit>) {
        if (shelf == null) Catalogue(scrollToTopRequests) {} else shelf.Content { header -> Catalogue(scrollToTopRequests, header) }
    }

    @Composable
    private fun Catalogue(scrollToTopRequests: Flow<Unit>, header: @Composable () -> Unit) {
        list.Content(onOpenCoffee = { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) },
            modifier = Modifier, bottomClearance = LocalFloatingNavClearance.current,
            scrollToTopRequests = scrollToTopRequests, animateScrollToTop = !platformReduceMotion(), header = header)
    }

    @Composable
    override fun Detail(slug: String) {
        detail.Content(slug, onBack = Navigator::popBack,
            onOpenCoffee = { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) },
            onOpenRoaster = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
            onOpenSeller = OpenInBrowser::openInBrowser,
            onShare = { name, path -> ShareHelper.shareText("$name — https://coffeepeek.by$path") }, modifier = Modifier)
    }
}

internal fun coffeeCatalogueScreenRenderer(repository: CoffeeRepository, shelf: CoffeeShelfTab? = null): CoffeeCatalogueScreenRenderer {
    val image: @Composable (String, String, Modifier, androidx.compose.ui.layout.ContentScale) -> Unit =
        { url, description, modifier, scale ->
            CoffeeShopImage(imageUrl = url, modifier = modifier, contentDescription = description, contentScale = scale)
        }
    return CoffeeCatalogueScreenRenderer(
        createCoffeeListEntry(repository, CoffeeCatalogueAnalytics, CoffeeCatalogueScreenAnalytics, image) { onRetry -> LoadErrorContent(onRetry) },
        createCoffeeDetailEntry(repository, image,
            { urls, index, onDismiss -> FullScreenImageDialog(urls, index, onDismiss = onDismiss) },
            ::utcIsoToLocalDateTime, CoffeeCatalogueAnalytics, CoffeeCatalogueScreenAnalytics),
        shelf,
    )
}
