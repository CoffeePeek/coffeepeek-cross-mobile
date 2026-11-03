package com.coffeepeek.admin.ui.screen.review

import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

import com.coffeepeek.core.designsystem.component.CoffeePeekSelectField
import com.coffeepeek.core.designsystem.component.CoffeePeekSelectOption
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.select_drink_none
import coffeepeek.composeapp.generated.resources.select_drink_label
import coffeepeek.composeapp.generated.resources.select_drink_other
import coffeepeek.composeapp.generated.resources.select_drink_custom
import coffeepeek.composeapp.generated.resources.select_drink_error
import coffeepeek.composeapp.generated.resources.select_retry
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.ReviewTextInput
import com.coffeepeek.domain.model.ConsumedDrinkOption
import com.coffeepeek.domain.model.savedDrinkName
import com.coffeepeek.domain.model.consumedDrinkIssue

@Composable
internal fun ConsumedDrinkField(
    drinks: List<ConsumedDrinkOption>,
    slug: String?,
    customName: String?,
    savedName: String?,
    error: String?,
    onRetry: () -> Unit,
    onChange: (String?, String?) -> Unit,
    savedNameEn: String? = null,
) {
    val language = Locale.current.language
    val selected = drinks.find { it.slug == slug }
    val selectedName = selected?.let { drink ->
        if (language == "en") drink.nameEn.ifBlank { drink.nameRu }
        else drink.nameRu.ifBlank { drink.nameEn }
    }
    val label = when (slug) {
        null -> stringResource(Res.string.select_drink_none)
        "other" -> customName?.takeIf { it.isNotBlank() }
            ?: selectedName ?: if (language == "en") "Other" else stringResource(Res.string.select_drink_other)
        else -> savedDrinkName(savedName, savedNameEn, null, language) ?: selectedName ?: slug
    }
    val none = stringResource(Res.string.select_drink_none)
    val menuItems = buildList {
        add(CoffeePeekSelectOption("none", none, selected = slug == null) { onChange(null, null) })
        // Retain the saved choice when it is absent from the current catalog.
        if (slug != null && selected == null) {
            add(CoffeePeekSelectOption("saved:$slug", label, selected = true) { })
        }
        drinks.distinctBy { it.slug }.forEach { drink ->
            val drinkName = if (language == "en") drink.nameEn.ifBlank { drink.nameRu }
                else drink.nameRu.ifBlank { drink.nameEn }
            add(CoffeePeekSelectOption("drink:${drink.slug}", drinkName, selected = drink.slug == slug) {
                if (drink.slug != slug) onChange(drink.slug, if (drink.slug == "other") "" else null)
            })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        val title = stringResource(Res.string.select_drink_label)
        Text(title, style = MaterialTheme.typography.labelMedium)
        CoffeePeekSelectField(label, menuItems, title, modifier = Modifier.fillMaxWidth())
        if (error != null) {
            Text(stringResource(Res.string.select_drink_error), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text(stringResource(Res.string.select_retry)) }
        }
        if (slug == "other") {
            ReviewTextInput(value = customName.orEmpty(), onValueChange = { onChange(slug, it.take(100)) },
                placeholder = stringResource(Res.string.select_drink_custom), singleLine = true, maxLength = 100)
            consumedDrinkIssue(slug, customName)?.let { Text(it.message(consumedDrinkValidationLabels()), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ConsumedDrinkFieldPreview(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface { Column(Modifier.padding(16.dp)) {
        ConsumedDrinkField(listOf(ConsumedDrinkOption("filter", "Фильтр", "Filter")), "filter", null, null, null, {}, { _, _ -> })
    } }
}
@Preview @Composable private fun ConsumedDrinkFieldLightPreview() = ConsumedDrinkFieldPreview(false)
@Preview @Composable private fun ConsumedDrinkFieldDarkPreview() = ConsumedDrinkFieldPreview(true)
