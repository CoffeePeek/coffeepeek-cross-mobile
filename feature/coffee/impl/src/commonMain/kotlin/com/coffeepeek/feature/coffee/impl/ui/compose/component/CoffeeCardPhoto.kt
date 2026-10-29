package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.Coffee
import com.coffeepeek.feature.coffee.impl.resources.*

@Composable
internal fun CoffeeCardPhoto(coffee: Coffee, imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(216.dp).clipToBounds()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        coffee.photoUrl?.let { imageContent(it, coffee.name, Modifier.matchParentSize(), ContentScale.Fit) }
            ?: CoffeePeekImagePlaceholder(contentDescription = stringResource(Res.string.coffee_photo_missing))

        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(CoffeePeekDimens.spacing4).size(48.dp)
                .shadow(3.dp, CircleShape).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                .semantics { contentDescription = coffee.roasterName },
            contentAlignment = Alignment.Center,
        ) {
            val logo = coffee.roasterPhotoUrl
            if (!logo.isNullOrBlank()) {
                imageContent(logo, coffee.roasterName, Modifier.matchParentSize(), ContentScale.Crop)
            } else {
                Text(
                    text = coffee.roasterName.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable private fun CoffeeCardPhotoPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeeCardPhoto(previewCoffee().copy(roasterPhotoUrl = "preview-logo")) { _, _, modifier, _ ->
        CoffeePeekImagePlaceholder(modifier)
    }
}
@Preview @Composable private fun CoffeeCardPhotoLightPreview() = CoffeeCardPhotoPreview(false)
@Preview @Composable private fun CoffeeCardPhotoDarkPreview() = CoffeeCardPhotoPreview(true)
