package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.loader_checking_updates
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun PlatformActivityIndicator(modifier: Modifier = Modifier) {
    CoffeePeekLoader(contentDescription = stringResource(Res.string.loader_checking_updates),
        modifier = modifier, size = CpDimens.settingsIconSize,
        color = MaterialTheme.colorScheme.onSurfaceVariant, strokeWidth = 2.dp)
}

@Preview @Composable private fun ActivityIndicatorLightPreview() = CoffeePeekTheme(darkTheme = false) {
    Surface { PlatformActivityIndicator() }
}
@Preview @Composable private fun ActivityIndicatorDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    Surface { PlatformActivityIndicator() }
}
