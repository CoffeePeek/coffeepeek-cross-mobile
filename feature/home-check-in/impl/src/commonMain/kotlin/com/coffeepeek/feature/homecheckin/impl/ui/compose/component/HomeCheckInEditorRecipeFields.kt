package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.ui.text.input.KeyboardType
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationInput
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
    expanded: Boolean?, onExpandedChange: (Boolean) -> Unit, onOpenBean: () -> Unit,
) {
    val hasRecipe = state.draft.hasRecipe()
    val hasError = state.fieldErrors.keys.any { it in recipeFields }
    val visible = expanded ?: (hasRecipe || hasError)
    LaunchedEffect(hasError, state.saveAttempt) {
        if (hasError) onExpandedChange(true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        HomeCheckInRecipeDisclosure(hasRecipe, visible, enabled) { onExpandedChange(!visible) }
        if (visible) {
            HomeCheckInRecipeMethod(state, enabled, formatMethod, media, onAction)
            HomeCheckInRecipeBean(state, enabled, media, onAction, onOpenBean)
            HomeCheckInRecipeAmounts(state, enabled, onAction)
        }
    }
}

private val recipeFields = setOf(
    HomePreparationField.Method, HomePreparationField.Bean, HomePreparationField.Dose, HomePreparationField.Duration,
)

private fun HomePreparationInput.hasRecipe(): Boolean =
    brewMethodCode != null || coffeeProductSlug != null || coffeeProductId != null || shelfBagId != null ||
        customBeanName != null || doseGrams != null || brewDurationSeconds != null

@Composable
private fun HomeCheckInRecipeDisclosure(hasRecipe: Boolean, expanded: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (hasRecipe) Res.string.home_check_in_recipe_title else Res.string.home_check_in_recipe_expand)
    val description = stringResource(if (expanded) Res.string.home_check_in_recipe_expanded
        else Res.string.home_check_in_recipe_collapsed)
    val direction = if (!expanded && LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    Row(
        Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.controlHeight)
            .semantics { stateDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
    ) {
        Icon(if (expanded) CoffeePeekIcons.ChevronDown else CoffeePeekIcons.ChevronRight,
            contentDescription = null, modifier = Modifier.size(18.dp).scale(scaleX = direction, scaleY = 1f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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

@Composable private fun HomeEditorRecipePreview(dark: Boolean, expanded: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { Column(Modifier.padding(16.dp)) {
        HomeCheckInEditorRecipeFields(previewHomeEditorState(), true, { it }, previewHomeEditorMedia(), {},
            expanded, {}, {})
    } }
}

@Preview @Composable private fun HomeEditorRecipeLightPreview() = HomeEditorRecipePreview(false, false)
@Preview @Composable private fun HomeEditorRecipeDarkPreview() = HomeEditorRecipePreview(true, false)
@Preview @Composable private fun HomeEditorRecipeExpandedLightPreview() = HomeEditorRecipePreview(false, true)
@Preview @Composable private fun HomeEditorRecipeExpandedDarkPreview() = HomeEditorRecipePreview(true, true)
