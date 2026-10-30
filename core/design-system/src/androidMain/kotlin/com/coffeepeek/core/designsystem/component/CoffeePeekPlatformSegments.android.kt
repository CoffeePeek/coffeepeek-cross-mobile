package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass

@Composable
internal actual fun CoffeePeekPlatformSegments(
    labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier, enabled: Boolean,
) {
    Row(modifier.fillMaxWidth().coffeePeekLiquidGlass(RoundedCornerShape(percent = 50), shadowElevation = 0.dp)
        .padding(4.dp).selectableGroup()) {
        labels.forEachIndexed { index, label ->
            CoffeePeekCapsuleSegment(label, selectedIndex == index, enabled,
                { onSelected(index) }, Modifier.weight(1f))
        }
    }
}
