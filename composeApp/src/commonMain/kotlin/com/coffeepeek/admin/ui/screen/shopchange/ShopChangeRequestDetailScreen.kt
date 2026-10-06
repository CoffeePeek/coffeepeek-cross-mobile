package com.coffeepeek.admin.ui.screen.shopchange

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.SettingsIconBadge
import com.coffeepeek.admin.ui.component.SettingsIconColors
import com.coffeepeek.admin.ui.component.SettingsIconPalette
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.domain.model.MenuItemAvailability
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.ShopChangeContacts
import com.coffeepeek.domain.model.ShopChangeMenu
import com.coffeepeek.domain.model.ShopChangePayload
import com.coffeepeek.domain.model.ShopChangeSection
import org.koin.core.parameter.parametersOf

@Composable
fun ShopChangeRequestDetailScreen(requestId: String) {
    val vm: ShopChangeRequestDetailViewModel = platformViewModel(
        parameters = { parametersOf(requestId) },
    )
    val state by vm.state.collectAsState()

    state.error?.let { err ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text("Ошибка") },
            text = { Text(err) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("Понятно") } },
        )
    }

    Scaffold(
        topBar = { CpTopBar("Отправленная правка") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val request = state.request
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CoffeePeekLoader() }

            request == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Заявка не найдена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(CpDimens.spacing3))
                    Button(
                        onClick = vm::refresh,
                        modifier = Modifier.height(CpDimens.buttonHeight),
                        shape = RoundedCornerShape(percent = 50),
                    ) { Text("Повторить") }
                }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(CpDimens.spacing4),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
            ) {
                ChangeHeroCard(
                    section = request.section,
                    status = request.status,
                    createdAt = utcIsoToLocalDateTime(request.createdAtUtc),
                )
                request.rejectionReason?.takeIf { it.isNotBlank() }?.let { RejectionCard(it) }
                ChangePayloadCard(section = request.section, payload = request.payload)
                OutlinedButton(
                    onClick = { Navigator.navigate(Navigator.Screen.ShopDetail(request.shopId)) },
                    enabled = request.shopId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(CpDimens.buttonHeight),
                    shape = RoundedCornerShape(percent = 50),
                ) {
                    Icon(CpIcons.Coffee, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(CpDimens.spacing2))
                    Text("Открыть кофейню")
                }
                Spacer(Modifier.height(CpDimens.spacing4))
            }
        }
    }
}

