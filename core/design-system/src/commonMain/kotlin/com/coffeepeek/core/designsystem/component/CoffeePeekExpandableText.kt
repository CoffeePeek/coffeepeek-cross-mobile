package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Controlled expansion, with the affordance shown only when the text overflows. */
@Composable
fun CoffeePeekExpandableText(
    text: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    expandLabel: String,
    collapseLabel: String,
    modifier: Modifier = Modifier,
    collapsedLines: Int = 4,
) {
    require(collapsedLines > 0)
    var overflows by remember(text, collapsedLines) { mutableStateOf(false) }
    val actionColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        CoffeePeekColors.AccentTextDark
    } else {
        CoffeePeekColors.AccentTextLight
    }

    Column(modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (overflows || expanded) {
            TextButton(
                onClick = onToggle,
                colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
            ) {
                Text(if (expanded) collapseLabel else expandLabel)
            }
        }
    }
}

@Composable
private fun CoffeePeekExpandableTextPreviewContent(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    var expanded by remember { mutableStateOf(false) }
    Surface {
        CoffeePeekExpandableText("CoffeePeek. ".repeat(60), expanded, { expanded = !expanded }, "Read more", "Collapse")
    }
}

@Preview @Composable private fun CoffeePeekExpandableTextLightPreview() = CoffeePeekExpandableTextPreviewContent(false)
@Preview @Composable private fun CoffeePeekExpandableTextDarkPreview() = CoffeePeekExpandableTextPreviewContent(true)
