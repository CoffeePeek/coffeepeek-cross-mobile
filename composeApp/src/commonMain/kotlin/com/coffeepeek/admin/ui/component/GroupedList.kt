package com.coffeepeek.admin.ui.component

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.domain.model.CatalogItem

// iOS inset grouped list (Settings-style): footnote header, rounded card, 44dp rows, inset hairlines.

private val GroupShape = RoundedCornerShape(12.dp)
private val RowMinHeight = 44.dp
private val RowInset = 16.dp

@Composable
fun GroupSection(
    title: String? = null,
    trailing: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        if (title != null || trailing != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RowInset)
                    .padding(bottom = 6.dp),
            ) {
                if (title != null) {
                    Text(
                        text = title.uppercase(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (trailing != null) {
                    Text(text = trailing, fontSize = 13.sp, color = CpColor.Primary)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GroupShape)
                .background(MaterialTheme.colorScheme.surface),
            content = content,
        )
        if (footer != null) {
            Text(
                text = footer,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = RowInset).padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun CheckmarkRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .heightIn(min = RowMinHeight)
            .padding(horizontal = RowInset, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Text(
            text = label,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (checked) {
            Icon(
                imageVector = CpIcons.Check,
                contentDescription = null,
                tint = CpColor.Primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Accent-coloured action row, e.g. «Показать все» or «Добавить…». */
@Composable
fun ActionRow(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = RowMinHeight)
            .padding(horizontal = RowInset, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = CpColor.Primary, modifier = Modifier.size(20.dp))
        }
        Text(text = label, fontSize = 17.sp, color = CpColor.Primary)
    }
}

/** iOS-style navigation row: the whole 44dp+ row opens the next screen. */
@Composable
fun NavigationRow(
    label: String,
    onClick: () -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = RowMinHeight)
            .padding(horizontal = RowInset, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            if (description != null) {
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = CpIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun RowSeparator() {
    HorizontalDivider(
        modifier = Modifier.padding(start = RowInset),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    )
}

/**
 * Multi-select catalog section. Collapses to [collapsedCount] rows (selected items always stay visible);
 * [extraRows] go at the bottom of the card, e.g. an «Добавить» action.
 */
@Composable
fun CheckmarkSection(
    title: String,
    items: List<CatalogItem>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    collapsedCount: Int = 5,
    footer: String? = null,
    leading: (@Composable (CatalogItem) -> Unit)? = null,
    extraRows: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val localizedLegacyGroupedListDiscoveryFiltersCollapse = stringResource(Res.string.legacy_grouped_list_discovery_filters_collapse)

    if (items.isEmpty() && extraRows == null) return

    var expanded by remember { mutableStateOf(false) }
    val collapsible = items.size > collapsedCount
    val visibleItems = if (expanded || !collapsible) items
    else (items.take(collapsedCount) + items.filter { it.id in selectedIds }).distinctBy { it.id }
    val selectedCount = items.count { it.id in selectedIds }

    GroupSection(
        title = title,
        trailing = selectedCount.takeIf { it > 0 }?.let { stringResource(Res.string.legacy_selected_count, it) },
        footer = footer,
    ) {
        visibleItems.forEachIndexed { index, item ->
            if (index > 0) RowSeparator()
            CheckmarkRow(
                label = item.name,
                checked = item.id in selectedIds,
                onToggle = { onToggle(item.id) },
                leading = leading?.let { { it(item) } },
            )
        }
        if (collapsible) {
            RowSeparator()
            ActionRow(
                label = if (expanded) localizedLegacyGroupedListDiscoveryFiltersCollapse else stringResource(Res.string.legacy_show_all_count, items.size),
                onClick = { expanded = !expanded },
            )
        }
        if (extraRows != null) {
            if (visibleItems.isNotEmpty()) RowSeparator()
            extraRows()
        }
    }
}

@Composable
fun RoasterAvatar(item: CatalogItem) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape),
    ) {
        val photoUrl = item.photoUrl?.takeIf(String::isNotBlank)
        if (photoUrl != null) {
            CoffeeShopImage(
                imageUrl = photoUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                placeholderLabelSize = 6.sp,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CoffeeShopPlaceholderImage(
                labelSize = 6.sp,
                contentDescription = item.name,
            )
        }
    }
}

/** The whole row is the toggle target; PlatformSwitch owns thumb visibility in both themes. */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = RowMinHeight)
            .padding(horizontal = RowInset, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        PlatformSwitch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.clearAndSetSemantics { })
    }
}

/** Label + value + iOS UIStepper (− | +). */
@Composable
fun StepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String = stringResource(Res.string.legacy_grouped_list_decrease),
    increaseDescription: String = stringResource(Res.string.legacy_grouped_list_increase),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(horizontal = RowInset, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperHalf("−", decreaseDescription, onDecrease)
            VerticalDivider(
                modifier = Modifier.height(18.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            )
            StepperHalf("+", increaseDescription, onIncrease)
        }
    }
}

@Composable
private fun StepperHalf(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(47.dp)
            .height(32.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