@Composable
private fun ChangeHeroCard(section: ShopChangeSection, status: ModerationStatus, createdAt: String) {
    val visual = section.visual()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(CpDimens.spacing4)) {
            Row(verticalAlignment = Alignment.Top) {
                SettingsIconBadge(
                    icon = visual.icon,
                    colors = visual.colors,
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.width(CpDimens.spacing3))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.title(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(CpDimens.spacing1))
                    Text(
                        text = section.hint(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(CpDimens.spacing4))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status)
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = CpIcons.Calendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(CpDimens.spacing1))
                Text(
                    text = createdAt,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RejectionCard(reason: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CpDimens.radiusLg))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.09f))
            .padding(CpDimens.spacing4),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = CpIcons.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp),
        )
        Column {
            Text(
                text = "Причина отклонения",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(CpDimens.spacing1))
            Text(reason, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChangePayloadCard(section: ShopChangeSection, payload: ShopChangePayload) {
    val visual = section.visual()
    DetailCard(title = "Что отправлено", icon = visual.icon, colors = visual.colors) {
        when (section) {
            ShopChangeSection.Description -> DescriptionContent(payload.description)
            ShopChangeSection.Contacts -> ContactsContent(payload.contacts)
            ShopChangeSection.Photos -> PhotoContent(
                retained = payload.photos?.retainedPhotoIds?.size ?: 0,
                added = payload.photos?.newPhotos?.size ?: 0,
            )
            ShopChangeSection.Tags -> CountContent("Выбрано особенностей", payload.tagIds?.size ?: 0)
            ShopChangeSection.Roasters -> CountContent("Выбрано обжарщиков", payload.roasterIds?.size ?: 0)
            ShopChangeSection.Equipment -> CountContent("Выбрано оборудования", payload.equipmentIds?.size ?: 0)
            ShopChangeSection.BrewMethods -> CountContent("Выбрано методов", payload.brewMethodIds?.size ?: 0)
            ShopChangeSection.Menu -> MenuContent(payload.menu)
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    icon: ImageVector,
    colors: SettingsIconColors,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(CpDimens.spacing4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBadge(icon = icon, colors = colors)
            Spacer(Modifier.width(CpDimens.spacing3))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
            content = content,
        )
    }
}

@Composable
private fun DescriptionContent(description: String?) {
    Text(
        text = description?.takeIf { it.isNotBlank() } ?: "Описание удалено",
        style = MaterialTheme.typography.bodyLarge,
        color = if (description.isNullOrBlank()) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ContactsContent(contacts: ShopChangeContacts?) {
    if (contacts == null) {
        EmptyPayloadText()
        return
    }
    DetailValueRow(CpIcons.Phone, "Телефон", contacts.phoneNumber)
    DetailValueRow(CpIcons.Email, "Email", contacts.email)
    DetailValueRow(CpIcons.Globe, "Сайт", contacts.siteLink)
    DetailValueRow(CpIcons.Instagram, "Instagram", contacts.instagramLink)
}

@Composable
private fun DetailValueRow(icon: ImageVector, label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(CpDimens.spacing3))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value?.takeIf { it.isNotBlank() } ?: "Удалено",
                style = MaterialTheme.typography.bodyMedium,
                color = if (value.isNullOrBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PhotoContent(retained: Int, added: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        StatBlock(value = retained.toString(), label = "Сохранено", modifier = Modifier.weight(1f))
        StatBlock(value = added.toString(), label = "Добавлено", modifier = Modifier.weight(1f))
    }
    Text(
        text = "Новый состав галереи применится после одобрения.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CountContent(label: String, count: Int) {
    StatBlock(value = count.toString(), label = label, modifier = Modifier.fillMaxWidth())
    Text(
        text = "Новый набор применится после одобрения.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MenuContent(menu: ShopChangeMenu?) {
    if (menu == null) {
        EmptyPayloadText()
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        StatBlock(menu.items.size.toString(), "Позиций", Modifier.weight(1f))
        StatBlock(
            (menu.retainedPhotoIds.size + menu.newPhotos.size).toString(),
            "Фото меню",
            Modifier.weight(1f),
        )
    }
    menu.items.forEachIndexed { index, item ->
        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.slug, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                val details = listOfNotNull(
                    item.volumeMl?.let { "$it мл" },
                    item.price?.let { "$it BYN" },
                ).joinToString(" · ")
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = item.availability.title(),
                style = MaterialTheme.typography.labelMedium,
                color = when (item.availability) {
                    MenuItemAvailability.Present -> CpColor.Success
                    MenuItemAvailability.Absent -> MaterialTheme.colorScheme.error
                    MenuItemAvailability.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CpDimens.radiusMd))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(CpDimens.spacing3),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyPayloadText() {
    Text(
        text = "В заявке нет данных для отображения.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatusBadge(status: ModerationStatus) {
    val color = status.color()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(color),
        )
        Text(status.title(), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

private data class SectionVisual(val icon: ImageVector, val colors: SettingsIconColors)

private fun ShopChangeSection.visual() = when (this) {
    ShopChangeSection.Photos -> SectionVisual(CpIcons.Photo, SettingsIconPalette.Sky)
    ShopChangeSection.Contacts -> SectionVisual(CpIcons.Phone, SettingsIconPalette.Mint)
    ShopChangeSection.Description -> SectionVisual(CpIcons.NoteEdit, SettingsIconPalette.Lavender)
    ShopChangeSection.Tags -> SectionVisual(CpIcons.Sparkle, SettingsIconPalette.Gold)
    ShopChangeSection.Roasters -> SectionVisual(CpIcons.CoffeeBean, SettingsIconPalette.Rose)
    ShopChangeSection.Equipment -> SectionVisual(CpIcons.Settings, SettingsIconPalette.Cyan)
    ShopChangeSection.Menu -> SectionVisual(CpIcons.Menu, SettingsIconPalette.Emerald)
    ShopChangeSection.BrewMethods -> SectionVisual(CpIcons.Coffee, SettingsIconPalette.Aqua)
}

private fun ModerationStatus.color(): Color = when (this) {
    ModerationStatus.Approved -> CpColor.Success
    ModerationStatus.Pending -> CpColor.GoldWarmHover
    ModerationStatus.Rejected -> CpColor.Error
}

private fun MenuItemAvailability.title() = when (this) {
    MenuItemAvailability.Unknown -> "Не указано"
    MenuItemAvailability.Present -> "Есть"
    MenuItemAvailability.Absent -> "Нет"
}
