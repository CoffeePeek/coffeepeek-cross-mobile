package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.impl.R

@Composable
internal fun CoffeeLoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.coffee_load_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onRetry) { Text(stringResource(R.string.coffee_retry)) }
    }
}

@PreviewLightDark @Composable private fun CoffeeLoadErrorPreview() = CoffeePeekTheme {
    Surface { CoffeeLoadError({}) }
}
