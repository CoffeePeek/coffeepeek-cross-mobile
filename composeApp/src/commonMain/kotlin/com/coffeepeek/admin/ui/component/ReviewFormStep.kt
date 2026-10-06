package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
internal fun ReviewFormStep(
    optional: Boolean = false,
    last: Boolean = false,
    content: @Composable () -> Unit,
) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxWidth()
            .semantics { if (optional) stateDescription = "Необязательный пункт" }
            .drawBehind {
                val x = 5.dp.toPx()
                val y = 10.dp.toPx()
                val stroke = 2.dp.toPx()
                drawLine(color, Offset(x, 0f), Offset(x, if (last) y else size.height), strokeWidth = stroke,
                    pathEffect = if (optional) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx(), 2.dp.toPx(), 4.dp.toPx())) else null)
                drawCircle(color, radius = 4.dp.toPx(), center = Offset(x, y),
                    style = if (optional) Stroke(stroke) else androidx.compose.ui.graphics.drawscope.Fill)
            }
            .padding(start = 22.dp, bottom = if (last) 0.dp else 24.dp),
    ) { content() }
}
