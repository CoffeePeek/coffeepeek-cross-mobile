package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

interface CoffeePeekNativeOperationStatusPresenter {
    @Composable fun Present(message: String): Boolean
}

val LocalCoffeePeekNativeOperationStatusPresenter = staticCompositionLocalOf<CoffeePeekNativeOperationStatusPresenter?> { null }

/** Nonmodal feedback. It does not trap back/cancel behind a disabled alert. */
@Composable
fun CoffeePeekOperationStatus(message: String, bottomClearance: androidx.compose.ui.unit.Dp) {
    if (LocalCoffeePeekNativeOperationStatusPresenter.current?.Present(message) == true) return
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = CoffeePeekDimens.spacing4,
        vertical = bottomClearance + CoffeePeekDimens.spacing4), contentAlignment = Alignment.BottomCenter) {
        OperationStatusContent(message)
    }
}

@Composable
private fun OperationStatusContent(message: String) {
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.widthIn(max = 440.dp), shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
        color = colors.surface, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Row(Modifier.padding(horizontal = CoffeePeekDimens.spacing5, vertical = CoffeePeekDimens.spacing4).semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            CoffeePeekLoader(size = CoffeePeekDimens.loaderButton, contentDescription = "")
            BasicText(message, style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface, fontWeight = FontWeight.Medium))
        }
    }
}

@Preview @Composable private fun CoffeePeekOperationStatusLightPreview() = CoffeePeekTheme(darkTheme = false) {
    OperationStatusContent("Сохраняем изменения…")
}
@Preview @Composable private fun CoffeePeekOperationStatusDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    OperationStatusContent("Сохраняем изменения…")
}
