package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.coffee.impl.resources.Res
import com.coffeepeek.feature.coffee.impl.resources.coffee_load_failed
import com.coffeepeek.feature.coffee.impl.resources.coffee_retry
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun CoffeeLoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(Res.string.coffee_load_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onRetry) { Text(stringResource(Res.string.coffee_retry)) }
    }
}

@Composable private fun CoffeeLoadErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeeLoadError({}) }
}
@Preview @Composable private fun CoffeeLoadErrorLightPreview() = CoffeeLoadErrorPreview(false)
@Preview @Composable private fun CoffeeLoadErrorDarkPreview() = CoffeeLoadErrorPreview(true)
