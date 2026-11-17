package com.coffeepeek.feature.collections.impl.ui.compose.component

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        overlay = { SavedCollectionCardCaption(value, Modifier.matchParentSize()) })
}

@Composable
private fun SavedCollectionCardCaption(value: SavedCollectionSummary, modifier: Modifier) {
    Box(modifier.background(Brush.verticalGradient(
        0f to Color.Transparent, .35f to Color.Black.copy(alpha = .08f),
        .65f to Color.Black.copy(alpha = .45f), 1f to Color.Black.copy(alpha = .85f),
    ))) {
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value.title, style = MaterialTheme.typography.titleMedium, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(Res.string.collections_saved_count, value.itemCount),
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .9f))
        }
    }
}

@Composable private fun SavedCollectionCardSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    SavedCollectionCard(SavedCollectionSummary("sample", "Кофейни выходного дня", itemCount = 7), {}, Modifier.width(180.dp)) {
        Box(it.background(Brush.horizontalGradient(listOf(Color(0xFFF1D5A9), Color(0xFF669A91), Color(0xFFEDDCC5)))))
    }
}
@Preview @Composable private fun SavedCollectionCardLightPreview() = SavedCollectionCardSample(false)
@Preview @Composable private fun SavedCollectionCardDarkPreview() = SavedCollectionCardSample(true)
