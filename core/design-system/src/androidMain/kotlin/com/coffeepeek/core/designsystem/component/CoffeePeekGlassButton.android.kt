package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import dev.chrisbanes.haze.HazeState

@Composable
actual fun CoffeePeekGlassButton(
    text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean,
    isLoading: Boolean, loadingDescription: String?, leadingIcon: CoffeePeekGlassButtonIcon?, hazeState: HazeState?,
) {
    val active = enabled && !isLoading
    val loadingLabel = if (isLoading) requireNotNull(loadingDescription) else ""
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 1f else 0.38f)
    Row(modifier.height(CoffeePeekDimens.buttonHeight)
        .coffeePeekLiquidGlass(CircleShape, hazeState, shadowElevation = 8.dp)
        .clickable(enabled = active, role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) {
            if (isLoading) {
                stateDescription = loadingLabel
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
        }
        .padding(horizontal = CoffeePeekDimens.spacing4),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        if (isLoading) CoffeePeekLoader(loadingLabel, Modifier.clearAndSetSemantics {}, size = 16.dp, strokeWidth = 2.dp)
        else when (leadingIcon) {
            CoffeePeekGlassButtonIcon.Check -> Icon(CoffeePeekIcons.Check, null, Modifier.size(16.dp), tint = color)
            null -> Unit
        }
        Text(text, color = color, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
