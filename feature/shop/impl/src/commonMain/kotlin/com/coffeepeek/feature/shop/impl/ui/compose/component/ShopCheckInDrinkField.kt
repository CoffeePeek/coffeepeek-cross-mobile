package com.coffeepeek.feature.shop.impl.ui.compose.component

import com.coffeepeek.core.designsystem.component.CoffeePeekSelectField
import com.coffeepeek.core.designsystem.component.CoffeePeekSelectOption

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_drink
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_drink_none
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_drink_other
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_drink_custom
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_drinks_error
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_retry
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_drink
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInDrinkField(
    drinks: List<ShopConsumedDrinkOption>, slug: String?, customName: String?,
    loading: Boolean, failed: Boolean, enabled: Boolean, invalid: Boolean,
    onRetry: () -> Unit, onSelect: (String?) -> Unit, onCustomName: (String) -> Unit,
) {
    val language = Locale.current.language
    val selectedDrink = drinks.firstOrNull { it.slug == slug }
    val errorMessage = stringResource(Res.string.shop_checkin_form_invalid_drink)
    val label = when (slug) {
        null -> stringResource(Res.string.shop_checkin_form_drink_none)
        "other" -> customName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.shop_checkin_form_drink_other)
        else -> selectedDrink?.displayName(language) ?: slug
    }
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(stringResource(Res.string.shop_checkin_form_drink), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface)
        val none = stringResource(Res.string.shop_checkin_form_drink_none)
        val options = buildList {
            add(CoffeePeekSelectOption("none", none, slug == null) { onSelect(null) })
            if (slug != null && selectedDrink == null) {
                add(CoffeePeekSelectOption("saved:$slug", label, true) { })
            }
            drinks.forEach { drink ->
                add(CoffeePeekSelectOption("drink:${drink.slug}", drink.displayName(language), drink.slug == slug) {
                    if (drink.slug != slug) onSelect(drink.slug)
                })
            }
        }
        CoffeePeekSelectField(label, options, stringResource(Res.string.shop_checkin_form_drink),
            modifier = Modifier.fillMaxWidth(), enabled = enabled && !loading)
        if (loading) CircularProgressIndicator()
        if (failed) {
            Text(stringResource(Res.string.shop_checkin_form_drinks_error), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry, enabled = !loading) { Text(stringResource(Res.string.shop_checkin_form_retry)) }
        }
        if (slug == "other") OutlinedTextField(
            value = customName.orEmpty(), onValueChange = onCustomName, enabled = enabled,
            isError = invalid, singleLine = true, modifier = Modifier.fillMaxWidth().semantics {
                if (invalid) error(errorMessage)
            },
            label = { Text(stringResource(Res.string.shop_checkin_form_drink_custom)) },
            shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
        )
    }
}

private fun ShopConsumedDrinkOption.displayName(language: String): String =
    (if (language == "en") nameEn.ifBlank { nameRu } else nameRu.ifBlank { nameEn }).ifBlank { slug }

@Preview @Composable private fun ShopCheckInDrinkFieldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    Surface { ShopCheckInDrinkField(listOf(ShopConsumedDrinkOption("filter", "Фильтр", "Filter")), "filter", null,
        false, false, true, false, {}, {}, {}) }
}
@Preview @Composable private fun ShopCheckInDrinkFieldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    Surface { ShopCheckInDrinkField(emptyList(), "other", "", false, true, true, true, {}, {}, {}) }
}
