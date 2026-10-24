package com.coffeepeek.admin.ui.screen.profile

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import com.coffeepeek.admin.ui.component.CpTopBar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.ThemeMode
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.SettingsIconBadge
import com.coffeepeek.admin.ui.component.SettingsIconColors
import com.coffeepeek.admin.ui.component.SettingsIconPalette
import com.coffeepeek.admin.ui.icons.CpIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen() {
    val selectedTheme by ThemeManager.themeMode.collectAsState()

    Scaffold(
        topBar = {
            CpTopBar("Тема")
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = CpDimens.spacing4),
        ) {
            Spacer(Modifier.height(CpDimens.spacing4))
            Text(
                text = "Внешний вид",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(CpDimens.spacing2))
            Text(
                text = "Выберите, как CoffeePeek будет выглядеть на вашем устройстве.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(CpDimens.spacing6))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CpDimens.cardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    ThemeOption(
                        mode = mode,
                        selected = mode == selectedTheme,
                        onClick = { ThemeManager.setTheme(mode) },
                    )
                    if (index != ThemeMode.entries.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = CpDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(
            icon = mode.icon(),
            colors = mode.iconColors(),
        )
        Spacer(Modifier.size(CpDimens.spacing3))
        Text(
            text = mode.title(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = CpIcons.Check,
                contentDescription = "Выбрано",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(CpDimens.settingsIconSize),
            )
        }
    }
}

private fun ThemeMode.title(): String = when (this) {
    ThemeMode.SYSTEM -> "Системная"
    ThemeMode.LIGHT -> "Светлая"
    ThemeMode.DARK -> "Тёмная"
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.SYSTEM -> CpIcons.ThemeSystem
    ThemeMode.LIGHT -> CpIcons.ThemeLight
    ThemeMode.DARK -> CpIcons.ThemeDark
}

private fun ThemeMode.iconColors(): SettingsIconColors = when (this) {
    ThemeMode.SYSTEM -> SettingsIconPalette.Cyan
    ThemeMode.LIGHT -> SettingsIconPalette.Gold
    ThemeMode.DARK -> SettingsIconPalette.Violet
}
