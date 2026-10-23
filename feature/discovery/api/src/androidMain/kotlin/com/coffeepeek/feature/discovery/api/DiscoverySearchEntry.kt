package com.coffeepeek.feature.discovery.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The parts of the screen the application measures: an element asks for its exposure to be tracked. */
enum class DiscoveryExposure { Search, Filters, ShopCatalog, Favorite }

/**
 * What the discovery screens borrow from the application: images with their placeholder policy, the brew method icons, the branded illustration of the
 * «Не нашли?» card, the exposure tracking and the haptic tick. Defaults keep previews and tests free of app services.
 */
class DiscoveryHost(
    /** A remote image (a shop cover, a roaster logo); [description] is for accessibility. */
    val image: @Composable (url: String, description: String?, modifier: Modifier, scale: ContentScale) -> Unit = { _, _, _, _ -> },
    /** What a shop without a photo shows. */
    val coverPlaceholder: @Composable (description: String?, modifier: Modifier) -> Unit = { _, _ -> },
    val brewMethodIcon: @Composable (name: String) -> Unit = {},
    val tagIcon: @Composable (tagId: String) -> Unit = {},
    val roasterLogo: @Composable (name: String, logoUrl: String?) -> Unit = { _, _ -> },
    val suggestionIllustration: @Composable () -> Painter = { error("The application provides the illustration") },
    val exposure: (Modifier, DiscoveryExposure) -> Modifier = { modifier, _ -> modifier },
    val haptic: () -> Unit = {},
)

/**
 * The shop search: the search field with the filter button, the quick filters, the filter sheet and the list of results. While nothing is searched
 * or filtered the [discovery] content is shown instead of the list; with a text, [textResults] (the roasters that match) leads the list.
 * The caller owns navigation and the ViewModel store.
 */
interface DiscoverySearchEntry {
    @Composable
    fun Content(
        onOpenShop: (slug: String) -> Unit,
        onSignIn: () -> Unit,
        onAddShop: () -> Unit,
        modifier: Modifier = Modifier,
        /** The floating navigation (and the system bar) that the lists must clear at the bottom. */
        bottomClearance: Dp = 0.dp,
        discovery: @Composable () -> Unit = {},
        textResults: (@Composable () -> Unit)? = null,
    )
}
