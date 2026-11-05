package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIllustrations
import com.coffeepeek.core.designsystem.resources.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The established shared service-failure illustration, wording and content-width retry. */
@Composable
fun CoffeePeekLoadErrorContent(onRetry: () -> Unit, modifier: Modifier = Modifier,
    retryLabel: String = stringResource(Res.string.coffeepeek_load_error_retry)) {
    Column(modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Image(painterResource(CoffeePeekIllustrations.LoadError), null, Modifier.size(120.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.coffeepeek_load_error_title), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(Res.string.coffeepeek_load_error_message), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        CoffeePeekButton(retryLabel, onRetry, fillWidth = false, minHeight = ButtonDefaults.MinHeight)
    }
}

@Preview @Composable private fun CoffeePeekLoadErrorContentLightPreview() = CoffeePeekTheme(darkTheme = false) { CoffeePeekLoadErrorContent({}) }
@Preview @Composable private fun CoffeePeekLoadErrorContentDarkPreview() = CoffeePeekTheme(darkTheme = true) { CoffeePeekLoadErrorContent({}) }
