package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var revealMethod by remember { mutableStateOf(false) }
    LaunchedEffect(hasError, state.saveAttempt) {
        if (hasError) onExpandedChange(true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        HomeCheckInRecipeDisclosure(hasRecipe, visible, enabled) {
            focus.clearFocus()
            keyboard?.hide()
            revealMethod = !visible
            onExpandedChange(!visible)
        }
        if (visible) {
            HomeCheckInRecipeStart(revealMethod, { revealMethod = false }) {
                HomeCheckInRecipeMethod(state, enabled, formatMethod, media, onAction)
            }
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
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.controlHeight)
            .clip(CircleShape)
            .graphicsLayer { alpha = if (!enabled) 0.4f else if (pressed) 0.6f else 1f }
            .semantics { stateDescription = description },
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing2),
        interactionSource = interactions,
    ) {
        Icon(if (expanded) CoffeePeekIcons.ChevronDown else CoffeePeekIcons.ChevronRight,
            contentDescription = null, modifier = Modifier.padding(end = CoffeePeekDimens.spacing2)
                .size(18.dp).scale(scaleX = direction, scaleY = 1f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun HomeCheckInRecipeStart(reveal: Boolean, onRevealHandled: () -> Unit, content: @Composable () -> Unit) {
    val requester = remember { BringIntoViewRequester() }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(placed) {
        if (placed && reveal) {
            onRevealHandled()
            requester.bringIntoView()
        }
    }
    Box(Modifier.bringIntoViewRequester(requester).onGloballyPositioned { placed = true }) { content() }
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
