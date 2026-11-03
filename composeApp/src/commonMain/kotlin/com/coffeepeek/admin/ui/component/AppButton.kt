package com.coffeepeek.admin.ui.component

import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekButton

@Composable
fun AppButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    CoffeePeekButton(text, onClick, modifier, enabled)

@Composable
private fun AppButtonCompatibilityPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    AppButton("Continue", {})
}

@Preview @Composable private fun AppButtonCompatibilityLightPreview() = AppButtonCompatibilityPreview(false)
@Preview @Composable private fun AppButtonCompatibilityDarkPreview() = AppButtonCompatibilityPreview(true)
