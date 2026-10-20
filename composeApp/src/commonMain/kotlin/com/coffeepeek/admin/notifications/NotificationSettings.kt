package com.coffeepeek.admin.notifications

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.admin.ui.component.*
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.feature.notifications.api.NotificationFeature
import org.koin.compose.koinInject

@Composable
internal fun NotificationSettings() {
    val notifications = koinInject<NotificationFeature>()
    val state by notifications.state.collectAsState()
    val device by notifications.device.collectAsState()
    LaunchedEffect(Unit) { notifications.refresh() }
    SettingsSection(title = "Уведомления", description = "Изменения модерации, напоминания и новости CoffeePeek") {
        if (!device.available || !device.permissionGranted) {
            SettingsRow(icon = CpIcons.Info, label = if (device.available) "Разрешить уведомления" else "Уведомления недоступны",
                description = if (device.available) "Открыть разрешения этого устройства" else "Требуется настройка Firebase для этой сборки",
                enabled = device.available, onClick = notifications::requestPermission)
            SettingsDivider()
        }
        state.preferences?.let { preferences ->
            SettingsRow(icon = CpIcons.Review, label = "Решения модерации", showArrow = false, enabled = !state.loading,
                onClick = { notifications.save(preferences.copy(moderation = !preferences.moderation)) },
                trailing = { PlatformSwitch(preferences.moderation, { notifications.save(preferences.copy(moderation = it)) }, !state.loading) })
            SettingsDivider()
            SettingsRow(icon = CpIcons.CoffeeBean, label = "Напоминания о чекинах", description = "После 7 дней без посещений и чекинов, раз в неделю",
                showArrow = false, enabled = !state.loading,
                onClick = { notifications.save(preferences.copy(inactivity = !preferences.inactivity)) },
                trailing = { PlatformSwitch(preferences.inactivity, { notifications.save(preferences.copy(inactivity = it)) }, !state.loading) })
            SettingsDivider()
            SettingsRow(icon = CpIcons.Info, label = "Другие сообщения", showArrow = false, enabled = !state.loading,
                onClick = { notifications.save(preferences.copy(custom = !preferences.custom)) },
                trailing = { PlatformSwitch(preferences.custom, { notifications.save(preferences.copy(custom = it)) }, !state.loading) })
        }
        if (state.loading) Text("Загружаем настройки…")
        state.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
            SettingsRow(icon = CpIcons.Info, label = "Повторить", onClick = notifications::refresh)
        }
    }
}
