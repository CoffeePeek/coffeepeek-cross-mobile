package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Photo count/layout only. URL alignment, loading, privacy and original selection belong to the caller. */
@Composable
fun CoffeePeekPhotoStrip(
    count: Int, description: (Int) -> String, onPhoto: (Int) -> Unit,
    modifier: Modifier = Modifier, singleAspectRatio: Float = 2f,
    photoEnabled: (Int) -> Boolean = { true },
    image: @Composable (Int, Modifier) -> Unit,
) {
    if (count <= 0) return
    val tile: @Composable (Int, Modifier) -> Unit = { index, bounds ->
        CoffeePeekPhotoStripTile(index, count, description(index), photoEnabled(index), onPhoto, bounds, image)
    }
    if (count == 1) tile(0, modifier.fillMaxWidth().aspectRatio(singleAspectRatio))
    else Row(modifier.fillMaxWidth().aspectRatio(4f / 3f), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        tile(0, Modifier.weight(1f).fillMaxHeight())
        if (count == 2) tile(1, Modifier.weight(1f).fillMaxHeight())
        else Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            tile(1, Modifier.weight(1f).fillMaxWidth())
            tile(2, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun CoffeePeekPhotoStripTile(index: Int, count: Int, description: String, enabled: Boolean,
    onPhoto: (Int) -> Unit, modifier: Modifier, image: @Composable (Int, Modifier) -> Unit) {
    Box(modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
        .clickable(enabled = enabled, role = Role.Button) { onPhoto(index) }
        .semantics(mergeDescendants = true) { contentDescription = description }) {
        image(index, Modifier.fillMaxSize())
        if (index == 2 && count > 3) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center) {
            Text("+${count - 3}", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }
    }
}

@Composable private fun PhotoStripPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekPhotoStrip(4, { "Фото ${it + 1} из 4" }, {}) { _, modifier ->
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
@Preview @Composable private fun PhotoStripLightPreview() = PhotoStripPreview(false)
@Preview @Composable private fun PhotoStripDarkPreview() = PhotoStripPreview(true)
