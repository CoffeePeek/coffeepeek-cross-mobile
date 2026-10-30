package com.coffeepeek.admin.ui.component

import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.coffeepeek.core.designsystem.component.CoffeePeekContentSizedSheetLayout

@Composable
internal fun ContentSizedSheetLayout(
    maxContentHeight: Dp,
    onContentHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = CoffeePeekContentSizedSheetLayout(maxContentHeight, onContentHeightChanged, modifier, content)

@Composable
private fun ContentSizedSheetLayoutCompatibilityPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ContentSizedSheetLayout(300.dp, {}) { Text("Sheet content") }
}

@Preview @Composable private fun ContentSizedSheetLayoutCompatibilityLightPreview() = ContentSizedSheetLayoutCompatibilityPreview(false)
@Preview @Composable private fun ContentSizedSheetLayoutCompatibilityDarkPreview() = ContentSizedSheetLayoutCompatibilityPreview(true)
