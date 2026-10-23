package com.coffeepeek.admin.feature.coffee.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.admin.ui.component.CoffeeShopImage

@Composable
internal actual fun CoffeeRoasterLogo(imageUrl: String, description: String, modifier: Modifier) {
    CoffeeShopImage(
        imageUrl = imageUrl, contentDescription = description,
        contentScale = ContentScale.Crop, placeholderLabelSize = MaterialTheme.typography.labelSmall.fontSize,
        modifier = modifier,
    )
}
