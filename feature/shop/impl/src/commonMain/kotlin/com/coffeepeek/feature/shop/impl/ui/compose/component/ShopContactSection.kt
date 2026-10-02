package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopContact
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_contact_copy_phone
import com.coffeepeek.feature.shop.impl.resources.shop_contact_title
import com.coffeepeek.feature.shop.impl.ui.data.ContactLink
import com.coffeepeek.feature.shop.impl.ui.data.emailLink
import com.coffeepeek.feature.shop.impl.ui.data.instagramLink
import com.coffeepeek.feature.shop.impl.ui.data.phoneLink
import com.coffeepeek.feature.shop.impl.ui.data.websiteLink
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** External actions are delegated to the feature entry/application boundary. */
@Composable
internal fun ShopContactSection(
    contact: ShopContact,
    onOpenLink: (String) -> Unit,
    onCopyPhone: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val phone = phoneLink(contact.phone)
    val links = listOfNotNull(
        instagramLink(contact.instagram),
        websiteLink(contact.website),
        emailLink(contact.email),
    )
    if (phone == null && links.isEmpty()) return
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            Text(stringResource(Res.string.shop_contact_title), style = MaterialTheme.typography.titleMedium)
            phone?.let { link ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ContactLinkButton(link, onOpenLink, Modifier.weight(1f))
                    TextButton(onClick = { onCopyPhone(link.label) }) {
                        Text(stringResource(Res.string.shop_contact_copy_phone))
                    }
                }
            }
            links.forEach { link -> ContactLinkButton(link, onOpenLink) }
        }
    }
}

@Composable
private fun ContactLinkButton(link: ContactLink, onOpenLink: (String) -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = { onOpenLink(link.target) }, modifier = modifier) {
        Text(link.label, maxLines = 1)
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
