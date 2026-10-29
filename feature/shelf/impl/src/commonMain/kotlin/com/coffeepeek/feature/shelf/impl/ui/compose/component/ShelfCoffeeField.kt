package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekTextField
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfCoffeeOption
import com.coffeepeek.feature.shelf.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The bag's coffee: typing searches the catalogue; a chosen match links the bag, plain text becomes its own name. */
@Composable
internal fun ShelfCoffeeField(
    query: String, coffee: ShelfCoffeeOption?, options: List<ShelfCoffeeOption>,
    onQuery: (String) -> Unit, onChoose: (ShelfCoffeeOption) -> Unit, modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        CoffeePeekTextField(stringResource(Res.string.shelf_form_coffee), query, onQuery, stringResource(Res.string.shelf_form_coffee_hint),
            leadingIcon = CoffeePeekIcons.Search)
        if (coffee != null) ShelfCatalogMatch(coffee)
        options.forEach { option -> ShelfCoffeeOptionRow(option) { onChoose(option) } }
    }
}

/** The linked catalogue coffee, outlined with a dashed line like a label pinned to the bag. */
@Composable
private fun ShelfCatalogMatch(coffee: ShelfCoffeeOption) {
    val line = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().drawBehind {
        val radius = CoffeePeekDimens.radiusMd.toPx()
        drawRoundRect(line, cornerRadius = CornerRadius(radius),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
    }.padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing2),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
        Icon(CoffeePeekIcons.CheckCircle, null, Modifier.size(20.dp), tint = line)
        Column {
            Text("${coffee.roasterName} · ${coffee.name}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(Res.string.shelf_form_catalog_match), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ShelfCoffeeOptionRow(option: ShelfCoffeeOption, onClick: () -> Unit) {
    val shape = RoundedCornerShape(CoffeePeekDimens.radiusMd)
    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
        .clickable(onClick = onClick).padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing2)) {
        Text(option.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(option.roasterName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ShelfCoffeeFieldPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
        ShelfCoffeeField("Эфи", null, listOf(ShelfCoffeeOption("a", "Эфиопия Гуджи", "River Roasters"),
            ShelfCoffeeOption("b", "Эфиопия Сидамо", "Sibaristica")), {}, {})
        ShelfCoffeeField("Кения Ньери", ShelfCoffeeOption("k", "Кения Ньери", "River Roasters"), emptyList(), {}, {})
    }
}

@Preview @Composable private fun ShelfCoffeeFieldLightPreview() = ShelfCoffeeFieldPreview(false)
@Preview @Composable private fun ShelfCoffeeFieldDarkPreview() = ShelfCoffeeFieldPreview(true)
