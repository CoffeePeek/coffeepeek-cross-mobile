package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.admin.analytics.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.SettingsIconBadge
import com.coffeepeek.admin.ui.component.SettingsIconPalette
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.theme.CoffeePeekTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun CheckInCreateButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val analyticsPage = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
    FloatingActionButton(onClick = onClick, shape = CircleShape,
        modifier = modifier.analyticsExposure("check_in", "global_create", analyticsPage),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary) {
        Icon(CpIcons.Add, "Добавить чекин", Modifier.size(28.dp))
    }
}

@Composable
internal fun CheckInCreateSheet(onDismiss: () -> Unit, onShop: () -> Unit, onHome: () -> Unit) {
    SwipeDismissModalBottomSheet(onDismissRequest = onDismiss) {
        CheckInCreateContent(onShop, onHome)
    }
}

@Composable
internal fun CheckInCreateContent(onShop: () -> Unit, onHome: () -> Unit) {
    val analyticsPage = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
    var homeSelected by rememberSaveable { mutableStateOf(false) }
    PlatformScrollableColumn(
        Modifier.fillMaxWidth().analyticsExposure("home_check_in", "check_in_type_picker", analyticsPage).padding(horizontal = CpDimens.spacing4).padding(bottom = CpDimens.spacing6),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                Text("Новый чекин", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Где вы пили кофе?", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            CreateChoice("В кофейне", "Выбрать место и напиток", CpIcons.Location, !homeSelected) { homeSelected = false }
            CreateChoice("Дома", "Записать приготовление", CpIcons.Coffee, homeSelected) { homeSelected = true }
        }
        AppButton("Продолжить", onClick = { if (homeSelected) onHome() else onShop() })
    }
}

@Composable
private fun CreateChoice(title: String, description: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(CpDimens.cardRadius)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                else MaterialTheme.colorScheme.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 72.dp).padding(CpDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        SettingsIconBadge(icon, SettingsIconPalette.Gold)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(CpIcons.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}

@Preview @Composable private fun CheckInCreatePickerLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInCreateContent({}, {})
}

@Preview @Composable private fun CheckInCreatePickerDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInCreateContent({}, {})
}
