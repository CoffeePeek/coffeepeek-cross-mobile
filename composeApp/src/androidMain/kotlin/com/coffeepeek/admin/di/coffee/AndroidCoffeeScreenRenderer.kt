package com.coffeepeek.admin.di.coffee

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
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

/** Android routes to the migrated catalogue; root navigation, browser, share and photo viewer stay app-owned. */
internal class AndroidCoffeeScreenRenderer(
    private val list: CoffeeListEntry,
    private val detail: CoffeeDetailEntry,
) : CoffeeScreenRenderer {
    @Composable
    override fun List() {
        list.Content(onOpenCoffee = { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) },
            modifier = Modifier, bottomClearance = LocalFloatingNavClearance.current)
    }

    @Composable
    override fun Detail(slug: String) {
        detail.Content(slug, onBack = Navigator::popBack,
            onOpenCoffee = { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) },
            onOpenRoaster = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
            onOpenSeller = OpenInBrowser::openInBrowser,
            onShare = { name, path -> ShareHelper.shareText("$name — https://coffeepeek.by$path") })
    }
}

internal fun androidCoffeeScreenRenderer(repository: CoffeeRepository): AndroidCoffeeScreenRenderer {
    val image: @Composable (String, String, Modifier, androidx.compose.ui.layout.ContentScale) -> Unit =
        { url, description, modifier, scale ->
            CoffeeShopImage(imageUrl = url, modifier = modifier, contentDescription = description, contentScale = scale)
        }
    return AndroidCoffeeScreenRenderer(
        createCoffeeListEntry(repository, AndroidCoffeeAnalytics, AndroidCoffeeScreenAnalytics, image) { onRetry -> LoadErrorContent(onRetry) },
        createCoffeeDetailEntry(repository, image,
            { urls, index, onDismiss -> FullScreenImageDialog(urls, index, onDismiss = onDismiss) },
            ::utcIsoToLocalDateTime, AndroidCoffeeAnalytics, AndroidCoffeeScreenAnalytics),
    )
}
