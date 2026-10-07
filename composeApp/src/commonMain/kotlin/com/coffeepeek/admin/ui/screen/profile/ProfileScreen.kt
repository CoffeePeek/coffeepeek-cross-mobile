package com.coffeepeek.admin.ui.screen.profile

import com.coffeepeek.admin.ui.component.GuestAuthCard
import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.SettingsDivider
import com.coffeepeek.admin.ui.component.SettingsIconPalette
import com.coffeepeek.admin.ui.component.SettingsRow
import com.coffeepeek.admin.ui.component.SettingsSection
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.admin.utils.CpImage
import org.koin.compose.koinInject
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.favorites_profile_title
import coffeepeek.composeapp.generated.resources.favorites_profile_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileScreen(vm: ProfileViewModel = koinInject()) {
    val state by vm.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = { showLogoutDialog = false; vm.logout() },
            onDismiss = { showLogoutDialog = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                CoffeePeekLoader()
            }
            return@Scaffold
        }

        if (state.error != null && !state.hasContent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(CpDimens.spacing4),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error ?: "Ошибка загрузки профиля",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(CpDimens.spacing3))
                    Button(
                        onClick = vm::refreshProfile,
                        modifier = Modifier.height(CpDimens.buttonHeight),
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Попробовать снова")
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Профиль",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = CpDimens.settingsPagePadding,
                        vertical = CpDimens.spacing4,
                    ),
            )

            state.refreshError?.let { refreshError ->
                Text(
                    text = refreshError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing2),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // ── Шапка ─────────────────────────────────────────────────────────
            if (state.isLoggedIn) {
                ProfileHeader(
                    state = state,
                    onEdit = { Navigator.navigate(Navigator.Screen.EditProfile) },
                )
            } else {
                GuestLoginHeader(
                    onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
                    onRegister = { Navigator.navigate(Navigator.Screen.Register) },
                )
            }

            Spacer(Modifier.height(CpDimens.settingsSectionSpacing))

            SettingsSection(title = "Моя активность") {
                SettingsRow(
                    icon = CpIcons.Favorite,
                    label = stringResource(Res.string.favorites_profile_title),
                    description = stringResource(Res.string.favorites_profile_description),
                    iconColors = SettingsIconPalette.Rose,
                    onClick = { Navigator.navigate(Navigator.Screen.Favorites) },
                )
                SettingsDivider()
                SettingsRow(
                    icon = CpIcons.Location,
                    label = "Чекины",
                    description = "Публичные и скрытые записи о ваших визитах",
                    iconColors = SettingsIconPalette.Sky,
                    onClick = { Navigator.navigate(Navigator.Screen.VisitedPlaces) },
                )
            }

            if (state.isLoggedIn) {
                Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
                SettingsSection(title = "Модерация") {
                    SettingsRow(
                        icon = CpIcons.NoteEdit,
                        label = "Правки кофеен",
                        description = "Изменения, которые вы отправили",
                        iconColors = SettingsIconPalette.Gold,
                        onClick = { openContributions(ContributionKind.Changes) },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = CpIcons.Add,
                        label = "Отправленные кофейни",
                        description = "Кофейни, которые вы добавили",
                        iconColors = SettingsIconPalette.Mint,
                        onClick = { openContributions(ContributionKind.Shops) },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = CpIcons.CoffeeBean,
                        label = "Отправленные обжарщики",
                        description = "Обжарщики, которых вы добавили",
                        iconColors = SettingsIconPalette.Sky,
                        onClick = { openContributions(ContributionKind.Roasters) },
                    )
                }
            }

            if (state.isLoggedIn) {
                Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CpDimens.settingsPagePadding)
                        .height(CpDimens.buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CpColor.Error.copy(alpha = 0.1f),
                        contentColor   = CpColor.Error,
                    ),
                    shape = RoundedCornerShape(CpDimens.buttonRadius),
                ) {
                    Icon(
                        imageVector = CpIcons.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(CpDimens.spacing2))
                    Text(
                        text = "Выйти",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(Modifier.height(CpDimens.spacing8 + LocalFloatingNavClearance.current))
        }
    }
}

// ── Шапка профиля ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(state: ProfileUiState, onEdit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = CpDimens.settingsPagePadding,
                end = CpDimens.settingsPagePadding,
            ),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.size(104.dp)) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(CpColor.Primary, CpColor.GoldWarm))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.initials.ifEmpty { "?" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = CpColor.DarkTextOnPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!state.avatarUrl.isNullOrBlank()) {
                        CpImage(
                            data = state.avatarUrl,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = CpIcons.Edit,
                        contentDescription = "Редактировать профиль",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = state.displayName.ifBlank { "Пользователь" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.email.isNotBlank()) {
                    Text(
                        text = state.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(CpDimens.spacing2))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatBadge(state.checkInCount, "Чекины", Modifier.weight(1f)) {
                        Navigator.navigate(Navigator.Screen.VisitedPlaces)
                    }
                    StatBadge(state.addedShopsCount, "Кофейни", Modifier.weight(1f)) {
                        openContributions(ContributionKind.Shops)
                    }
                }
            }
        }

        if (!state.about.isNullOrBlank()) {
            Text(
                text = state.about,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GuestLoginHeader(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        GuestAuthCard(
            onLogin = onLogin,
            onRegister = onRegister,
            modifier = Modifier.padding(horizontal = CpDimens.settingsPagePadding),
        )
    }
}

@Composable
private fun StatBadge(count: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CpDimens.radiusMd))
            .clickable(onClickLabel = label, onClick = onClick),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun openContributions(kind: ContributionKind) =
    Navigator.navigate(Navigator.Screen.MyContributions(kind.name))

// ── Диалог выхода ─────────────────────────────────────────────────────────────

@Composable
private fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Выйти из аккаунта?", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(
                text = "Вы уверены? Потребуется повторный вход.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.height(CpDimens.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CpColor.Error,
                    contentColor   = Color.White,
                ),
                shape = RoundedCornerShape(percent = 50),
            ) {
                Text("Выйти", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Отмена",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        shape = RoundedCornerShape(CpDimens.radius2xl),
    )
}
