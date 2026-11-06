package com.coffeepeek.feature.contributions.impl.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekAvatar
import com.coffeepeek.core.designsystem.component.CoffeePeekBrandedImage
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ContributionRoasterLogo(item: CatalogOption) {
    CoffeePeekAvatar(item.name, Modifier.size(28.dp)) { imageModifier ->
        val photo = item.photoUrl?.takeIf(String::isNotBlank)
        if (photo != null) CoffeePeekBrandedImage(photo, imageModifier, item.name, placeholderLabelSize = 6.sp)
        else CoffeePeekImagePlaceholder(imageModifier, labelSize = 6.sp, contentDescription = item.name)
    }
}

@Preview @Composable private fun ContributionRoasterLogoLightPreview() = CoffeePeekTheme(darkTheme = false) { ContributionRoasterLogo(CatalogOption("r", "Roast")) }
@Preview @Composable private fun ContributionRoasterLogoDarkPreview() = CoffeePeekTheme(darkTheme = true) { ContributionRoasterLogo(CatalogOption("r", "Roast")) }
