package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekChipOption
import com.coffeepeek.core.designsystem.component.CoffeePeekChoiceChips
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationInput
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_drink
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_drink_other
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_drinks_empty
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_drinks_error
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_retry
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInEditorAction
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInEditorState
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInDrinkOption
import com.coffeepeek.feature.homecheckin.impl.ui.data.editorText
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun HomeCheckInEditorDrinkField(
    state: HomeCheckInEditorState, enabled: Boolean, onAction: (HomeCheckInEditorAction) -> Unit,
) {
    val language = LocalCoffeePeekLanguageTag.current
    val items = state.drinks.map { CoffeePeekChipOption(it.slug, it.displayName(language)) }
    val saved = state.draft.drinkSlug
    val legacyLabel = stringResource(Res.string.home_check_in_drink_other)
    val legacy = saved?.takeIf { id -> items.none { it.id == id } }?.let { id ->
        val name = state.draft.customDrinkName?.takeIf(String::isNotBlank)
            ?: state.entry?.drinkName?.takeIf { state.entry.drinkSlug == id && it.isNotBlank() }
        CoffeePeekChipOption(id, name ?: if (id == "other") legacyLabel else id)
    }
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(stringResource(Res.string.home_check_in_drink), style = MaterialTheme.typography.titleMedium)
        CoffeePeekChoiceChips(listOfNotNull(legacy) + items, saved, { onAction(HomeCheckInEditorAction.DrinkSelected(it)) }, enabled)
        when {
            state.drinksLoading -> CoffeePeekLoader(size = 24.dp)
            state.drinksFailed -> {
                Text(stringResource(Res.string.home_check_in_drinks_error), color = MaterialTheme.colorScheme.error)
                CoffeePeekTextButton(onClick = { onAction(HomeCheckInEditorAction.RetryDrinks) }, enabled = enabled) {
                    Text(stringResource(Res.string.home_check_in_retry))
                }
            }
            items.isEmpty() -> Text(stringResource(Res.string.home_check_in_drinks_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.fieldErrors[HomePreparationField.Drink]?.editorText()?.let { HomeCheckInEditorFieldError(it) }
    }
}

@Composable
private fun HomeCheckInDrinkFieldPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HomeCheckInEditorDrinkField(HomeCheckInEditorState(
        draft = HomePreparationInput(preparedAt = "", drinkSlug = "filter"),
        drinks = listOf(HomeCheckInDrinkOption("filter", "Фильтр", "Filter"),
            HomeCheckInDrinkOption("espresso", "Эспрессо", "Espresso")),
    ), true, {})
}

@Preview @Composable private fun HomeCheckInDrinkFieldLightPreview() = HomeCheckInDrinkFieldPreview(false)
@Preview @Composable private fun HomeCheckInDrinkFieldDarkPreview() = HomeCheckInDrinkFieldPreview(true)
