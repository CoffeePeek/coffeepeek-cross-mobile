package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.Shield
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.legal.LegalUrls
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.PlatformSwitch
import com.coffeepeek.admin.ui.component.SettingsIconBadge
import com.coffeepeek.admin.ui.component.SettingsIconColors
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.OpenInBrowser
import kotlinx.coroutines.launch

@Composable
internal fun UsagePolicyScreen() {
    val analytics = Analytics.service
    val state = analytics?.state?.collectAsState()?.value
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { CpTopBar("Политика использования") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PlatformScrollableColumn(Modifier.fillMaxSize().padding(padding)) {
            Spacer(Modifier.height(CpDimens.spacing6))
            Column(Modifier.padding(horizontal = CpDimens.settingsPagePadding)) {
                Text(
                    text = "КОНФИДЕНЦИАЛЬНОСТЬ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(CpDimens.spacing2))
                val iconColors = SettingsIconColors(
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    icon = MaterialTheme.colorScheme.primary,
                )
                if (analytics != null && state != null && state.authReady && !state.userId.isNullOrBlank()) {
                    val analyticsAccepted = state.consent == AnalyticsConsent.Accepted
                    UsagePolicyCard(
                        modifier = Modifier.toggleable(
                            value = analyticsAccepted,
                            role = Role.Switch,
                            onValueChange = { checked ->
                                scope.launch {
                                    analytics.setConsent(
                                        if (checked) AnalyticsConsent.Accepted else AnalyticsConsent.Declined,
                                    )
                                }
                            },
                        ),
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            SettingsIconBadge(
                                icon = PhosphorIcons.Regular.Shield,
                                colors = iconColors,
                            )
                            Spacer(Modifier.width(CpDimens.spacing3))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Аналитика использования",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(CpDimens.spacing2))
                                Text(
                                    text = "С вашего согласия события использования отправляются в PostHog (ЕС), " +
                                        "чтобы улучшать CoffeePeek. Без текстов поиска, заметок, фото и точных координат.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(CpDimens.spacing3))
                            PlatformSwitch(
                                checked = analyticsAccepted,
                                onCheckedChange = null,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .clearAndSetSemantics { },
                            )
                        }
                    }
                    Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
                }
                UsagePolicyLinkCard(
                    title = "Правила использования",
                    description = "Узнайте, как работает CoffeePeek.",
                    url = LegalUrls.TERMS,
                    icon = PhosphorIcons.Regular.FileText,
                    iconColors = iconColors,
                )
                Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
                UsagePolicyLinkCard(
                    title = "Политика конфиденциальности",
                    description = "Как мы обрабатываем и защищаем ваши данные.",
                    url = LegalUrls.PRIVACY,
                    icon = PhosphorIcons.Regular.Shield,
                    iconColors = iconColors,
                )
            }
            Spacer(Modifier.height(CpDimens.spacing8))
        }
    }
}

@Composable
private fun UsagePolicyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CpDimens.settingsRowPaddingH),
            content = content,
        )
    }
}

@Composable
private fun UsagePolicyLinkCard(
    title: String,
    description: String,
    url: String,
    icon: ImageVector,
    iconColors: SettingsIconColors,
) {
    UsagePolicyCard(modifier = Modifier.clickable(role = Role.Button) { OpenInBrowser.openInBrowser(url) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIconBadge(icon = icon, colors = iconColors)
            Spacer(Modifier.width(CpDimens.spacing3))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(CpDimens.spacing1))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(CpDimens.spacing2))
            Icon(
                imageVector = CpIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(CpDimens.settingsIconSize),
            )
        }
    }
}
