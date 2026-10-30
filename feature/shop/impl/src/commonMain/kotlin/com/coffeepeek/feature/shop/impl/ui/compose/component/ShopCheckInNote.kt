package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_read_more
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_collapse_note
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInNote(checkInId: String, note: String, modifier: Modifier = Modifier,
    fullVersion: Boolean = false) {
    var expanded by rememberSaveable(checkInId, note) { mutableStateOf(false) }
    var overflows by remember(note) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(note, style = MaterialTheme.typography.bodyLarge,
            maxLines = if (fullVersion || expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.animateContentSize(),
            onTextLayout = { if (!expanded && !fullVersion) overflows = it.hasVisualOverflow })
        if (!fullVersion && (overflows || expanded)) TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) Res.string.shop_checkins_collapse_note
                else Res.string.shop_checkins_read_more))
        }
    }
}

@Preview @Composable private fun ShopCheckInNoteLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInNote("preview", "Вкусный кофе и приятная атмосфера. ".repeat(10))
}
@Preview @Composable private fun ShopCheckInNoteDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInNote("preview", "Вкусный кофе и приятная атмосфера.")
}
