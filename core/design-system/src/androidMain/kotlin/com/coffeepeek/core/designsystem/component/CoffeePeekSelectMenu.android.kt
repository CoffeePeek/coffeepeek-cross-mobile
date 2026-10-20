package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens

@Composable
internal actual fun CoffeePeekSelectMenu(
    options: List<CoffeePeekSelectOption>, contentDescription: String, valueDescription: String,
    errorDescription: String?, modifier: Modifier, enabled: Boolean,
    content: @Composable (openMenu: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) expanded = false }
    Box(modifier) {
        content { if (enabled) expanded = true }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 224.dp, max = 320.dp).heightIn(max = 360.dp),
            shape = RoundedCornerShape(CoffeePeekDimens.selectRadius),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { expanded = false; option.onSelect() },
                    modifier = Modifier.semantics { selected = option.selected },
                    leadingIcon = option.leadingIcon,
                    trailingIcon = {
                        Box(Modifier.size(20.dp)) {
                            if (option.selected) Icon(CoffeePeekIcons.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                )
            }
        }
    }
}
