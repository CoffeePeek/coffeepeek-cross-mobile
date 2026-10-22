package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
fun CoffeePeekCatalogSuggestionCard(
    title: String,
    description: String,
    actionLabel: String,
    illustration: Painter,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(CoffeePeekDimens.radiusXl))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing6),
        horizontalAlignment = Alignment.Start) {
        Image(illustration, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.size(132.dp).align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(CoffeePeekDimens.spacing3))
        Column(Modifier.fillMaxWidth().padding(end = CoffeePeekDimens.spacing5)) {
            Text(title, Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(CoffeePeekDimens.spacing2))
            Text(description, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        TextButton(onAdd, contentPadding = PaddingValues(horizontal = 0.dp, vertical = CoffeePeekDimens.spacing2)) {
            Icon(CoffeePeekIcons.Add, contentDescription = null, Modifier.size(20.dp))
            Spacer(Modifier.width(CoffeePeekDimens.spacing2))
            Text(actionLabel)
        }
    }
}

@Composable
private fun CatalogSuggestionPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        CoffeePeekCatalogSuggestionCard("Missing item?", "Suggest an addition to the catalog.", "Add item",
            ColorPainter(Color.LightGray), {}, Modifier.padding(16.dp))
    }
}

@Preview @Composable private fun CatalogSuggestionLightPreview() = CatalogSuggestionPreview(false)
@Preview @Composable private fun CatalogSuggestionDarkPreview() = CatalogSuggestionPreview(true)
