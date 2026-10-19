package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Read-only photo marker: glass appearance without a disabled/fictional toggle. */
@Composable
fun CoffeePeekLikeBadge(selected: Boolean, contentDescription: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(32.dp).coffeePeekLikeBackground(CoffeePeekLikeButtonStyle.Glass, CircleShape),
            contentAlignment = Alignment.Center) {
            CoffeePeekLikeIcon(selected, contentDescription, overImage = true)
        }
    }
}

@Preview @Composable private fun CoffeePeekLikeBadgeLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekLikeBadge(true, "Saved")
}
@Preview @Composable private fun CoffeePeekLikeBadgeDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekLikeBadge(true, "Saved")
}
