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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.impl.R

@Composable
internal fun RoasterDetailError(onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(CoffeePeekDimens.spacing4), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            Text(stringResource(R.string.roaster_detail_load_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onRetry, Modifier.height(CoffeePeekDimens.buttonHeight), shape = RoundedCornerShape(percent = 50)) {
                Text(stringResource(R.string.roaster_detail_retry))
            }
            OutlinedButton(onBack, Modifier.height(CoffeePeekDimens.buttonHeight), shape = RoundedCornerShape(percent = 50)) {
                Text(stringResource(R.string.roaster_detail_back))
            }
        }
    }
}

@PreviewLightDark @Composable private fun RoasterDetailErrorPreview() = CoffeePeekTheme {
    androidx.compose.material3.Surface { RoasterDetailError({}, {}) }
}
