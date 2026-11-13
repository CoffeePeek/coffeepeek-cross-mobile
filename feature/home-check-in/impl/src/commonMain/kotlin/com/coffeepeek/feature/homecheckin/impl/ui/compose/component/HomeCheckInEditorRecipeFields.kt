package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.ui.text.input.KeyboardType
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField
import com.coffeepeek.feature.homecheckin.impl.resources.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInEditorAction
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInEditorState
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInEditorMedia
import com.coffeepeek.feature.homecheckin.impl.ui.data.editorText
import com.coffeepeek.feature.homecheckin.impl.ui.compose.previewHomeEditorMedia
import com.coffeepeek.feature.homecheckin.impl.ui.compose.previewHomeEditorState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun HomeCheckInEditorRecipeFields(
    state: HomeCheckInEditorState, enabled: Boolean, formatMethod: (String) -> String,
    media: HomeCheckInEditorMedia, onAction: (HomeCheckInEditorAction) -> Unit,
    onOpenBean: () -> Unit,
) {
    val draft = state.draft
    val hasError = state.fieldErrors.keys.any { it in setOf(HomePreparationField.Method,
        HomePreparationField.Bean, HomePreparationField.Dose, HomePreparationField.Duration) }
    var expanded by rememberSaveable(state.entry?.id, state.isReady) {
        mutableStateOf(draft.brewMethodCode != null || draft.shelfBagId != null || draft.customBeanName != null ||
            draft.doseGrams != null || draft.brewDurationSeconds != null)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded || hasError) Res.string.home_check_in_recipe_collapse
                else Res.string.home_check_in_recipe_expand))
        }
        if (expanded || hasError) {
            HomeCheckInRecipeMethod(state, enabled, formatMethod, media, onAction)
            HomeCheckInRecipeBean(state, enabled, media, onAction, onOpenBean)
            HomeCheckInRecipeAmounts(state, enabled, onAction)
        }
    }
}

@Composable
private fun HomeCheckInRecipeMethod(state: HomeCheckInEditorState, enabled: Boolean,
    formatMethod: (String) -> String, media: HomeCheckInEditorMedia, onAction: (HomeCheckInEditorAction) -> Unit) {
    val error = state.fieldErrors[HomePreparationField.Method]?.editorText()
    HomeCheckInEditorField(stringResource(Res.string.home_check_in_brew_method), error) {
        HomeCheckInEditorMethod(state.draft.brewMethodCode, state.methods, enabled && !state.methodsLoading,
            error, formatMethod, media.methodArtwork, { onAction(HomeCheckInEditorAction.MethodChanged(it)) })
        if (state.methodsLoading) Text(stringResource(Res.string.home_check_in_methods_loading))
        if (state.methodsIssue != null) TextButton(onClick = { onAction(HomeCheckInEditorAction.RetryMethods) },
            enabled = enabled) { Text(stringResource(Res.string.home_check_in_methods_retry)) }
    }
}

@Composable
private fun HomeCheckInRecipeBean(state: HomeCheckInEditorState, enabled: Boolean,
    media: HomeCheckInEditorMedia, onAction: (HomeCheckInEditorAction) -> Unit, onOpenBean: () -> Unit) {
    val draft = state.draft
    val error = state.fieldErrors[HomePreparationField.Bean]?.editorText()
    HomeCheckInEditorField(stringResource(Res.string.home_check_in_bean), error) {
        HomeCheckInEditorSelect(draft.customBeanName ?: state.beanSelectionName ?:
            stringResource(Res.string.home_check_in_choose_bean), CoffeePeekIcons.CoffeeBean,
            stringResource(Res.string.home_check_in_choose_bean), enabled, error, onClick = onOpenBean)
        state.shelfBagCardUrl?.takeIf { draft.shelfBagId != null }?.let { card ->
            HomeCheckInEditorShelfBag(card, draft.showShelfBag, enabled, media.shelfBagImage) {
                onAction(HomeCheckInEditorAction.ShowShelfBagChanged(it))
            }
        }
    }
}

@Composable
private fun HomeCheckInRecipeAmounts(state: HomeCheckInEditorState, enabled: Boolean,
    onAction: (HomeCheckInEditorAction) -> Unit) {
    val doseError = state.fieldErrors[HomePreparationField.Dose]?.editorText()
    HomeCheckInEditorField(stringResource(Res.string.home_check_in_dose_label), doseError) {
        HomeCheckInEditorInput(state.draft.doseGrams.orEmpty(), { onAction(HomeCheckInEditorAction.DoseChanged(it)) },
            stringResource(Res.string.home_check_in_dose_description), stringResource(Res.string.home_check_in_dose_hint),
            enabled, doseError, keyboardType = KeyboardType.Decimal, maxLength = 8)
    }
    HomeCheckInEditorDuration(state.draft.brewDurationSeconds, enabled,
        state.fieldErrors[HomePreparationField.Duration]?.editorText()) {
        onAction(HomeCheckInEditorAction.DurationChanged(it))
    }
}

@Composable private fun HomeEditorRecipePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { Column(Modifier.padding(16.dp)) {
        HomeCheckInEditorRecipeFields(previewHomeEditorState(), true, { it }, previewHomeEditorMedia(), {}, {})
    } }
}

@Preview @Composable private fun HomeEditorRecipeLightPreview() = HomeEditorRecipePreview(false)
@Preview @Composable private fun HomeEditorRecipeDarkPreview() = HomeEditorRecipePreview(true)
