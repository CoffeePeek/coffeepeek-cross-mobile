package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_public_hint
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_public_title
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_audience_everyone
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_audience_only_me
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun HomeCheckInEditorVisibility(isPublic: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val everyone = stringResource(Res.string.home_check_in_audience_everyone)
    val onlyMe = stringResource(Res.string.home_check_in_audience_only_me)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.home_check_in_public_title), style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold)
        CoffeePeekCapsuleSegmentedControl(listOf(true, false), isPublic,
            { if (it) everyone else onlyMe }, onChange, enabled = enabled)
        Text(stringResource(Res.string.home_check_in_public_hint), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun HomeCheckInEditorVisibilityPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInEditorVisibility(false, true, {}) }
}
@Preview @Composable private fun HomeCheckInEditorVisibilityLightPreview() = HomeCheckInEditorVisibilityPreview(false)
@Preview @Composable private fun HomeCheckInEditorVisibilityDarkPreview() = HomeCheckInEditorVisibilityPreview(true)
