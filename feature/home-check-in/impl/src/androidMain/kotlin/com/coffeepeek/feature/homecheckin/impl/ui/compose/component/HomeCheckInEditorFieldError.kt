package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomeCheckInEditorFieldError(message: String) {
    val color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f)
        lerp(MaterialTheme.colorScheme.error, Color.White, 0.3f) else MaterialTheme.colorScheme.error
    Row(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
        Icon(CoffeePeekIcons.Error, null, Modifier.size(18.dp), tint = color)
        Text(message, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@PreviewLightDark @Composable private fun HomeCheckInEditorFieldErrorPreview() = CoffeePeekTheme {
    Surface { HomeCheckInEditorFieldError(stringResource(R.string.home_check_in_error_dose)) }
}
