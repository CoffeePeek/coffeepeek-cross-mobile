package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** A stable identifier and a label localized by the owning feature. */
@Immutable
data class CoffeePeekChipOption(val id: String, val label: String)

/** Optional single selection: activating the selected chip clears it. Labels wrap without truncation. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoffeePeekChoiceChips(
    options: List<CoffeePeekChipOption>,
    selectedId: String?,
    onSelectionChanged: (String?) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
    ) {
        options.forEach { option ->
            key(option.id) {
                CoffeePeekFilterChip(
                    label = option.label,
                    selected = selectedId == option.id,
                    onClick = { onSelectionChanged(option.id.takeUnless { it == selectedId }) },
                    enabled = enabled,
                    maxLines = Int.MAX_VALUE,
                )
            }
        }
    }
}

@Composable
private fun ChoiceChipsPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    var selected by remember { mutableStateOf<String?>("espresso") }
    val options = listOf(
        CoffeePeekChipOption("espresso", "Эспрессо"),
        CoffeePeekChipOption("filter", "Фильтр"),
        CoffeePeekChipOption("long", "Напиток с очень длинным названием для узкого экрана"),
        CoffeePeekChipOption("other", "Другое"),
    )
    Surface {
        Column(Modifier.width(320.dp).padding(CoffeePeekDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
            CoffeePeekChoiceChips(options, selected, { selected = it }, modifier = Modifier.fillMaxWidth())
            CoffeePeekChoiceChips(options, "long", {}, enabled = false, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview @Composable private fun CoffeePeekChoiceChipsLightPreview() = ChoiceChipsPreviewContent(false)
@Preview @Composable private fun CoffeePeekChoiceChipsDarkPreview() = ChoiceChipsPreviewContent(true)
