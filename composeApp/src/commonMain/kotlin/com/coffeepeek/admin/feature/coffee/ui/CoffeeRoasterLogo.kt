package com.coffeepeek.admin.feature.coffee.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal expect fun CoffeeRoasterLogo(imageUrl: String, description: String, modifier: Modifier)

@Composable
private fun CoffeeRoasterLogoPreview(darkTheme: Boolean) {
    CoffeePeekTheme(darkTheme = darkTheme) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)) {
            CoffeeRoasterLogo(imageUrl = "", description = "Обжарщик", modifier = Modifier.matchParentSize())
        }
    }
}

@Preview
@Composable
private fun CoffeeRoasterLogoLightPreview() = CoffeeRoasterLogoPreview(darkTheme = false)

@Preview
@Composable
private fun CoffeeRoasterLogoDarkPreview() = CoffeeRoasterLogoPreview(darkTheme = true)
