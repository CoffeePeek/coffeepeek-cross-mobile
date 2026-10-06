package com.coffeepeek.admin.ui.screen.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.coffeepeek.admin.ui.component.ReviewTextInput
import com.coffeepeek.domain.model.ConsumedDrinkOption
import com.coffeepeek.domain.model.validateConsumedDrink

@Composable
internal fun ConsumedDrinkField(
    drinks: List<ConsumedDrinkOption>,
    slug: String?,
    customName: String?,
    savedName: String?,
    error: String?,
    onRetry: () -> Unit,
    onChange: (String?, String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = drinks.find { it.slug == slug }
    val label = savedName ?: selected?.nameRu?.ifBlank { selected.nameEn } ?: slug ?: "Не выбран"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Напиток", style = MaterialTheme.typography.labelMedium)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { stateDescription = label },
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                Spacer(Modifier.width(8.dp))
                Icon(CpIcons.ChevronUpDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 220.dp, max = 320.dp).heightIn(max = 320.dp),
                shape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                DrinkMenuItem("Не выбран", slug == null) { onChange(null, null); expanded = false }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                if (slug != null && selected == null) {
                    DrinkMenuItem(label, true) { expanded = false }
                }
                drinks.forEach { drink ->
                    DrinkMenuItem(drink.nameRu.ifBlank { drink.nameEn }, drink.slug == slug) {
                        if (drink.slug != slug) {
                            onChange(drink.slug, if (drink.slug == "other") "" else null)
                        }
                        expanded = false
                    }
                }
            }
        }
        if (error != null) {
            Text("Не удалось загрузить напитки", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Повторить") }
        }
        if (slug == "other") {
            ReviewTextInput(value = customName.orEmpty(), onValueChange = { onChange(slug, it.take(100)) },
                placeholder = "Название напитка", singleLine = true, maxLength = 100)
            validateConsumedDrink(slug, customName)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun DrinkMenuItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        modifier = Modifier.heightIn(min = 44.dp).semantics { selected = isSelected },
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.onSurface,
            leadingIconColor = MaterialTheme.colorScheme.onSurface,
        ),
        leadingIcon = {
            if (isSelected) {
                Icon(CpIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            } else {
                Box(Modifier.size(18.dp))
            }
        },
    )
}
