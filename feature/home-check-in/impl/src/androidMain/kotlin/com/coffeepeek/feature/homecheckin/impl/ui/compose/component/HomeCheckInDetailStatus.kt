package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomeCheckInDetailStatus(
    loading: Boolean,
    deleting: Boolean,
    issue: HomePreparationIssue?,
    deleteFailed: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (loading || deleting) {
            val description = stringResource(if (deleting) R.string.home_check_in_deleting else R.string.home_check_in_loading)
            CoffeePeekLoader(description)
        }
        if (issue != null) {
            val message = when (issue) {
                HomePreparationIssue.Offline -> R.string.home_check_in_offline
                HomePreparationIssue.Timeout -> R.string.home_check_in_timeout
                HomePreparationIssue.Unauthorized -> R.string.home_check_in_unauthorized
                HomePreparationIssue.Forbidden -> R.string.home_check_in_forbidden
                HomePreparationIssue.NotFound -> R.string.home_check_in_not_found
                HomePreparationIssue.RateLimited -> R.string.home_check_in_rate_limited
                HomePreparationIssue.Unavailable -> R.string.home_check_in_unavailable
                else -> if (deleteFailed) R.string.home_check_in_delete_failed else R.string.home_check_in_load_failed
            }
            Text(stringResource(message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (!loading && !deleting && issue !in setOf(HomePreparationIssue.Unauthorized, HomePreparationIssue.Forbidden,
                    HomePreparationIssue.NotFound)) {
                // A retry refreshes the record. It never repeats a DELETE without confirmation.
                TextButton(onClick = onRetry) { Text(stringResource(R.string.home_check_in_retry)) }
            }
        }
    }
}

@PreviewLightDark @Composable private fun HomeCheckInDetailStatusPreview() = CoffeePeekTheme {
    Surface { HomeCheckInDetailStatus(false, false, HomePreparationIssue.Offline, false, {}) }
}
