package com.coffeepeek.admin.ui.screen.checkins

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
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.admin.feature.homecheckin.ui.HomeEditor
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.SettingsIconBadge
import com.coffeepeek.admin.ui.component.SettingsIconPalette
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
internal fun CheckInCreateButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, shape = CircleShape,
        modifier = modifier,
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

/** Feed and profile share one sheet, including the transition into the home form. */
@Composable
internal fun CheckInCreateFlow(
    onDismiss: () -> Unit,
    onShop: () -> Unit,
    onHomeSaved: () -> Unit = {},
    homeVm: HomeCheckInViewModel = platformViewModel(),
) {
    val home by homeVm.state.collectAsState()
    var creatingHome by remember { mutableStateOf(false) }
    LaunchedEffect(creatingHome, home.editing) {
        if (creatingHome && !home.editing) {
            if (home.selected != null) onHomeSaved()
            onDismiss()
        }
    }
    SwipeDismissModalBottomSheet(
        onDismissRequest = { if (creatingHome) homeVm.closeEditor(); onDismiss() },
        dismissEnabled = !home.saving,
    ) {
        if (creatingHome) HomeEditor(home, homeVm, Modifier.fillMaxWidth().heightIn(max = 680.dp))
        else CheckInCreateContent(onShop, onHome = { homeVm.create(); creatingHome = true })
    }
}

@Composable
private fun CheckInCreateContent(onShop: () -> Unit, onHome: () -> Unit) {
    var homeSelected by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = CpDimens.spacing4).padding(bottom = CpDimens.spacing6),
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
