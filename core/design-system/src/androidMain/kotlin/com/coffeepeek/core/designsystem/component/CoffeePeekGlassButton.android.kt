package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import dev.chrisbanes.haze.HazeState
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState

@Composable
actual fun CoffeePeekGlassButton(
    text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean,
    isLoading: Boolean, loadingDescription: String?, leadingIcon: CoffeePeekGlassButtonIcon?, hazeState: HazeState?,
) {
    val loadingLabel = if (isLoading) requireNotNull(loadingDescription) else null
    CompositionLocalProvider(LocalCoffeePeekGlassHazeState provides hazeState) {
        CoffeePeekTonalButton(onClick, modifier.semantics {
            if (loadingLabel != null) {
                stateDescription = loadingLabel
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
        }, enabled = enabled && !isLoading) {
            if (isLoading) CoffeePeekLoader(loadingLabel!!, Modifier.clearAndSetSemantics {}, size = 16.dp)
            else if (leadingIcon == CoffeePeekGlassButtonIcon.Check) Icon(CoffeePeekIcons.Check, null)
            if (isLoading || leadingIcon != null) Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
