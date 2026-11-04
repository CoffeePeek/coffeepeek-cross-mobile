package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekShimmer
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_loading
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

/** Initial profile geometry only: no identity, private values, images or interactive destinations. */
@Composable
internal fun ProfileLoadingSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.profile_loading)
    LazyColumn(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).clearAndSetSemantics {
        contentDescription = description
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    }, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), userScrollEnabled = false) {
        item { ProfileLoadingHeader() }
        item { ProfileLoadingAbout() }
        item { CoffeePeekShimmer(Modifier.fillMaxWidth().height(90.dp)) }
        item { ProfileLoadingSection() }
    }
}

@Composable
private fun ProfileLoadingHeader() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CoffeePeekShimmer(Modifier.size(88.dp), shape = CircleShape)
            CoffeePeekShimmer(Modifier.weight(1f).height(28.dp))
        }
        ProfileLoadingCounters()
    }
}

@Composable
private fun ProfileLoadingCounters() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CoffeePeekShimmer(Modifier.width(36.dp).height(24.dp))
                CoffeePeekShimmer(Modifier.fillMaxWidth(0.85f).height(12.dp))
            }
        }
    }
}

@Composable
private fun ProfileLoadingAbout() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CoffeePeekShimmer(Modifier.fillMaxWidth(0.9f).height(16.dp))
        CoffeePeekShimmer(Modifier.fillMaxWidth(0.6f).height(16.dp))
    }
}

@Composable
private fun ProfileLoadingSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CoffeePeekShimmer(Modifier.fillMaxWidth(0.5f).height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CoffeePeekShimmer(Modifier.fillMaxWidth().aspectRatio(1f))
                    CoffeePeekShimmer(Modifier.fillMaxWidth(0.75f).height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileLoadingSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { ProfileLoadingSkeleton(Modifier.width(360.dp).height(760.dp)) }
}
@Preview @Composable private fun ProfileLoadingLightPreview() = ProfileLoadingSample(false)
@Preview @Composable private fun ProfileLoadingDarkPreview() = ProfileLoadingSample(true)
