package com.coffeepeek.feature.settings.impl.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgeColors
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitch
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.settings.domain.UsageConsentState
import com.coffeepeek.feature.settings.impl.R
import com.coffeepeek.feature.settings.impl.ui.UsagePolicyViewModel
import com.coffeepeek.feature.settings.impl.ui.model.UsagePolicyAction

@Composable
internal fun UsagePolicyScreen(vm: UsagePolicyViewModel, onOpenTerms: () -> Unit, onOpenPrivacy: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    UsagePolicyContent(state.consent, { vm.onAction(UsagePolicyAction.SetConsent(it)) }, onOpenTerms, onOpenPrivacy, onBack, modifier)
}

@Composable
internal fun UsagePolicyContent(
    consent: UsageConsentState, onConsent: (Boolean) -> Unit, onOpenTerms: () -> Unit, onOpenPrivacy: () -> Unit,
    onBack: () -> Unit, modifier: Modifier = Modifier,
) {
    val iconColors = CoffeePeekIconBadgeColors(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.colorScheme.primary)
    SettingsPage(stringResource(R.string.settings_policy_title), onBack, modifier) {
        Spacer(Modifier.height(CoffeePeekDimens.spacing6))
        Column(Modifier.padding(horizontal = CoffeePeekDimens.settingsPagePadding)) {
            Text(
                stringResource(R.string.settings_policy_section), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(CoffeePeekDimens.spacing2))
            if (consent.available) {
                SettingsCard(Modifier.toggleable(value = consent.accepted, role = Role.Switch, onValueChange = onConsent)) {
                    Row(Modifier.padding(CoffeePeekDimens.settingsRowPaddingH), verticalAlignment = Alignment.Top) {
                        CoffeePeekIconBadge(CoffeePeekIcons.Shield, iconColors)
                        Spacer(Modifier.width(CoffeePeekDimens.spacing3))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_policy_analytics_title), style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(CoffeePeekDimens.spacing2))
                            Text(
                                stringResource(R.string.settings_policy_analytics_text), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(CoffeePeekDimens.spacing3))
                        CoffeePeekSwitch(consent.accepted, null, Modifier.align(Alignment.CenterVertically).clearAndSetSemantics { })
                    }
                }
                Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
            }
            PolicyLink(R.string.settings_policy_terms_title, R.string.settings_policy_terms_description, CoffeePeekIcons.FileText, iconColors, onOpenTerms)
            Spacer(Modifier.height(CoffeePeekDimens.settingsSectionSpacing))
            PolicyLink(R.string.settings_policy_privacy_title, R.string.settings_policy_privacy_description, CoffeePeekIcons.Shield, iconColors, onOpenPrivacy)
        }
        Spacer(Modifier.height(CoffeePeekDimens.spacing8))
    }
}

@Composable
private fun PolicyLink(title: Int, description: Int, icon: ImageVector, iconColors: CoffeePeekIconBadgeColors, onClick: () -> Unit) {
    SettingsCard(Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(CoffeePeekDimens.settingsRowPaddingH), verticalAlignment = Alignment.CenterVertically) {
            CoffeePeekIconBadge(icon, iconColors)
            Spacer(Modifier.width(CoffeePeekDimens.spacing3))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(CoffeePeekDimens.spacing1))
                Text(stringResource(description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(CoffeePeekDimens.spacing2))
            Icon(CoffeePeekIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(CoffeePeekDimens.settingsIconSize))
        }
    }
}

@PreviewLightDark
@Composable
private fun UsagePolicyPreview() = CoffeePeekTheme {
    Surface { UsagePolicyContent(UsageConsentState(available = true, accepted = true), {}, {}, {}, {}) }
}

@PreviewLightDark
@Composable
private fun UsagePolicyGuestPreview() = CoffeePeekTheme { Surface { UsagePolicyContent(UsageConsentState.Unavailable, {}, {}, {}, {}) } }
