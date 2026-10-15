package com.coffeepeek.admin.utils

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.utils.DrawableExt.toPainterResource
import io.kamel.core.Resource
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun CpImage(
    data: Any,
    placeholder: DrawableResource? = null,
    // ponytail: image_not_available.png was deleted; null → neutral color fallback in rememberCpImagePainter.
    error: DrawableResource? = null,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = rememberCpImagePainter(cpImageData(data), placeholder, error),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

internal fun cpImageData(data: Any): Any = when (data) {
    is String -> CustomUrl(when {
        data.startsWith("https://") || data.startsWith("http://") -> data
        data.startsWith("/") -> "${Constants.BASE_URL.trimEnd('/')}$data"
        else -> "${Constants.BASE_URL}api/file/$data"
    })
    else -> data
}

@Composable
fun rememberCpImagePainter(
    resource: Any,
    placeholder: DrawableResource? = null,
    error: DrawableResource? = null,
): androidx.compose.ui.graphics.painter.Painter {
    val loadingPlaceholder = placeholder?.toPainterResource()
        ?: ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    return when (val r = asyncPainterResource(data = resource)) {
        is Resource.Failure -> error?.toPainterResource() ?: loadingPlaceholder
        is Resource.Loading -> loadingPlaceholder
        is Resource.Success -> r.value
    }
}
