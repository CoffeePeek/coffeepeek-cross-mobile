package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekEmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Box(Modifier.size(CoffeePeekDimens.spacing16).background(colors.primary.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
            Image(icon, null, Modifier.size(CoffeePeekDimens.spacing8), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
        }
        BasicText(title, Modifier.fillMaxWidth().semantics { heading() }, style = MaterialTheme.typography.headlineSmall.copy(color = colors.onSurface, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center))
        BasicText(message, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceVariant, textAlign = TextAlign.Center))
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(CoffeePeekDimens.spacing1))
            CoffeePeekButton(actionLabel, onAction)
        }
    }
}

@Preview @Composable private fun CoffeePeekEmptyStateLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekEmptyState("Список пуст", "Здесь появятся записи.", CoffeePeekIcons.VisibilityOff)
}
@Preview @Composable private fun CoffeePeekEmptyStateDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekEmptyState("Список пуст", "Здесь появятся записи.", CoffeePeekIcons.VisibilityOff)
}
