package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.media.image.rememberImageResource
import io.kamel.core.Resource

@Composable
fun CoffeeShopPlaceholderImage(
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 18.sp,
    contentDescription: String = "Фото кофейни отсутствует",
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDarkTheme) Color.White else Color.Black
    val textColor = if (isDarkTheme) Color.Black else Color.White

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "COFFEEPEEK",
            color = textColor,
            fontSize = labelSize,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Coffee-shop photo with the branded placeholder kept visible until decoding completes. */
@Composable
fun CoffeeShopImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderLabelSize: TextUnit = 18.sp,
) {
    when (val resource = rememberImageResource(imageUrl)) {
        is Resource.Success -> Image(
            painter = resource.value,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
        is Resource.Loading,
        is Resource.Failure -> CoffeeShopPlaceholderImage(
            modifier = modifier,
            labelSize = placeholderLabelSize,
            contentDescription = contentDescription ?: "Фото кофейни загружается",
        )
    }
}
