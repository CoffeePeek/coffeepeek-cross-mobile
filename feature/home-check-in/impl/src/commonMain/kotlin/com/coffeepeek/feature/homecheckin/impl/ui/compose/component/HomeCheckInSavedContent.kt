package com.coffeepeek.feature.homecheckin.impl.ui.compose.component


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.impl.resources.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.previewHomePreparation
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun HomeCheckInSavedContent(entry: HomePreparation, visibilityUnconfirmed: Boolean,
    onDone: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            CoffeePeekIcon(CoffeePeekIcons.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(if (entry.visibility == com.coffeepeek.feature.homecheckin.domain.model.HomePreparationVisibility.Private)
                Res.string.home_check_in_saved_private else Res.string.home_check_in_saved_unknown),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (visibilityUnconfirmed) Text(stringResource(Res.string.home_check_in_saved_visibility_changed),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            CoffeePeekButton(stringResource(Res.string.home_check_in_done), onDone)
        }
    }
}

@Composable private fun HomeCheckInSavedPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInSavedContent(previewHomePreparation(), false, {}) }
}

@Preview @Composable private fun HomeCheckInSavedLightPreview() = HomeCheckInSavedPreview(false)
@Preview @Composable private fun HomeCheckInSavedDarkPreview() = HomeCheckInSavedPreview(true)
