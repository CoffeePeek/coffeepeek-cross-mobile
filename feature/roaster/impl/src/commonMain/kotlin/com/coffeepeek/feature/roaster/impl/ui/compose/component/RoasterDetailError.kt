package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.impl.resources.Res
import com.coffeepeek.feature.roaster.impl.resources.roaster_detail_back
import com.coffeepeek.feature.roaster.impl.resources.roaster_detail_load_failed
import com.coffeepeek.feature.roaster.impl.resources.roaster_detail_retry

@Composable
internal fun RoasterDetailError(onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(CoffeePeekDimens.spacing4), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            Text(stringResource(Res.string.roaster_detail_load_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onRetry, Modifier.height(CoffeePeekDimens.buttonHeight), shape = RoundedCornerShape(percent = 50)) {
                Text(stringResource(Res.string.roaster_detail_retry))
            }
            OutlinedButton(onBack, Modifier.height(CoffeePeekDimens.buttonHeight), shape = RoundedCornerShape(percent = 50)) {
                Text(stringResource(Res.string.roaster_detail_back))
            }
        }
    }
}

@Preview @Composable private fun RoasterDetailErrorLightPreview() = CoffeePeekTheme(darkTheme = false) {
    androidx.compose.material3.Surface { RoasterDetailError({}, {}) }
}

@Preview @Composable private fun RoasterDetailErrorDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    androidx.compose.material3.Surface { RoasterDetailError({}, {}) }
}
