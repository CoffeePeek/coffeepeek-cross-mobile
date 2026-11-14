package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekLikeCount(
    count: Int,
    selected: Boolean,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val target = CoffeePeekDimens.minimumTouchTarget
    val actionModifier = if (onClick == null) Modifier.semantics { this.selected = selected }
    else Modifier.toggleable(
        value = selected, enabled = enabled, role = Role.Checkbox,
        interactionSource = null, indication = null, onValueChange = { onClick() },
    )
    Row(
        modifier.sizeIn(minWidth = target, minHeight = target).then(actionModifier)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
    ) {
        CoffeePeekLikeIcon(selected, contentDescription = null)
        Text(count.toString(), Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CoffeePeekLikeCountPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    var selected by remember { mutableStateOf(dark) }
    val count = if (selected) 13 else 12
    CoffeePeekLikeCount(count, selected, "Лайки: $count", onClick = { selected = !selected })
}

@Preview @Composable private fun CoffeePeekLikeCountLightPreview() = CoffeePeekLikeCountPreview(false)
@Preview @Composable private fun CoffeePeekLikeCountDarkPreview() = CoffeePeekLikeCountPreview(true)
