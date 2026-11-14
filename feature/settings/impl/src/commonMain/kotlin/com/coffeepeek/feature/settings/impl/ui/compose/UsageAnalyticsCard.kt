package com.coffeepeek.feature.settings.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgeColors
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitch
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.feature.settings.domain.UsageConsentState
import com.coffeepeek.feature.settings.impl.resources.Res
import com.coffeepeek.feature.settings.impl.resources.settings_analytics_sync_failed
import com.coffeepeek.feature.settings.impl.resources.settings_policy_analytics_text
import com.coffeepeek.feature.settings.impl.resources.settings_policy_analytics_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun UsageAnalyticsCard(consent: UsageConsentState, onConsent: (Boolean) -> Unit, iconColors: CoffeePeekIconBadgeColors) {
    SettingsCard(Modifier.toggleable(value = consent.accepted, role = Role.Switch, onValueChange = onConsent)) {
        Row(Modifier.padding(CoffeePeekDimens.settingsRowPaddingH), verticalAlignment = Alignment.Top) {
            CoffeePeekIconBadge(CoffeePeekIcons.Shield, iconColors)
            Spacer(Modifier.width(CoffeePeekDimens.spacing3))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.settings_policy_analytics_title), style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(CoffeePeekDimens.spacing2))
                Text(
                    stringResource(if (consent.syncFailed) Res.string.settings_analytics_sync_failed else Res.string.settings_policy_analytics_text),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(CoffeePeekDimens.spacing3))
            CoffeePeekSwitch(consent.accepted, null, Modifier.align(Alignment.CenterVertically).clearAndSetSemantics { })
        }
    }
}

@Composable
private fun UsageAnalyticsPreview(dark: Boolean, failed: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        UsageAnalyticsCard(UsageConsentState(true, !failed, syncFailed = failed), {},
            CoffeePeekIconBadgeColors(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.colorScheme.primary))
    }
}

@Preview @Composable private fun UsageAnalyticsLightPreview() = UsageAnalyticsPreview(false)
@Preview @Composable private fun UsageAnalyticsDarkPreview() = UsageAnalyticsPreview(true)
@Preview @Composable private fun UsageAnalyticsFailureLightPreview() = UsageAnalyticsPreview(false, true)
@Preview @Composable private fun UsageAnalyticsFailureDarkPreview() = UsageAnalyticsPreview(true, true)
