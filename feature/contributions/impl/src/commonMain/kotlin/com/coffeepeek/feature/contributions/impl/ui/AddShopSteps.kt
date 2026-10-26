package com.coffeepeek.feature.contributions.impl.ui

import com.coffeepeek.feature.contributions.impl.ui.component.ContributionPriceSelector
import androidx.compose.foundation.layout.Arrangement
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_loading
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_address_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_address_placeholder
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_city_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_contacts_intro
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_description_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_description_placeholder
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_email_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_email_placeholder
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_instagram_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_instagram_placeholder
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_location_denied
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_location_no_address
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_location_not_found
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_name_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_name_placeholder
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_phone_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_phone_placeholder
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_pick_on_map
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_website_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_website_placeholder
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.contributions.api.AddShopHost
import com.coffeepeek.feature.contributions.domain.rules.ContactIssue
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.domain.rules.ShopBasicsIssue
import com.coffeepeek.feature.contributions.impl.shop.AddShopAction
import com.coffeepeek.feature.contributions.impl.shop.AddShopState
import com.coffeepeek.feature.contributions.impl.shop.LocationHint

@Composable
internal fun StepBasic(state: AddShopState, onAction: (AddShopAction) -> Unit) {
    RequiredLegend()
    Spacer(Modifier.height(CoffeePeekDimens.spacing3))
    val issues = state.basicsIssues
    FormField(stringResource(Res.string.contributions_shop_name_label), required = true) {
        OutlinedField(
            state.name, { onAction(AddShopAction.Name(it)) }, stringResource(Res.string.contributions_shop_name_placeholder),
            errorText = issues.firstOrNull { it == ShopBasicsIssue.NameBlank || it == ShopBasicsIssue.NameTooLong }?.takeIf { state.name.isNotEmpty() }?.message(),
            counter = "${state.name.length}/${ContributionLimits.SHOP_NAME}",
        )
    }
    Spacer(Modifier.height(CoffeePeekDimens.spacing4))
    FormField(stringResource(Res.string.contributions_shop_description_label), optional = true) {
        OutlinedField(state.description, { onAction(AddShopAction.Description(it)) }, stringResource(Res.string.contributions_shop_description_placeholder), minLines = 3, maxLines = 6)
    }
    Spacer(Modifier.height(CoffeePeekDimens.spacing4))
    FormField(stringResource(Res.string.contributions_shop_city_label), required = true) {
        val cities = state.catalogs.cities
        if (cities.size == 1) ReadOnlyField(cities.first().name)
        else CityField(cities, state.cityId, { onAction(AddShopAction.City(it)) }, ShopBasicsIssue.CityMissing.takeIf { it in issues && state.name.isNotEmpty() }?.message())
    }
    Spacer(Modifier.height(CoffeePeekDimens.spacing4))
    FormField(stringResource(Res.string.contributions_shop_address_label), required = true) { AddressRow(state, onAction) }
    Spacer(Modifier.height(CoffeePeekDimens.spacing4))
    ContributionPriceSelector(state.priceRange) { onAction(AddShopAction.Price(it)) }
}

@Composable
private fun AddressRow(state: AddShopState, onAction: (AddShopAction) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalAlignment = Alignment.Top) {
        OutlinedField(
            state.address, { onAction(AddShopAction.Address(it)) }, stringResource(Res.string.contributions_shop_address_placeholder),
            errorText = ShopBasicsIssue.AddressBlank.takeIf { it in state.basicsIssues && state.address.isNotEmpty() }?.message(),
            leadingIcon = CoffeePeekIcons.Navigation, modifier = Modifier.weight(1f),
        )
        MapButton(state.resolvingAddress) { onAction(AddShopAction.OpenMap) }
    }
    state.locationHint?.let { hint ->
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        Text(hint.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MapButton(resolving: Boolean, onClick: () -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        FilledIconButton(
            onClick = onClick, enabled = !resolving, modifier = Modifier.size(CoffeePeekDimens.buttonHeight), shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            ),
        ) {
            if (resolving) CoffeePeekLoader(stringResource(Res.string.contributions_loading), Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(CoffeePeekIcons.Map, stringResource(Res.string.contributions_shop_pick_on_map), Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LocationHint.text() = stringResource(
    when (this) {
        LocationHint.NotFound -> Res.string.contributions_shop_location_not_found
        LocationHint.NoAddress -> Res.string.contributions_shop_location_no_address
        LocationHint.PermissionDenied -> Res.string.contributions_shop_location_denied
    },
)

@Composable
internal fun StepContacts(state: AddShopState, onAction: (AddShopAction) -> Unit) {
    OptionalLegend()
    Text(
        stringResource(Res.string.contributions_shop_contacts_intro), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = CoffeePeekDimens.spacing4),
    )
    val issues = state.contactIssues
    ContactField(Res.string.contributions_shop_phone_label, Res.string.contributions_shop_phone_placeholder, state.phone, KeyboardType.Phone, CoffeePeekIcons.Phone, ContactIssue.Phone.takeIf { it in issues }) { onAction(AddShopAction.Phone(it)) }
    Spacer(Modifier.height(CoffeePeekDimens.spacing3))
    ContactField(Res.string.contributions_shop_instagram_label, Res.string.contributions_shop_instagram_placeholder, state.instagram, KeyboardType.Uri, CoffeePeekIcons.Instagram, ContactIssue.Instagram.takeIf { it in issues }) { onAction(AddShopAction.Instagram(it)) }
    Spacer(Modifier.height(CoffeePeekDimens.spacing3))
    ContactField(Res.string.contributions_shop_website_label, Res.string.contributions_shop_website_placeholder, state.website, KeyboardType.Uri, CoffeePeekIcons.Globe, ContactIssue.Website.takeIf { it in issues }) { onAction(AddShopAction.Website(it)) }
    Spacer(Modifier.height(CoffeePeekDimens.spacing3))
    ContactField(Res.string.contributions_shop_email_label, Res.string.contributions_shop_email_placeholder, state.email, KeyboardType.Email, CoffeePeekIcons.Email, ContactIssue.Email.takeIf { it in issues }) { onAction(AddShopAction.Email(it)) }
}

@Composable
private fun ContactField(
    label: StringResource, placeholder: StringResource, value: String, keyboard: KeyboardType, icon: ImageVector,
    issue: ContactIssue?, onChange: (String) -> Unit,
) {
    FormField(stringResource(label), optional = true) {
        OutlinedField(value, onChange, stringResource(placeholder), errorText = issue?.takeIf { value.isNotEmpty() }?.message(), keyboardType = keyboard, leadingIcon = icon)
    }
}
