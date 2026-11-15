package com.coffeepeek.core.designsystem.component

import com.coffeepeek.core.designsystem.theme.reducedMotionDuration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Equal-width text tabs with one underline that slides between tabs. */
@Composable
fun <T> CoffeePeekUnderlineTabs(
    options: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(options.isNotEmpty() && options.distinct().size == options.size && selected in options)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup()) {
            options.forEach { option ->
                CoffeePeekUnderlineTab(label(option), option == selected, { onSelected(option) },
                    Modifier.weight(1f).fillMaxHeight())
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / options.size
            val offset by animateDpAsState(
                targetValue = tabWidth * options.indexOf(selected),
                animationSpec = tween(reducedMotionDuration(200)), label = "tab-underline-offset",
            )
            // The thin full-width line under the labels...
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f), thickness = .5.dp)
            // ...with the single accent underline sliding along it.
            Box(Modifier.offset(x = offset).width(tabWidth).height(2.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
        }
    }
}

@Composable
private fun CoffeePeekUnderlineTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier.heightIn(min = 48.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick)) {
        Text(text, Modifier.align(Alignment.Center).padding(horizontal = 8.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun CoffeePeekUnderlineTabsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        CoffeePeekUnderlineTabs(listOf("First", "Second", "Third"), "First", { it }, {},
            Modifier.padding(CoffeePeekDimens.spacing4))
    }
}

@Preview @Composable private fun CoffeePeekUnderlineTabsLightPreview() = CoffeePeekUnderlineTabsPreview(false)
@Preview @Composable private fun CoffeePeekUnderlineTabsDarkPreview() = CoffeePeekUnderlineTabsPreview(true)
