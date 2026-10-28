package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekFilterChip
import com.coffeepeek.core.designsystem.component.CoffeePeekTextField
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.PackModerationReason
import com.coffeepeek.feature.shelf.domain.model.ShelfGrind
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfAddAction
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfAddState
import com.coffeepeek.feature.shelf.impl.ui.compose.previewToday
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val Weights = listOf(200, 250, 500, 1000)

/** The new bag: its card, optional advice from the scanner and a few optional fields; "Заполню позже" saves the card alone. */
@Composable
internal fun ShelfBagFormContent(state: ShelfAddState, onAction: (ShelfAddAction) -> Unit, onRetake: () -> Unit, modifier: Modifier = Modifier) {
    val form = state.form
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        val result = state.result
        ShelfFormCard(result?.card?.bytes, result?.let { it.cardWidth.toFloat() / it.cardHeight.coerceAtLeast(1) }?.takeIf { it > 0f } ?: 0.75f, onRetake)
        if (state.hints.isNotEmpty()) ShelfScanHints(state.hints, onRetake)
        ShelfCoffeeField(form.query, form.coffee, state.options, { onAction(ShelfAddAction.QueryChanged(it)) },
            { onAction(ShelfAddAction.CoffeeChosen(it)) })
        CoffeePeekTextField(stringResource(Res.string.shelf_form_roaster), form.roaster, { onAction(ShelfAddAction.RoasterChanged(it)) }, "")
        ShelfChoiceRow(stringResource(Res.string.shelf_form_weight), Weights, form.weightGrams, { weightLabel(it) }) {
            onAction(ShelfAddAction.WeightChosen(it))
        }
        ShelfChoiceRow(stringResource(Res.string.shelf_form_grind), ShelfGrind.entries, form.grind, { grindLabel(it) }) {
            onAction(ShelfAddAction.GrindChosen(it))
        }
        ShelfRoastDateField(form.roastedOn, state.today, { onAction(ShelfAddAction.RoastDateChosen(it)) })
        CoffeePeekButton(stringResource(Res.string.shelf_form_save), { onAction(ShelfAddAction.Save(later = false)) }, enabled = !state.saving)
        TextButton({ onAction(ShelfAddAction.Save(later = true)) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.saving) {
            Text(stringResource(Res.string.shelf_form_later))
        }
    }
}

@Composable
/** The cut card at its own proportions, like it will stand on the shelf. */
private fun ShelfFormCard(card: ByteArray?, aspect: Float, onRetake: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
        if (card != null) ShelfScanPhoto(card, null, Modifier.width(120.dp).aspectRatio(aspect), ShelfCardShape, elevation = 10.dp)
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.shelf_form_ready), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onRetake) { Text(stringResource(Res.string.shelf_retake_camera)) }
        }
    }
}

/** "Принято с подсказкой": advice, not a block. */
@Composable
private fun ShelfScanHints(hints: List<PackModerationReason>, onRetake: () -> Unit) {
    Surface(shape = RoundedCornerShape(CoffeePeekDimens.cardRadius), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))) {
        Column(Modifier.fillMaxWidth().padding(CoffeePeekDimens.cardPadding), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(stringResource(Res.string.shelf_form_hint), style = MaterialTheme.typography.titleSmall)
            hints.forEach { Text(it.text(), style = MaterialTheme.typography.bodyMedium) }
            TextButton(onRetake) { Text(stringResource(Res.string.shelf_retake_camera)) }
        }
    }
}

/** Single choice that a second tap clears. */
@Composable
private fun <T> ShelfChoiceRow(title: String, options: List<T>, selected: T?, label: @Composable (T) -> String, onSelect: (T?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            options.forEach { option -> CoffeePeekFilterChip(label(option), option == selected, { onSelect(option.takeUnless { it == selected }) }) }
        }
    }
}

@Composable
private fun weightLabel(grams: Int): String =
    if (grams >= 1000 && grams % 1000 == 0) stringResource(Res.string.shelf_weight_kg_short, grams / 1000)
    else stringResource(Res.string.shelf_weight, grams)

@Composable
private fun grindLabel(grind: ShelfGrind): String = stringResource(
    when (grind) {
        ShelfGrind.Beans -> Res.string.shelf_grind_beans
        ShelfGrind.Ground -> Res.string.shelf_grind_ground
    },
)

@Composable
private fun ShelfBagFormPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfBagFormContent(ShelfAddState(previewToday), {}, {}, Modifier.background(MaterialTheme.colorScheme.background))
}

@Preview @Composable private fun ShelfBagFormLightPreview() = ShelfBagFormPreview(false)
@Preview @Composable private fun ShelfBagFormDarkPreview() = ShelfBagFormPreview(true)
