package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.impl.R

@Composable
internal fun RoasterLoadingRow(modifier: Modifier = Modifier.padding(24.dp)) {
    Box(Modifier.fillMaxWidth().then(modifier), contentAlignment = Alignment.Center) {
        CoffeePeekLoader(stringResource(R.string.roaster_loading))
    }
}

@Composable
internal fun RoasterEmptyText(modifier: Modifier = Modifier) {
    Text(stringResource(R.string.roaster_empty), modifier = modifier, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@PreviewLightDark @Composable private fun RoasterListStatusPreview() = CoffeePeekTheme {
    androidx.compose.foundation.layout.Column(Modifier.padding(CoffeePeekDimens.spacing4)) {
        RoasterLoadingRow(); RoasterEmptyText(Modifier.padding(24.dp))
    }
}
