package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

data class CoffeePeekSelectOption(
    val id: String,
    val label: String,
    val selected: Boolean = false,
    val leadingIcon: @Composable (() -> Unit)? = null,
    val onSelect: () -> Unit,
)

/** Single-choice field. Callers own selection, localized labels and validation messages. */
@Composable
fun CoffeePeekSelectField(
    value: String,
    options: List<CoffeePeekSelectOption>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPlaceholder: Boolean = false,
    errorDescription: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    coffeePeekValidateSelectOptions(options)
    val interactive = enabled && options.isNotEmpty()
    CoffeePeekSelectMenu(options, contentDescription, value, errorDescription, modifier, interactive) { openMenu ->
        OutlinedButton(
            onClick = openMenu,
            enabled = interactive,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                this.contentDescription = contentDescription
                stateDescription = value
                errorDescription?.let { error(it) }
            },
            shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
            border = BorderStroke(1.dp, if (errorDescription != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.buttonPaddingV),
        ) {
            leadingIcon?.invoke()
            Text(
                value,
                modifier = Modifier.weight(1f).padding(end = CoffeePeekDimens.spacing2),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                color = if (isPlaceholder && interactive) MaterialTheme.colorScheme.onSurfaceVariant else androidx.compose.ui.graphics.Color.Unspecified,
            )
            Icon(CoffeePeekIcons.ChevronUpDown, null, Modifier.size(18.dp))
        }
    }
}

internal fun coffeePeekValidateSelectOptions(options: List<CoffeePeekSelectOption>) {
    require(options.map { it.id }.distinct().size == options.size) { "Select option IDs must be unique" }
    require(options.count { it.selected } <= 1) { "A select field accepts at most one selected option" }
}

@Composable
internal expect fun CoffeePeekSelectMenu(
    options: List<CoffeePeekSelectOption>,
    contentDescription: String,
    valueDescription: String,
    errorDescription: String?,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable (openMenu: () -> Unit) -> Unit,
)

@Composable
private fun CoffeePeekSelectFieldPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    var selected by remember { mutableStateOf("Espresso") }
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val options = listOf("Espresso", "Filter", "Cappuccino").map { label ->
                CoffeePeekSelectOption(label, label, label == selected) { selected = label }
            }
            CoffeePeekSelectField(selected, options, "Drink")
            CoffeePeekSelectField("Choose a city", emptyList(), "City", isPlaceholder = true)
            CoffeePeekSelectField("Choose a city", options.map { it.copy(selected = false) }, "City",
                isPlaceholder = true, errorDescription = "Choose a city")
        }
    }
}

@Preview @Composable private fun CoffeePeekSelectFieldLightPreview() = CoffeePeekSelectFieldPreviewContent(false)
@Preview @Composable private fun CoffeePeekSelectFieldDarkPreview() = CoffeePeekSelectFieldPreviewContent(true)
