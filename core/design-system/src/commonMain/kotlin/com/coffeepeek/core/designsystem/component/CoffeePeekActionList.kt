package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Direct actions on the containing surface, separated only at logical group boundaries. */
@Composable
fun CoffeePeekActionList(groups: List<List<CoffeePeekMenuAction>>, modifier: Modifier = Modifier) {
    Column(modifier) {
        groups.filter { it.isNotEmpty() }.forEachIndexed { index, actions ->
            if (index > 0) Box(Modifier.fillMaxWidth()
                .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2)
                .height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            actions.forEach { action -> CoffeePeekActionMenuRow(action, action.onClick) }
        }
    }
}

@Composable
internal fun CoffeePeekActionMenuRow(action: CoffeePeekMenuAction, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    val tint = if (action.destructive) colors.error else colors.onSurface
    Row(Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.controlHeight)
        .graphicsLayer { alpha = if (pressed) .55f else 1f }
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
        .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Image(action.icon, null, Modifier.size(CoffeePeekDimens.settingsIconSize), colorFilter = ColorFilter.tint(tint))
        BasicText(action.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge.copy(color = tint))
    }
}

@Composable
private fun CoffeePeekActionListPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekActionList(listOf(
        listOf(CoffeePeekMenuAction("Чекин в кофейне", CoffeePeekIcons.Location) {},
            CoffeePeekMenuAction("Чекин дома", CoffeePeekIcons.Coffee) {}),
        listOf(CoffeePeekMenuAction("Пачку на полку", CoffeePeekIcons.Archive) {}),
    ), Modifier.background(MaterialTheme.colorScheme.surface).padding(vertical = 16.dp))
}

@Preview @Composable private fun CoffeePeekActionListLightPreview() = CoffeePeekActionListPreview(false)
@Preview @Composable private fun CoffeePeekActionListDarkPreview() = CoffeePeekActionListPreview(true)
