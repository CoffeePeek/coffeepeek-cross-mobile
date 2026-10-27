package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Bare shelves with an invitation: the empty shelf and the guest's view share it. */
@Composable
internal fun ShelfInvitation(title: String, body: String, buttonLabel: String, onButton: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        ShelfEmptyShelves()
        Spacer(Modifier.height(CoffeePeekDimens.spacing4))
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(CoffeePeekDimens.spacing3))
        CoffeePeekButton(buttonLabel, onButton)
    }
}

@Composable
private fun ShelfInvitationPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfInvitation("Здесь будет ваш кофе", "Пачки встанут на полку, а когда кофе закончится, уйдут в архив",
        "Посмотреть каталог", {}, Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4))
}

@Preview @Composable private fun ShelfInvitationLightPreview() = ShelfInvitationPreview(false)
@Preview @Composable private fun ShelfInvitationDarkPreview() = ShelfInvitationPreview(true)
