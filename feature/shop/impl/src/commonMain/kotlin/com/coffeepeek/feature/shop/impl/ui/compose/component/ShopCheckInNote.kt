package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.theme.reducedMotionDuration
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_read_more
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_collapse_note
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInNote(checkInId: String, note: String, modifier: Modifier = Modifier,
    fullVersion: Boolean = false) {
    var expanded by rememberSaveable(checkInId, note) { mutableStateOf(false) }
    var overflows by remember(note) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(note, style = MaterialTheme.typography.bodyLarge,
            maxLines = if (fullVersion || expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.animateContentSize(tween(reducedMotionDuration(200))),
            onTextLayout = { if (!expanded && !fullVersion) overflows = it.hasVisualOverflow })
        if (!fullVersion && (overflows || expanded)) TextButton(
            onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
            Text(stringResource(if (expanded) Res.string.shop_checkins_collapse_note
                else Res.string.shop_checkins_read_more))
        }
    }
}

@Preview @Composable private fun ShopCheckInNoteLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInNote("preview", "Вкусный кофе и приятная атмосфера. ".repeat(10))
}
@Preview @Composable private fun ShopCheckInNoteDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInNote("preview", "Вкусный кофе и приятная атмосфера. ".repeat(10))
}
