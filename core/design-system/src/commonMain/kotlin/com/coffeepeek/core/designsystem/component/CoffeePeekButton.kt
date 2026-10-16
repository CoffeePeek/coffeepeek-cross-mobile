package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CoffeePeekDimens.buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor         = MaterialTheme.colorScheme.primary,
            contentColor           = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor   = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
        enabled = enabled,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(CoffeePeekDimens.spacing2))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CoffeePeekButtonPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CoffeePeekButton("Continue", {})
            CoffeePeekButton("Unavailable", {}, enabled = false)
            CoffeePeekButton("Continue", {}, leadingIcon = { Icon(CoffeePeekIcons.Check, contentDescription = null) })
        }
    }
}

@Preview @Composable private fun CoffeePeekButtonLightPreview() = CoffeePeekButtonPreviewContent(false)
@Preview @Composable private fun CoffeePeekButtonDarkPreview() = CoffeePeekButtonPreviewContent(true)
