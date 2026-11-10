package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_photo_loading
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Same image pipeline, with the existing brand visible until decoding succeeds (also on failure). */
@Composable
fun CoffeePeekBrandedImage(imageUrl: String, modifier: Modifier = Modifier, contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop, placeholderLabelSize: TextUnit = 18.sp) {
    CoffeePeekImage(data = imageUrl, modifier = modifier, contentDescription = contentDescription, contentScale = contentScale,
        placeholderContent = { placeholderModifier -> CoffeePeekImagePlaceholder(placeholderModifier, placeholderLabelSize,
            contentDescription ?: stringResource(Res.string.coffeepeek_photo_loading)) })
}

@Preview @Composable private fun CoffeePeekBrandedImageLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekBrandedImage("", Modifier.size(160.dp))
}
@Preview @Composable private fun CoffeePeekBrandedImageDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekBrandedImage("", Modifier.size(160.dp))
}
