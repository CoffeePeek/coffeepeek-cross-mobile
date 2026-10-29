package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitch
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*

/** "Показать пачку": a copy of the chosen bag's [cardUrl] goes with the entry while [show] is on. */
@Composable
internal fun HomeCheckInEditorShelfBag(
    cardUrl: String,
    show: Boolean,
    enabled: Boolean,
    image: @Composable (url: String, Modifier) -> Unit,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).toggleable(show, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // The slot only bounds the card: the card keeps its own proportions, as in the bean picker.
        Box(Modifier.size(width = 36.dp, height = 46.dp), contentAlignment = Alignment.Center) {
            image(cardUrl, Modifier.clip(RoundedCornerShape(4.dp)))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.home_check_in_show_bag_title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(Res.string.home_check_in_show_bag_hint), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CoffeePeekSwitch(show, null, Modifier.clearAndSetSemantics {}, enabled)
    }
}

@Composable private fun HomeCheckInEditorShelfBagPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        HomeCheckInEditorShelfBag("https://preview.invalid/card", show = true, enabled = true,
            { _, modifier -> Box(modifier.size(34.dp, 46.dp).background(MaterialTheme.colorScheme.tertiary)) }, {})
    }
}
@Preview @Composable private fun HomeCheckInEditorShelfBagLightPreview() = HomeCheckInEditorShelfBagPreview(false)
@Preview @Composable private fun HomeCheckInEditorShelfBagDarkPreview() = HomeCheckInEditorShelfBagPreview(true)
