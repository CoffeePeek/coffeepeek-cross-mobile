package com.coffeepeek.feature.contributions.impl.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ContributionShopCover(bytes: ByteArray?, modifier: Modifier) {
    if (bytes != null) CoffeePeekImage(data = bytes, modifier = modifier, contentScale = ContentScale.Crop)
    else CoffeePeekImagePlaceholder(modifier, labelSize = 16.sp)
}

@Composable private fun CoverPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Box(Modifier.size(240.dp, 120.dp)) { ContributionShopCover(null, Modifier.fillMaxSize()) }
}
@Preview @Composable private fun ContributionShopCoverLightPreview() = CoverPreview(false)
@Preview @Composable private fun ContributionShopCoverDarkPreview() = CoverPreview(true)
