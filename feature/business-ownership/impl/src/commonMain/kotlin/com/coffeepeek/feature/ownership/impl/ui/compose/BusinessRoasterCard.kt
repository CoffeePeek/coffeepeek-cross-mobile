package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.component.CoffeePeekRoasterCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.api.OwnershipImage
import com.coffeepeek.feature.ownership.domain.model.BusinessKind
import com.coffeepeek.feature.ownership.domain.model.BusinessPhoto
import com.coffeepeek.feature.ownership.domain.model.BusinessProfile
import com.coffeepeek.feature.ownership.domain.model.OwnedBusiness
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun BusinessRoasterCard(business: OwnedBusiness, profile: BusinessProfile?, image: OwnershipImage,
    onClick: () -> Unit, modifier: Modifier = Modifier) {
    val photo = profile?.photos?.firstOrNull()
    CoffeePeekRoasterCard(name = business.name, description = profile?.about, shopsCount = 0, shopsLabel = "",
        productsCount = 0, productsLabel = "", showCounters = false, modifier = modifier,
        hasLogo = photo?.url != null || photo?.bytes != null, onClick = onClick,
        logo = { if (photo != null) image(photo.url, photo.bytes, Modifier.fillMaxSize(), ContentScale.Crop) })
}

@Composable private fun BusinessRoasterCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    BusinessRoasterCard(OwnedBusiness("preview", BusinessKind.Roaster, "CoffeePeek Roasters"),
        BusinessProfile("preview", BusinessKind.Roaster, 1, "CoffeePeek Roasters", about = "Свежая обжарка",
            photos = listOf(BusinessPhoto("cover", fileName = "cover.jpg", url = "preview"))),
        { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) }, {})
}

@Preview @Composable private fun BusinessRoasterCardLightPreview() = BusinessRoasterCardPreview(false)
@Preview @Composable private fun BusinessRoasterCardDarkPreview() = BusinessRoasterCardPreview(true)
