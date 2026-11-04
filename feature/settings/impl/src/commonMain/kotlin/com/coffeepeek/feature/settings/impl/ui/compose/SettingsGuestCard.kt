package com.coffeepeek.feature.settings.impl.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.settings.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The sign-in prompt a guest sees at the top of the Settings tab. */
@Composable
internal fun SettingsGuestCard(onLogin: () -> Unit, onRegister: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
        ) {
            Text(stringResource(Res.string.settings_guest_prompt), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            CoffeePeekFilledButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(CoffeePeekDimens.buttonHeight)) {
                Text(stringResource(Res.string.settings_guest_login), style = MaterialTheme.typography.labelLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(stringResource(Res.string.settings_guest_no_account), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CoffeePeekTextButton(onClick = onRegister) {
                    Text(stringResource(Res.string.settings_guest_register), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun SettingsGuestCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    SettingsGuestCard({}, {}, Modifier.padding(16.dp))
}

@Preview @Composable private fun SettingsGuestCardLightPreview() = SettingsGuestCardPreview(false)
@Preview @Composable private fun SettingsGuestCardDarkPreview() = SettingsGuestCardPreview(true)
