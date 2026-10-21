package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Declarative card builder: full-bleed header, padded body and optional full-bleed
 * footer. Slots own their content/spacing; this shell knows no feature models.
 * A null onClick creates a genuinely read-only card, not a disabled button.
 */
@Composable
fun CoffeePeekCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(CoffeePeekDimens.radius4xl),
    border: BorderStroke? = null,
    elevation: Dp = 2.dp,
    contentPadding: PaddingValues = PaddingValues(CoffeePeekDimens.spacing4),
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable ColumnScope.() -> Unit = {
        header?.invoke()
        Column(Modifier.fillMaxWidth().padding(contentPadding), content = content)
        footer?.invoke()
    }
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val cardElevation = CardDefaults.cardElevation(defaultElevation = elevation)

    if (onClick == null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = cardElevation,
            border = border,
            content = body,
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = cardElevation,
            border = border,
            content = body,
        )
    }
}

@Composable
private fun CoffeePeekCardPreviewContent(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekCard(
        header = { CoffeePeekImagePlaceholder(Modifier.height(140.dp)) },
        footer = { CoffeePeekButton("Continue", {}, Modifier.padding(16.dp)) },
    ) {
        Text("CoffeePeek", style = MaterialTheme.typography.titleLarge)
    }
}

@Preview @Composable private fun CoffeePeekCardLightPreview() = CoffeePeekCardPreviewContent(false)
@Preview @Composable private fun CoffeePeekCardDarkPreview() = CoffeePeekCardPreviewContent(true)
