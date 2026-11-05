package com.coffeepeek.feature.authorblocking.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.authorblocking.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun BlockedAuthorsGuestCard(onLogin: () -> Unit, onRegister: () -> Unit) {
    CoffeePeekCard(shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = 0.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            Text(stringResource(Res.string.author_blocking_guest_message), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            CoffeePeekButton(stringResource(Res.string.author_blocking_login), onLogin)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(stringResource(Res.string.author_blocking_no_account), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRegister) {
                    Text(stringResource(Res.string.author_blocking_register), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Preview @Composable private fun BlockedAuthorsGuestCardLightPreview() = CoffeePeekTheme(darkTheme = false) { BlockedAuthorsGuestCard({}, {}) }
@Preview @Composable private fun BlockedAuthorsGuestCardDarkPreview() = CoffeePeekTheme(darkTheme = true) { BlockedAuthorsGuestCard({}, {}) }
