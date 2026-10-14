package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Presentation-only form rail; the caller owns ordering, validation and localized descriptions. */
@Composable
fun CoffeePeekFormStep(
    optional: Boolean = false,
    last: Boolean = false,
    isError: Boolean = false,
    optionalDescription: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(modifier.fillMaxWidth()
        .semantics { if (optional) optionalDescription?.let { stateDescription = it } }
        .drawBehind {
            val x = 5.dp.toPx()
            val y = 10.dp.toPx()
            val stroke = 2.dp.toPx()
            drawLine(color, Offset(x, 0f), Offset(x, if (last) y else size.height), strokeWidth = stroke,
                pathEffect = if (optional) PathEffect.dashPathEffect(
                    floatArrayOf(8.dp.toPx(), 4.dp.toPx(), 2.dp.toPx(), 4.dp.toPx())) else null)
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(x, y),
                style = if (optional) Stroke(stroke) else Fill)
        }.padding(start = 22.dp, bottom = if (last) 0.dp else 24.dp)) { content() }
}

@Composable
private fun CoffeePeekFormStepPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.padding(16.dp)) {
        CoffeePeekFormStep { Text("Required field") }
        CoffeePeekFormStep(optional = true, last = true, optionalDescription = "Optional field") { Text("Optional field") }
    }
}
@Preview @Composable private fun CoffeePeekFormStepLightPreview() = CoffeePeekFormStepPreview(false)
@Preview @Composable private fun CoffeePeekFormStepDarkPreview() = CoffeePeekFormStepPreview(true)
