package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.analytics.AnalyticsConsentSwitch
import com.coffeepeek.admin.legal.LegalUrls
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.SettingsRow
import com.coffeepeek.admin.ui.component.SettingsSection
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
            Spacer(Modifier.height(CpDimens.spacing4))
            Column(Modifier.padding(horizontal = CpDimens.settingsPagePadding)) {
                Text(
                    "Ознакомьтесь с правилами использования CoffeePeek.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(CpDimens.spacing4))
                AppButton("Посмотреть политику использования", onClick = {
                    OpenInBrowser.openInBrowser(LegalUrls.TERMS)
                })
            }
            if (analytics != null && state?.let { it.authReady && !it.userId.isNullOrBlank() } == true) {
                Spacer(Modifier.height(CpDimens.settingsSectionSpacing))
                SettingsSection(
                    title = "Конфиденциальность",
                    description = "С вашего согласия события использования отправляются в PostHog (ЕС), " +
                        "чтобы улучшать CoffeePeek. Без текстов поиска, заметок, фото и точных координат.",
                ) {
                    SettingsRow(
                        icon = CpIcons.Info,
                        label = "Аналитика использования",
                        description = "Необязательно. Решение можно изменить в любой момент.",
                        onClick = { scope.launch {
                            analytics.setConsent(if (analytics.state.value.consent == AnalyticsConsent.Accepted)
                                AnalyticsConsent.Declined else AnalyticsConsent.Accepted)
                        } },
                        trailing = { AnalyticsConsentSwitch() },
                    )
                }
            }
            Spacer(Modifier.height(CpDimens.spacing8))
        }
    }
}
