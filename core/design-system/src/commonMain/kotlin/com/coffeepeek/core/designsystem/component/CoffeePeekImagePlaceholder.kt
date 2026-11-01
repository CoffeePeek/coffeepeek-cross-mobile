package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_image_placeholder_brand
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Existing application brand fallback; image-specific accessibility text belongs to the caller. */
@Composable
fun CoffeePeekImagePlaceholder(
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 18.sp,
    contentDescription: String? = null,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(modifier.fillMaxSize().background(if (dark) Color.White else Color.Black)
        .semantics { contentDescription?.let { this.contentDescription = it } },
        contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.coffeepeek_image_placeholder_brand),
            color = if (dark) Color.Black else Color.White,
            fontSize = labelSize, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp,
            textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Preview @Composable private fun CoffeePeekImagePlaceholderLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekImagePlaceholder(Modifier.height(300.dp), labelSize = 24.sp)
}
@Preview @Composable private fun CoffeePeekImagePlaceholderDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekImagePlaceholder(Modifier.height(300.dp), labelSize = 24.sp)
}
