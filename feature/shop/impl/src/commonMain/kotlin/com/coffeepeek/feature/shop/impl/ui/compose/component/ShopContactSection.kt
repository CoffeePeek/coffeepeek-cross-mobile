package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.feature.shop.api.ShopExposure
import com.coffeepeek.feature.shop.impl.ui.data.shopExposure
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopContact
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_contact_title
import com.coffeepeek.feature.shop.impl.ui.data.emailLink
import com.coffeepeek.feature.shop.impl.ui.data.instagramLink
import com.coffeepeek.feature.shop.impl.ui.data.phoneLink
import com.coffeepeek.feature.shop.impl.ui.data.websiteLink
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Validated external targets leave the feature only through caller-owned actions. */
@Composable
internal fun ShopContactSection(
    contact: ShopContact,
    onOpenLink: (String) -> Unit,
    onCopyPhone: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val phone = phoneLink(contact.phone)
    val links = listOfNotNull(
        instagramLink(contact.instagram)?.let { it to CoffeePeekIcons.Instagram },
        websiteLink(contact.website)?.let { it to CoffeePeekIcons.Globe },
        emailLink(contact.email)?.let { it to CoffeePeekIcons.Email },
    )
    if (phone == null && links.isEmpty()) return
    Column(modifier.shopExposure(ShopExposure.Contacts).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        ShopSectionTitle(stringResource(Res.string.shop_contact_title))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            phone?.let { link ->
                ShopContactRow(link, CoffeePeekIcons.Phone, onOpenLink, onCopy = { onCopyPhone(link.label) })
            }
            links.forEachIndexed { index, (link, icon) ->
                if (phone != null || index > 0) HorizontalDivider(Modifier.padding(horizontal = CoffeePeekDimens.spacing4),
                    color = MaterialTheme.colorScheme.outline)
                ShopContactRow(link, icon, onOpenLink)
            }
        }
    }
}

@Preview @Composable private fun ShopContactSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopContactSection(previewContact(), onOpenLink = {}, onCopyPhone = {})
}
@Preview @Composable private fun ShopContactSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopContactSection(previewContact(), onOpenLink = {}, onCopyPhone = {})
}
private fun previewContact() = ShopContact(
    phone = "+375 29 123 45 67",
    email = "hello@coffeepeek.app",
    website = "coffeepeek.app",
    instagram = "@coffeepeek",
)
