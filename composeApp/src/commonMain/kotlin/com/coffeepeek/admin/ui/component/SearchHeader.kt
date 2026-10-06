package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
internal fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    roastersSelected: Boolean,
    onSelectRoasters: (Boolean) -> Unit,
    filterCount: Int = 0,
    onFilters: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CpSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = if (roastersSelected) "Поиск обжарщиков…" else "Поиск кофейни…",
            modifier = Modifier.weight(1f),
            fieldHeight = CpDimens.buttonHeight,
        )
        if (onFilters != null) BadgedBox(badge = {
            if (filterCount > 0) Box(
                Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(filterCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }) {
            OutlinedIconButton(
                onClick = onFilters, modifier = Modifier.size(CpDimens.buttonHeight), shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Icon(CpIcons.Filter, "Фильтры", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
    Spacer(Modifier.height(CpDimens.spacing2))
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup().clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp),
    ) {
        listOf(false, true).forEach { roasters ->
            val selected = roastersSelected == roasters
            val shape = RoundedCornerShape(percent = 50)
            Row(
                modifier = Modifier.weight(1f).height(40.dp).clip(shape)
                    .background(if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelectRoasters(roasters) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Icon(
                    if (roasters) CpIcons.Factory else CpIcons.Coffee, null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(if (roasters) "Обжарщики" else "Кофейни", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
