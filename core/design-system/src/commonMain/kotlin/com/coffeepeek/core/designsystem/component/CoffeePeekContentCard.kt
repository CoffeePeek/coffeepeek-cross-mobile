package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Existing outlined detail-section treatment, without business content or click policy. */
@Composable
fun CoffeePeekContentCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(CoffeePeekDimens.spacing4),
    content: @Composable ColumnScope.() -> Unit,
) {
    CoffeePeekCard(
        modifier = modifier,
        shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = 0.dp,
        contentPadding = contentPadding,
        content = content,
    )
}

@Preview @Composable private fun CoffeePeekContentCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekContentCard { Text("CoffeePeek") }
}
@Preview @Composable private fun CoffeePeekContentCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekContentCard { Text("CoffeePeek") }
}
