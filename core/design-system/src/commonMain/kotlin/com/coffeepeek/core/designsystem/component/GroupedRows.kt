package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.theme.CpColor
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

// Settings-style grouped list: footnote header, rounded card, growing 48dp-minimum rows.

private val GroupShape = RoundedCornerShape(12.dp)
private val RowMinHeight = 48.dp
private val RowInset = 16.dp

@Composable
fun GroupSection(
    title: String,
    trailing: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RowInset)
                .padding(bottom = 6.dp),
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                Text(text = trailing, fontSize = 13.sp, color = CpColor.Primary)
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
            .clickable(role = Role.Button, onClick = onClick)
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

@Composable
fun RowSeparator() {
    HorizontalDivider(
        modifier = Modifier.padding(start = RowInset),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    )
}

/** Row with an iOS-style switch (white full-size thumb, no border); the whole row is the toggle target. */
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
        Switch(
            checked = checked,
            onCheckedChange = null, // row handles the toggle
            // Non-null thumbContent keeps the thumb full-size in both states, like UISwitch.
            thumbContent = { Box(Modifier.size(0.dp)) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CpColor.Primary,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** Label + value + iOS UIStepper (− | +). */
@Composable
fun StepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
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
                .heightIn(min = 48.dp)
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
            .widthIn(min = 48.dp)
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun GroupedRowsPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        Column(Modifier.padding(16.dp)) {
            GroupSection("Options") {
                CheckmarkRow("Selected option", true, {})
                RowSeparator()
                ActionRow("Action", {})
                SwitchRow("Notifications", true, {})
                StepperRow("Quantity", "2", {}, {}, "Decrease", "Increase")
            }
        }
    }
}

@Preview @Composable private fun GroupedRowsLightPreview() = GroupedRowsPreviewContent(false)
@Preview @Composable private fun GroupedRowsDarkPreview() = GroupedRowsPreviewContent(true)

// Keep the crowded settings-row scenario together to inspect large type and RTL.
@Composable
private fun GroupedRowsAdaptivePreviewContent(darkTheme: Boolean, direction: LayoutDirection) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalLayoutDirection provides direction,
        LocalDensity provides Density(density.density, fontScale = 2f)) {
        CoffeePeekTheme(darkTheme = darkTheme) {
            Surface {
                Column(Modifier.width(320.dp).verticalScroll(rememberScrollState()).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${direction.name} · fontScale 2.0 · Manrope")
                    CpTopBar("Длинный заголовок приложения", "Назад", onBack = {})
                    AppButton("Продолжить настройку приложения", {})
                    GroupSection("Параметры") {
                        StepperRow("Количество порций", "12", {}, {}, "Уменьшить", "Увеличить")
                        var checked by remember { mutableStateOf(false) }
                        SwitchRow("Получать уведомления об изменениях", checked, { checked = it })
                        CheckmarkRow("Длинный текст варианта выбора", true, {})
                        ActionRow("Настроить параметры", {})
                    }
                    SettingsRow(CpIcons.Settings, "Настройки оформления приложения",
                        description = "Системная тема", onClick = {})
                    var selection by remember { mutableStateOf("First") }
                    CapsuleSegmentedControl(listOf("First", "Second"), selection, { it }, { selection = it })
                    AppTextField("Название", "Кофе", {}, "Введите название", errorText = "Проверьте значение")
                    CpSearchField("Кофе", {}, "Поиск", "Очистить поиск")
                }
            }
        }
    }
}

@Preview @Composable private fun GroupedRowsLargeFontLtrLightPreview() = GroupedRowsAdaptivePreviewContent(false, LayoutDirection.Ltr)
@Preview @Composable private fun GroupedRowsLargeFontLtrDarkPreview() = GroupedRowsAdaptivePreviewContent(true, LayoutDirection.Ltr)
@Preview @Composable private fun GroupedRowsLargeFontRtlLightPreview() = GroupedRowsAdaptivePreviewContent(false, LayoutDirection.Rtl)
@Preview @Composable private fun GroupedRowsLargeFontRtlDarkPreview() = GroupedRowsAdaptivePreviewContent(true, LayoutDirection.Rtl)
