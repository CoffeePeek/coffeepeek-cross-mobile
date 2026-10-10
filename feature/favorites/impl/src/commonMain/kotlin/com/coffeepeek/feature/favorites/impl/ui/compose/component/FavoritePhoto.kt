package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
        Box(modifier.semantics { contentDescription = missingDescription },
            contentAlignment = Alignment.Center) {
            Text("COFFEEPEEK", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
