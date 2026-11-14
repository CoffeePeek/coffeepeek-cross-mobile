package com.coffeepeek.feature.collections.impl.ui.compose.component

import com.coffeepeek.core.designsystem.theme.CoffeePeekColors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekFrostedSurface
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoBackdrop
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedCollectionCard(value: SavedCollectionSummary, onClick: () -> Unit, modifier: Modifier = Modifier,
    image: @Composable (Modifier) -> Unit = { CoffeePeekImagePlaceholder(it) }) {
    CoffeePeekPhotoBackdrop(modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp))
        .clickable(role = Role.Button, onClick = onClick).semantics(mergeDescendants = true) {},
        photo = { image(Modifier.matchParentSize()) },
        overlay = { SavedCollectionCardCaption(value, Modifier.align(Alignment.BottomCenter)) })
}

@Composable
private fun SavedCollectionCardCaption(value: SavedCollectionSummary, modifier: Modifier) {
    Box(modifier.fillMaxWidth().heightIn(min = 96.dp)) {
        CoffeePeekFrostedSurface(Modifier.matchParentSize().bottomBlurFade(),
            shape = RoundedCornerShape(0.dp), tint = Color.Black.copy(alpha = .2f), blurRadius = 14.dp)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color.Transparent, .4f to Color.Black.copy(alpha = .2f), 1f to Color.Black.copy(alpha = .8f))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(CoffeePeekColors.PhotoBadgeOverlay).padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value.title, style = MaterialTheme.typography.titleMedium, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(Res.string.collections_saved_count, value.itemCount),
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .9f))
        }
    }
}

private fun Modifier.bottomBlurFade(): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(Brush.verticalGradient(0f to Color.Transparent, .3f to Color.White.copy(alpha = .12f),
            1f to Color.White), blendMode = BlendMode.DstIn)
    }

@Composable private fun SavedCollectionCardSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    SavedCollectionCard(SavedCollectionSummary("sample", "Кофейни выходного дня", itemCount = 7), {}, Modifier.width(180.dp))
}
@Preview @Composable private fun SavedCollectionCardLightPreview() = SavedCollectionCardSample(false)
@Preview @Composable private fun SavedCollectionCardDarkPreview() = SavedCollectionCardSample(true)
