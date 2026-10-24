package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_choose_method
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_method_action
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_omit
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInBrewMethod

@Composable
internal fun HomeCheckInEditorMethod(
    selected: String?, methods: List<HomeCheckInBrewMethod>, enabled: Boolean,
    error: String?, formatMethod: (String) -> String,
    artwork: @Composable (String, Modifier) -> Unit, onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMethod = methods.firstOrNull { it.code == selected || selected in it.legacyCodes }
    val label = selectedMethod?.name ?: selected?.takeIf { error == null }?.let(formatMethod)
    Box {
        HomeCheckInEditorSelect(label ?: stringResource(Res.string.home_check_in_choose_method), CoffeePeekIcons.Coffee,
            stringResource(Res.string.home_check_in_method_action), enabled, error,
            artwork = if (label == null) null else { modifier -> artwork(label, modifier) }) { expanded = true }
        DropdownMenu(expanded && enabled, { expanded = false }, modifier = Modifier.widthIn(min = 240.dp, max = 320.dp)
            .heightIn(max = 360.dp), shape = RoundedCornerShape(12.dp), containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp) {
            DropdownMenuItem(text = { Text(stringResource(Res.string.home_check_in_omit)) },
                onClick = { expanded = false; onSelect(null) })
            methods.forEach { method ->
                DropdownMenuItem(text = { Text(method.name, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { expanded = false; onSelect(method.code) },
                    leadingIcon = { artwork(method.name, Modifier.size(24.dp)) },
                    trailingIcon = { if (selectedMethod == method) Icon(CoffeePeekIcons.Check, null, Modifier.size(18.dp)) })
            }
        }
    }
}

@Preview @Composable private fun HomeCheckInEditorMethodPreview() = CoffeePeekTheme {
    Surface { HomeCheckInEditorMethod("hario-v60", listOf(HomeCheckInBrewMethod("hario-v60", "Hario V60")),
        true, null, { it }, { _, modifier -> Icon(CoffeePeekIcons.Coffee, null, modifier) }, {}) }
}
