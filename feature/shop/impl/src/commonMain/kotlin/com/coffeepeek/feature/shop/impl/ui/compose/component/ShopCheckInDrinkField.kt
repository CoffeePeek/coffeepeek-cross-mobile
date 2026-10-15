package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
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
        "other" -> customName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.shop_checkin_form_drink_other)
        else -> selectedDrink?.displayName(language) ?: slug
    }
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(stringResource(Res.string.shop_checkin_form_drink), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled && !loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { stateDescription = label },
                shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    contentColor = MaterialTheme.colorScheme.onSurface)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Icon(CoffeePeekIcons.ChevronUpDown, null, Modifier.size(18.dp))
            }
            DropdownMenu(expanded = expanded && enabled && !loading, onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 220.dp, max = 320.dp).heightIn(max = 320.dp),
                shape = RoundedCornerShape(14.dp), containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                ShopDrinkMenuItem(stringResource(Res.string.shop_checkin_form_drink_none), slug == null) {
                    onSelect(null); expanded = false
                }
                if (drinks.isNotEmpty() || slug != null) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                if (slug != null && selectedDrink == null) {
                    ShopDrinkMenuItem(label, true) { expanded = false }
                }
                drinks.forEach { drink ->
                    ShopDrinkMenuItem(drink.displayName(language), drink.slug == slug) {
                        if (drink.slug != slug) onSelect(drink.slug)
                        expanded = false
                    }
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
