package com.coffeepeek.admin.ui.screen.profile

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

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
import androidx.compose.ui.tooling.preview.Preview
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
import com.coffeepeek.admin.ui.component.SettingsDivider
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.OpenInBrowser
import kotlinx.coroutines.launch

@Composable
internal fun UsagePolicyScreen() {
    val localizedLegacyUsagePolicyScreenSettingsPolicyTitle = stringResource(Res.string.legacy_usage_policy_screen_settings_policy_title)
    val localizedLegacyUsagePolicyScreenSettingsPolicySection = stringResource(Res.string.legacy_usage_policy_screen_settings_policy_section)
    val localizedLegacyUsagePolicyScreenSettingsPolicyAnalyticsTitle = stringResource(Res.string.legacy_usage_policy_screen_settings_policy_analytics_title)
    val localizedLegacyUsagePolicyScreenAnalyticsPolicyPrefix = stringResource(Res.string.legacy_usage_policy_screen_analytics_policy_prefix)
    val localizedLegacyUsagePolicyScreenAnalyticsPolicySuffix = stringResource(Res.string.legacy_usage_policy_screen_analytics_policy_suffix)

    val analytics = Analytics.service
    val state = analytics?.state?.collectAsState()?.value
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { CpTopBar(localizedLegacyUsagePolicyScreenSettingsPolicyTitle) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PlatformScrollableColumn(reserveBottomClearance = true, modifier = Modifier.fillMaxSize().padding(padding)) {
            Spacer(Modifier.height(CpDimens.spacing6))
            Column(Modifier.padding(horizontal = CpDimens.settingsPagePadding)) {
                Text(
                    text = localizedLegacyUsagePolicyScreenSettingsPolicySection,
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
                        Row(Modifier.padding(CpDimens.settingsRowPaddingH), verticalAlignment = Alignment.Top) {
                            SettingsIconBadge(
                                icon = PhosphorIcons.Regular.Shield,
                                colors = iconColors,
                            )
                            Spacer(Modifier.width(CpDimens.spacing3))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = localizedLegacyUsagePolicyScreenSettingsPolicyAnalyticsTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(CpDimens.spacing2))
                                Text(
                                    text = localizedLegacyUsagePolicyScreenAnalyticsPolicyPrefix +
                                        localizedLegacyUsagePolicyScreenAnalyticsPolicySuffix,
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
                UsagePolicyLinks(
                    iconColors,
                    onOpenTerms = { OpenInBrowser.openInBrowser(LegalUrls.TERMS) },
                    onOpenPrivacy = { OpenInBrowser.openInBrowser(LegalUrls.PRIVACY) },
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
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun UsagePolicyLinks(
    iconColors: SettingsIconColors,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val localizedLegacyUsagePolicyScreenSettingsPolicyTermsTitle = stringResource(Res.string.legacy_usage_policy_screen_settings_policy_terms_title)
    val localizedLegacyUsagePolicyScreenSettingsPolicyTermsDescription = stringResource(Res.string.legacy_usage_policy_screen_settings_policy_terms_description)
    val localizedLegacyUsagePolicyScreenSettingsPolicyPrivacyTitle = stringResource(Res.string.legacy_usage_policy_screen_settings_policy_privacy_title)
    val localizedLegacyUsagePolicyScreenSettingsPolicyPrivacyDescription = stringResource(Res.string.legacy_usage_policy_screen_settings_policy_privacy_description)

    UsagePolicyCard {
        UsagePolicyLinkRow(
            title = localizedLegacyUsagePolicyScreenSettingsPolicyTermsTitle,
            description = localizedLegacyUsagePolicyScreenSettingsPolicyTermsDescription,
            onClick = onOpenTerms,
            icon = PhosphorIcons.Regular.FileText,
            iconColors = iconColors,
        )
        SettingsDivider()
        UsagePolicyLinkRow(
            title = localizedLegacyUsagePolicyScreenSettingsPolicyPrivacyTitle,
            description = localizedLegacyUsagePolicyScreenSettingsPolicyPrivacyDescription,
            onClick = onOpenPrivacy,
            icon = PhosphorIcons.Regular.Shield,
            iconColors = iconColors,
        )
    }
}

@Composable
private fun UsagePolicyLinkRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    icon: ImageVector,
    iconColors: SettingsIconColors,
) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
        .padding(CpDimens.settingsRowPaddingH), verticalAlignment = Alignment.CenterVertically) {
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

@Preview
@Composable
private fun UsagePolicyLinksLightPreview() = CoffeePeekTheme(darkTheme = false) {
    val colors = SettingsIconColors(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.colorScheme.primary)
    UsagePolicyLinks(colors, {}, {})
}

@Preview
@Composable
private fun UsagePolicyLinksDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    val colors = SettingsIconColors(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.colorScheme.primary)
    UsagePolicyLinks(colors, {}, {})
}
