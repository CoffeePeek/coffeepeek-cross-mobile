package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.impl.R
import com.coffeepeek.feature.homecheckin.impl.ui.compose.previewHomePreparation
import androidx.compose.ui.res.stringResource

@Composable
internal fun HomeCheckInHeading(entry: HomePreparation, dateLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(CoffeePeekIcons.Coffee, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.home_check_in_home), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            entry.visibilityLabelResource()?.let { resource ->
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(if (entry.visibility == HomePreparationVisibility.Private) CoffeePeekIcons.Lock else CoffeePeekIcons.Globe,
                            null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(resource), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text(entry.beanName?.takeIf(String::isNotBlank) ?: stringResource(R.string.home_check_in_title_fallback),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(dateLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun HomePreparation.visibilityLabelResource(): Int? = when (visibility) {
    HomePreparationVisibility.Private -> R.string.home_check_in_private
    HomePreparationVisibility.Public -> when (moderation) {
        HomePreparationModeration.Approved -> R.string.home_check_in_public
        HomePreparationModeration.Pending -> R.string.home_check_in_pending
        HomePreparationModeration.Rejected -> R.string.home_check_in_rejected
        HomePreparationModeration.NotSubmitted -> R.string.home_check_in_not_submitted
        HomePreparationModeration.Unknown -> R.string.home_check_in_unknown_moderation
    }
    null -> null
}

@PreviewLightDark @Composable private fun HomeCheckInHeadingPreview() = CoffeePeekTheme {
    Surface { HomeCheckInHeading(previewHomePreparation(), "17.10.2026 · 10:30") }
}
