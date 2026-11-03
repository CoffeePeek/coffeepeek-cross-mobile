package com.coffeepeek.admin.ui.component

import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl

@Composable
fun <T> CapsuleSegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) = CoffeePeekCapsuleSegmentedControl(options, selected, label, onSelected, modifier)

@Composable
private fun CapsuleSegmentedControlCompatibilityPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CapsuleSegmentedControl(listOf("First", "Second"), "First", { it }, {})
}

@Preview @Composable private fun CapsuleSegmentedControlCompatibilityLightPreview() = CapsuleSegmentedControlCompatibilityPreview(false)
@Preview @Composable private fun CapsuleSegmentedControlCompatibilityDarkPreview() = CapsuleSegmentedControlCompatibilityPreview(true)
