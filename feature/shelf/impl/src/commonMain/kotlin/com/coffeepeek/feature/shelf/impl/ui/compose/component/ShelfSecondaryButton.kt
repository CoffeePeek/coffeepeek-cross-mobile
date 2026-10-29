package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The second way on, under the yellow button: same size, quiet surface and outline. */
@Composable
internal fun ShelfSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick, modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.buttonHeight), enabled = enabled,
        shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ShelfSecondaryButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfSecondaryButton("Выбрать другое фото", {}, Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4))
}

@Preview @Composable private fun ShelfSecondaryButtonLightPreview() = ShelfSecondaryButtonPreview(false)
@Preview @Composable private fun ShelfSecondaryButtonDarkPreview() = ShelfSecondaryButtonPreview(true)
