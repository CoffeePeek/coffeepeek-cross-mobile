package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedCollectionCard(value: SavedCollectionSummary, onClick: () -> Unit, modifier: Modifier = Modifier,
    image: @Composable (Modifier) -> Unit = { CoffeePeekImagePlaceholder(it) }) {
    Column(modifier.clickable(role = Role.Button, onClick = onClick).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        image(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)))
        Text(value.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(stringResource(Res.string.collections_saved_count, value.itemCount), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun SavedCollectionCardSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    SavedCollectionCard(SavedCollectionSummary("sample", "Кофейни выходного дня", itemCount = 7), {}, Modifier.width(180.dp))
}
@Preview @Composable private fun SavedCollectionCardLightPreview() = SavedCollectionCardSample(false)
@Preview @Composable private fun SavedCollectionCardDarkPreview() = SavedCollectionCardSample(true)
