package com.coffeepeek.admin.ui.screen.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.PlatformMenuButton
import com.coffeepeek.admin.ui.component.PlatformMenuItem
import com.coffeepeek.admin.ui.component.ReviewTextInput
import com.coffeepeek.domain.model.ConsumedDrinkOption
import com.coffeepeek.domain.model.savedDrinkName
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
    savedNameEn: String? = null,
) {
    val language = Locale.current.language
    val selected = drinks.find { it.slug == slug }
    val selectedName = selected?.let { drink ->
        if (language == "en") drink.nameEn.ifBlank { drink.nameRu }
        else drink.nameRu.ifBlank { drink.nameEn }
    }
    val label = when (slug) {
        null -> "Не выбран"
        "other" -> customName?.takeIf { it.isNotBlank() }
            ?: selectedName ?: if (language == "en") "Other" else "Другое"
        else -> savedDrinkName(savedName, savedNameEn, null, language) ?: selectedName ?: slug
    }
    val menuItems = buildList {
        add(PlatformMenuItem("none", "Не выбран", selected = slug == null) { onChange(null, null) })
        // Retain the saved choice when it is absent from the current catalog.
        if (slug != null && selected == null) {
            add(PlatformMenuItem("saved:$slug", label, selected = true) { })
        }
        drinks.forEach { drink ->
            val drinkName = if (language == "en") drink.nameEn.ifBlank { drink.nameRu }
                else drink.nameRu.ifBlank { drink.nameEn }
            add(PlatformMenuItem(drink.slug, drinkName, selected = drink.slug == slug) {
                if (drink.slug != slug) onChange(drink.slug, if (drink.slug == "other") "" else null)
            })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        Text("Напиток", style = MaterialTheme.typography.labelMedium)
        PlatformMenuButton(
            items = menuItems,
            contentDescription = "Напиток",
            valueDescription = label,
            modifier = Modifier.fillMaxWidth(),
        ) { openMenu ->
            OutlinedButton(
                onClick = openMenu,
                modifier = Modifier.fillMaxWidth().heightIn(min = CpDimens.controlHeight).semantics { stateDescription = label },
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = CpDimens.spacing4, vertical = CpDimens.buttonPaddingV),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                Spacer(Modifier.width(8.dp))
                Icon(CpIcons.ChevronUpDown, contentDescription = null, modifier = Modifier.size(18.dp))
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
