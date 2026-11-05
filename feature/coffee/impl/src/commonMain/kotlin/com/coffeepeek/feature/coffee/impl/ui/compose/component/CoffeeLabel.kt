package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.background
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun CoffeeLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing1))
}

@Composable private fun CoffeeLabelPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeeLabel("Ягоды") }
}
@Preview @Composable private fun CoffeeLabelLightPreview() = CoffeeLabelPreview(false)
@Preview @Composable private fun CoffeeLabelDarkPreview() = CoffeeLabelPreview(true)
