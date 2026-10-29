package com.coffeepeek.admin.di.shelf

import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
import com.coffeepeek.feature.shelf.api.ShelfImage

/** Bag cards go through the protected photo loader: the owner's session, no shared cache, the whole card visible. */
internal val ShelfCardImage: ShelfImage = { url, description, alignment, modifier ->
    CoffeePeekImage(url, contentScale = ContentScale.Fit, alignment = alignment, modifier = modifier, contentDescription = description)
}
