package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.AppButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.impl.resources.Res
import com.coffeepeek.feature.favorites.impl.resources.favorites_retry
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FavoritesMessage(text: String, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
            onRetry?.let { AppButton(stringResource(Res.string.favorites_retry), it) }
        }
    }
}

@Preview @Composable private fun FavoritesMessageLightPreview() = CoffeePeekTheme(darkTheme = false) {
    FavoritesMessage("Не удалось загрузить избранное", {})
}
@Preview @Composable private fun FavoritesMessageDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    FavoritesMessage("Не удалось загрузить избранное", {})
}
