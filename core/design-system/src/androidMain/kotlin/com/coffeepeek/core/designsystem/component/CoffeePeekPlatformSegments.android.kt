package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens

@Composable
internal actual fun CoffeePeekPlatformSegments(
    labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier, enabled: Boolean,
    minSegmentHeight: Dp,
) {
    Row(modifier.fillMaxWidth().heightIn(min = minSegmentHeight)
        .coffeePeekLiquidGlass(RoundedCornerShape(percent = 50), shadowElevation = 0.dp)
        .padding(horizontal = CoffeePeekDimens.spacing1).selectableGroup()) {
        labels.forEachIndexed { index, label ->
            CoffeePeekCapsuleSegment(label, selectedIndex == index, enabled,
                { onSelected(index) }, Modifier.weight(1f),
                minHeight = minSegmentHeight)
        }
    }
}
