package com.coffeepeek.feature.community.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.community.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CommunitySections(leadersSelected: Boolean, onSelected: (Boolean) -> Unit,
    hasLeaders: Boolean, headerAction: @Composable () -> Unit) {
    val feed = stringResource(Res.string.community_title)
    val leaders = stringResource(Res.string.community_leaders)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (hasLeaders) CoffeePeekCapsuleSegmentedControl(listOf(false, true), leadersSelected,
            { if (it) leaders else feed }, onSelected, Modifier.weight(1f))
        else Text(feed, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
        headerAction()
    }
}

@Preview @Composable private fun CommunitySectionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CommunitySections(false, {}, true, {})
}
@Preview @Composable private fun CommunitySectionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CommunitySections(true, {}, true, {})
}

@Preview @Composable private fun CommunityFeedOnlyLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CommunitySections(false, {}, false, {})
}
@Preview @Composable private fun CommunityFeedOnlyDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CommunitySections(false, {}, false, {})
}
