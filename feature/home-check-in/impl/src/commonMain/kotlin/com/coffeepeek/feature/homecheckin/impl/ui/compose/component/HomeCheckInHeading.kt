package com.coffeepeek.feature.homecheckin.impl.ui.compose.component


import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_home
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_saved_private
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_rejection_fallback
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_rejected
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_title_fallback
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_unknown_moderation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCoffeeIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.impl.resources.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.previewHomePreparation
import org.jetbrains.compose.resources.StringResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun HomeCheckInHeading(entry: HomePreparation, dateLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CoffeePeekCoffeeIcon(null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(Res.string.home_check_in_home), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            entry.visibilityLabelResource()?.let { resource ->
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CoffeePeekIcon(if (entry.visibility == HomePreparationVisibility.Private) CoffeePeekIcons.Lock else CoffeePeekIcons.Globe,
                            null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(resource), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (entry.moderation == HomePreparationModeration.Rejected) Text(
                entry.rejectionReason?.takeIf(String::isNotBlank) ?: stringResource(Res.string.home_check_in_rejection_fallback),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
            )
        }
        Text(entry.beanName?.takeIf(String::isNotBlank) ?: stringResource(Res.string.home_check_in_title_fallback),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(dateLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun HomePreparation.visibilityLabelResource(): StringResource? = when (visibility) {
    HomePreparationVisibility.Private -> Res.string.home_check_in_saved_private
    HomePreparationVisibility.Public -> when (moderation) {
        HomePreparationModeration.Approved -> Res.string.home_check_in_unknown_moderation
        HomePreparationModeration.Pending -> Res.string.home_check_in_unknown_moderation
        HomePreparationModeration.Rejected -> Res.string.home_check_in_rejected
        HomePreparationModeration.NotSubmitted -> Res.string.home_check_in_unknown_moderation
        HomePreparationModeration.Unknown -> Res.string.home_check_in_unknown_moderation
    }
    null -> null
}

@Composable private fun HomeCheckInHeadingPreview(dark: Boolean, rejected: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val entry = previewHomePreparation().copy(visibility = HomePreparationVisibility.Public,
        moderation = if (rejected) HomePreparationModeration.Rejected else HomePreparationModeration.Pending)
    Surface { HomeCheckInHeading(entry, "17.10.2026 · 10:30") }
}
@Preview @Composable private fun HomeCheckInHeadingLightPreview() = HomeCheckInHeadingPreview(false)
@Preview @Composable private fun HomeCheckInHeadingDarkPreview() = HomeCheckInHeadingPreview(true)

@Preview @Composable private fun HomeCheckInRejectedLightPreview() = HomeCheckInHeadingPreview(false, true)
@Preview @Composable private fun HomeCheckInRejectedDarkPreview() = HomeCheckInHeadingPreview(true, true)
