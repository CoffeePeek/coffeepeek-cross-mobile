package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun RoasterSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Preview @Composable private fun RoasterSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterSection("Об обжарщике") { Text("Локальный обжарщик для тестирования", color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Preview @Composable private fun RoasterSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterSection("Об обжарщике") { Text("Локальный обжарщик для тестирования", color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
