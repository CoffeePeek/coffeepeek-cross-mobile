package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
internal actual fun PlatformMenuButton(
    items: List<PlatformMenuItem>,
    contentDescription: String,
    modifier: Modifier,
    enabled: Boolean,
    valueDescription: String?,
    content: @Composable (onClick: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        content { if (enabled) expanded = true }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 200.dp, max = 280.dp).heightIn(max = 320.dp),
            offset = DpOffset(0.dp, CpDimens.spacing1),
            shape = RoundedCornerShape(CpDimens.radiusLg),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.title, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { expanded = false; item.onClick() },
                    enabled = item.enabled,
                    modifier = Modifier.heightIn(min = CpDimens.controlHeight).semantics {
                        item.selected?.let { selected = it }
                    },
                    contentPadding = PaddingValues(horizontal = CpDimens.spacing4, vertical = 0.dp),
                    leadingIcon = if (item.selected != null || item.icon != null) {
                        {
                            val icon = when {
                                item.selected == true -> CpIcons.Check
                                item.selected == false -> null
                                item.icon == PlatformMenuIcon.Edit -> CpIcons.Edit
                                item.icon == PlatformMenuIcon.Hide -> CpIcons.VisibilityOff
                                item.icon == PlatformMenuIcon.Report -> CpIcons.Error
                                else -> null
                            }
                            if (icon != null) Icon(icon, null, Modifier.size(20.dp))
                            else Box(Modifier.size(20.dp))
                        }
                    } else null,
                )
            }
        }
    }
}
