package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekChipOption
import com.coffeepeek.core.designsystem.component.CoffeePeekChoiceChips
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_drink
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_drink_other
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_drinks_empty
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_drinks_error
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_drink
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_retry
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInDrinkField(
    drinks: List<ShopConsumedDrinkOption>, slug: String?, customName: String?,
    loading: Boolean, failed: Boolean, enabled: Boolean, invalid: Boolean,
    onRetry: () -> Unit, onSelect: (String?) -> Unit,
) {
    val language = LocalCoffeePeekLanguageTag.current
    val errorMessage = stringResource(Res.string.shop_checkin_form_invalid_drink)
    val options = drinks.map { CoffeePeekChipOption(it.slug, it.displayName(language)) }
    val legacyLabel = stringResource(Res.string.shop_checkin_form_drink_other)
    val legacy = slug?.takeIf { saved -> options.none { it.id == saved } }?.let { saved ->
        CoffeePeekChipOption(saved, customName?.takeIf(String::isNotBlank)
            ?: if (saved == "other") legacyLabel else saved)
    }
    Column(Modifier.semantics { if (invalid) error(errorMessage) },
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(stringResource(Res.string.shop_checkin_form_drink), style = MaterialTheme.typography.titleMedium)
        CoffeePeekChoiceChips(listOfNotNull(legacy) + options, slug, onSelect, enabled)
        when {
            loading -> CoffeePeekLoader(size = 24.dp)
            failed -> {
                Text(stringResource(Res.string.shop_checkin_form_drinks_error), color = MaterialTheme.colorScheme.error)
                CoffeePeekTextButton(onClick = onRetry, enabled = enabled) { Text(stringResource(Res.string.shop_checkin_form_retry)) }
            }
            options.isEmpty() -> Text(stringResource(Res.string.shop_checkin_form_drinks_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun ShopConsumedDrinkOption.displayName(language: String): String =
    (if (language == "en") nameEn.ifBlank { nameRu } else nameRu.ifBlank { nameEn }).ifBlank { slug }

@Composable
private fun ShopCheckInDrinkFieldPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShopCheckInDrinkField(
        listOf(ShopConsumedDrinkOption("filter", "Фильтр", "Filter"),
            ShopConsumedDrinkOption("cappuccino", "Капучино", "Cappuccino")),
        if (dark) "other" else "filter", "Тоник из старого черновика",
        loading = false, failed = false, enabled = true, invalid = false, onRetry = {}, onSelect = {},
    )
}

@Preview @Composable private fun ShopCheckInDrinkFieldLightPreview() = ShopCheckInDrinkFieldPreview(false)
@Preview @Composable private fun ShopCheckInDrinkFieldDarkPreview() = ShopCheckInDrinkFieldPreview(true)
