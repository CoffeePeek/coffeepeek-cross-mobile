package com.coffeepeek.feature.discovery.impl.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekBrandedImage
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DiscoveryRoasterAvatar(name: String, logoUrl: String?) {
    Box(Modifier.size(28.dp).clip(CircleShape)) {
        val photo = logoUrl?.takeIf(String::isNotBlank)
        if (photo != null) CoffeePeekBrandedImage(photo, Modifier.fillMaxSize(), name, placeholderLabelSize = 6.sp)
        else CoffeePeekImagePlaceholder(labelSize = 6.sp, contentDescription = name)
    }
}

@Preview @Composable private fun DiscoveryRoasterAvatarLightPreview() = CoffeePeekTheme(darkTheme = false) { DiscoveryRoasterAvatar("Roast", null) }
@Preview @Composable private fun DiscoveryRoasterAvatarDarkPreview() = CoffeePeekTheme(darkTheme = true) { DiscoveryRoasterAvatar("Roast", null) }
