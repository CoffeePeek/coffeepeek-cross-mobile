package com.coffeepeek.feature.discovery.impl.ui.compose.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekBrandedImage
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DiscoveryShopRoasterLogo(name: String, logoUrl: String?, modifier: Modifier = Modifier) {
    if (logoUrl != null) CoffeePeekBrandedImage(logoUrl, modifier.fillMaxSize(), name, placeholderLabelSize = 5.sp)
}

@Preview @Composable private fun DiscoveryShopRoasterLogoLightPreview() = CoffeePeekTheme(darkTheme = false) {
    DiscoveryShopRoasterLogo("Roast", "", Modifier.size(28.dp))
}
@Preview @Composable private fun DiscoveryShopRoasterLogoDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    DiscoveryShopRoasterLogo("Roast", "", Modifier.size(28.dp))
}
