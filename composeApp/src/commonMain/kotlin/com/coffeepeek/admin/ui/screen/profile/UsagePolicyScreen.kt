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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.Shield
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.analytics.AnalyticsConsentSwitch
import com.coffeepeek.admin.legal.LegalUrls
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
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
                if (analytics != null && state?.let { it.authReady && !it.userId.isNullOrBlank() } == true) {
                    UsagePolicyCard(onClick = {
                        scope.launch {
                            analytics.setConsent(
                                if (analytics.state.value.consent == AnalyticsConsent.Accepted)
                                    AnalyticsConsent.Declined else AnalyticsConsent.Accepted,
                            )
                        }
                    }) {
                        Row(verticalAlignment = Alignment.Top) {
                            SettingsIconBadge(
                                icon = PhosphorIcons.Regular.Shield,
                                colors = iconColors,
                            )
                            Spacer(Modifier.width(CpDimens.spacing3))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Аналитика использования",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(CpDimens.spacing2))
                                    AnalyticsConsentSwitch(
                                        modifier = Modifier.semantics {
                                            contentDescription = "Аналитика использования"
                                        },
                                    )
                                }
                                Spacer(Modifier.height(CpDimens.spacing2))
                                Text(
                                    text = "С вашего согласия события использования отправляются в PostHog (ЕС), " +
                                        "чтобы улучшать CoffeePeek. Без текстов поиска, заметок, фото и точных координат.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
                }
                UsagePolicyCard(onClick = { OpenInBrowser.openInBrowser(LegalUrls.TERMS) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconBadge(
                            icon = PhosphorIcons.Regular.FileText,
                            colors = iconColors,
                        )
                        Spacer(Modifier.width(CpDimens.spacing3))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Правила использования",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(CpDimens.spacing1))
                            Text(
                                text = "Узнайте, как работает CoffeePeek.",
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
            Spacer(Modifier.height(CpDimens.spacing8))
        }
    }
}

@Composable
private fun UsagePolicyCard(
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(CpDimens.settingsRowPaddingH),
            content = content,
        )
    }
}
