package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_login
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val gold = CoffeePeekColors.Primary
    val onGold = CoffeePeekColors.DarkTextOnPrimary
    CoffeePeekFilledButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = onGold,
            disabledContainerColor = gold.copy(alpha = if (loading) 1f else 0.5f),
            disabledContentColor = onGold.copy(alpha = if (loading) 1f else 0.7f)),
    ) {
        if (loading) CoffeePeekLoader(size = 20.dp, color = onGold)
        else Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Preview
@Composable
private fun AuthPrimaryLightPreview() = CoffeePeekTheme(darkTheme = false) {
    AuthPrimaryButton(stringResource(Res.string.auth_login), {})
}

@Preview
@Composable
private fun AuthPrimaryDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    AuthPrimaryButton(stringResource(Res.string.auth_login), {}, loading = true)
}
