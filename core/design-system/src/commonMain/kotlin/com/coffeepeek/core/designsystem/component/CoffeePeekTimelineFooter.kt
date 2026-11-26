package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Affinity, optional feature actions and saved drink. Without [onLike], affinity is display-only. */
@Composable
fun CoffeePeekTimelineFooter(
    count: Int?, selected: Boolean, likesDescription: String, drink: String?, modifier: Modifier = Modifier,
    onLike: (() -> Unit)? = null, likeEnabled: Boolean = true,
    actions: (@Composable () -> Unit)? = null,
) {
    if (count == null && drink == null && actions == null) return
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        if (count != null) CoffeePeekLikeCount(count, selected, likesDescription, onClick = onLike, enabled = likeEnabled)
        Spacer(Modifier.weight(1f))
        actions?.invoke()
        drink?.let { CoffeePeekDrinkLabel(it, size = CoffeePeekDrinkLabelSize.Compact) }
    }
}

@Composable private fun TimelineFooterPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column {
        CoffeePeekTimelineFooter(3, true, "Лайки: 3", "Капучино")
        CoffeePeekTimelineFooter(3, true, "Убрать лайк: 3", "Капучино", onLike = {},
            actions = { CoffeePeekTextButton(onClick = {}) { Text("Опубликовать") } })
    }
}
@Preview @Composable private fun TimelineFooterLightPreview() = TimelineFooterPreview(false)
@Preview @Composable private fun TimelineFooterDarkPreview() = TimelineFooterPreview(true)
