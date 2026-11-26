@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.core.designsystem.component.coffeePeekNativeDrawableImage
import com.coffeepeek.core.designsystem.component.coffeePeekNativeVectorImage
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.impl.ui.compose.component.discoveryBrewArtwork
import com.coffeepeek.feature.discovery.impl.ui.compose.component.discoveryTagArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.*
import platform.UIKit.UIImage
import platform.UIKit.UIScreen

/** One language/artwork lifetime; the source is created on the native main actor. */
internal class DiscoveryIosResources {
    private val environment = getSystemResourceEnvironment()
    private val scale = UIScreen.mainScreen.scale.toFloat()
    private val brewImages = mutableMapOf<DrawableResource, UIImage>()
    private val tagImages = mutableMapOf<String, UIImage>()
    suspend fun text(resource: StringResource, vararg args: Any): String =
        if (args.isEmpty()) getString(environment, resource) else getString(environment, resource, *args)
    suspend fun price(level: PriceLevel?) = text(priceLabelResource(level))
    suspend fun distance(meters: Double): String = distanceLabelValue(meters).let { text(it.first, it.second) }
    suspend fun label(group: DiscoveryFilterGroup, entry: CatalogEntry): String {
        val key = when (group) {
            DiscoveryFilterGroup.BrewMethods -> brewMethodLabelResource(entry.id)
            DiscoveryFilterGroup.Tags -> tagLabelResource(entry.id)
            else -> null
        }
        return key?.let { text(it) } ?: entry.name
    }
    suspend fun filterIcon(group: DiscoveryFilterGroup, entry: CatalogEntry): UIImage? = when (group) {
        DiscoveryFilterGroup.BrewMethods -> brew(entry.name)
        DiscoveryFilterGroup.Tags -> tag(entry.id)
        else -> null
    }
    suspend fun brew(name: String): UIImage {
        val resource = discoveryBrewArtwork(name)
        brewImages[resource]?.let { return it }
        val bytes = getDrawableResourceBytes(environment, resource)
        val image = withContext(Dispatchers.Default) {
            coffeePeekNativeDrawableImage(bytes, 28f, scale)
        }
        brewImages[resource] = image
        return image
    }
    private suspend fun tag(id: String): UIImage {
        tagImages[id]?.let { return it }
        val image = withContext(Dispatchers.Default) { coffeePeekNativeVectorImage(discoveryTagArtwork(id), 28f, scale) }
        tagImages[id] = image
        return image
    }
}
