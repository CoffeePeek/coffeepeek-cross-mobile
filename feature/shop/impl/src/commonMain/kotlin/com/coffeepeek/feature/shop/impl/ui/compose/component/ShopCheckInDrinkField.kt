package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
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
    var expanded by remember { mutableStateOf(false) }
    val language = Locale.current.language
    val selectedDrink = drinks.firstOrNull { it.slug == slug }
    val errorMessage = stringResource(Res.string.shop_checkin_form_invalid_drink)
    val label = when (slug) {
        null -> stringResource(Res.string.shop_checkin_form_drink_none)
        "other" -> stringResource(Res.string.shop_checkin_form_drink_other)
        else -> selectedDrink?.displayName(language) ?: slug
    }
    Column {
        Text(stringResource(Res.string.shop_checkin_form_drink), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled && !loading, modifier = Modifier.fillMaxWidth()) {
                Text(label)
            }
            DropdownMenu(expanded = expanded && enabled && !loading, onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp)) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.shop_checkin_form_drink_none)) },
                    onClick = { onSelect(null); expanded = false }, modifier = Modifier.semantics { selected = slug == null })
                if (slug != null && selectedDrink == null) {
                    DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false },
                        modifier = Modifier.semantics { selected = true })
                }
                drinks.forEach { drink ->
                    DropdownMenuItem(text = { Text(drink.displayName(language)) },
                        onClick = { onSelect(drink.slug); expanded = false },
                        modifier = Modifier.semantics { selected = drink.slug == slug })
                }
            }
        }
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
        )
    }
}

private fun ShopConsumedDrinkOption.displayName(language: String): String =
    (if (language == "en") nameEn.ifBlank { nameRu } else nameRu.ifBlank { nameEn }).ifBlank { slug }

@Preview @Composable private fun ShopCheckInDrinkFieldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInDrinkField(listOf(ShopConsumedDrinkOption("filter", "Фильтр", "Filter")), "filter", null,
        false, false, true, false, {}, {}, {})
}
@Preview @Composable private fun ShopCheckInDrinkFieldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInDrinkField(emptyList(), "other", "", false, true, true, true, {}, {}, {})
}
