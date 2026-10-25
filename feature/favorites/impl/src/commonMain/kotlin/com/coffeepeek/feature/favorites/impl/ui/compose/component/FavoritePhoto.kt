package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.impl.resources.Res
import com.coffeepeek.feature.favorites.impl.resources.favorites_photo_missing
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FavoritePhoto(url: String?, title: String, modifier: Modifier) {
    if (url.isNullOrBlank()) {
        val missingDescription = stringResource(Res.string.favorites_photo_missing, title)
        CoffeePeekImagePlaceholder(modifier, contentDescription = missingDescription)
    } else {
        KamelImage(
            resource = { asyncPainterResource(url) },
            contentDescription = title,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onLoading = { FavoritePhoto(null, title, Modifier.fillMaxSize()) },
            onFailure = { FavoritePhoto(null, title, Modifier.fillMaxSize()) },
        )
    }
}

@Preview @Composable private fun FavoritePhotoLightPreview() = CoffeePeekTheme(darkTheme = false) {
    FavoritePhoto(null, "Кофейня", Modifier.size(240.dp))
}
@Preview @Composable private fun FavoritePhotoDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    FavoritePhoto(null, "Кофейня", Modifier.size(240.dp))
}
