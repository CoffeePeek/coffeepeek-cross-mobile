package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_retry
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShelfLoadError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        TextButton(onRetry) { Text(stringResource(Res.string.shelf_retry)) }
    }
}

@Composable
private fun ShelfLoadErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfLoadError("Не удалось загрузить полку", {}, Modifier.background(MaterialTheme.colorScheme.background))
}

@Preview @Composable private fun ShelfLoadErrorLightPreview() = ShelfLoadErrorPreview(false)
@Preview @Composable private fun ShelfLoadErrorDarkPreview() = ShelfLoadErrorPreview(true)
