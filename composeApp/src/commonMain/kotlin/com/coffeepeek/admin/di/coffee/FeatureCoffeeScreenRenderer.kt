package com.coffeepeek.admin.di.coffee

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewer
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

/** Both platforms use migrated catalogue entries; browser, share, photos and root navigation remain app-owned. */
internal class FeatureCoffeeScreenRenderer(
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
            onShare = { name, path -> ShareHelper.shareText("$name — https://coffeepeek.by$path") }, modifier = Modifier)
    }
}

internal fun featureCoffeeScreenRenderer(repository: CoffeeRepository): FeatureCoffeeScreenRenderer {
    val image: @Composable (String, String, Modifier, androidx.compose.ui.layout.ContentScale) -> Unit =
        { url, description, modifier, scale ->
            CoffeeShopImage(imageUrl = url, modifier = modifier, contentDescription = description, contentScale = scale)
        }
    return FeatureCoffeeScreenRenderer(
        createCoffeeListEntry(repository, AppCoffeeAnalytics, AppCoffeeScreenAnalytics, image) { onRetry -> LoadErrorContent(onRetry) },
        createCoffeeDetailEntry(repository, image,
            { urls, index, onDismiss -> CoffeePeekPhotoViewer(urls, index, onDismiss = onDismiss) },
            ::utcIsoToLocalDateTime, AppCoffeeAnalytics, AppCoffeeScreenAnalytics),
    )
}
