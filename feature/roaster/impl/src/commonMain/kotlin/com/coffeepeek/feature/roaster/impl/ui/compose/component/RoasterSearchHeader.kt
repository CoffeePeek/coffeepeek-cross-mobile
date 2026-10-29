package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCircularBackButton
import com.coffeepeek.core.designsystem.component.CoffeePeekNativeSearchField
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.api.RoasterExposure
import com.coffeepeek.feature.roaster.api.RoasterScreenAnalytics
import com.coffeepeek.feature.roaster.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun RoasterSearchHeader(query: String, onQueryChange: (String) -> Unit, onCancel: () -> Unit,
    analytics: RoasterScreenAnalytics) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val cancel = {
        focus.clearFocus(force = true)
        keyboard?.hide()
        onCancel()
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CoffeePeekCircularBackButton(cancel, stringResource(Res.string.roaster_search_back),
            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
        val modifier = analytics.exposure(Modifier.weight(1f), RoasterExposure.Search)
        val placeholder = stringResource(Res.string.roaster_search_placeholder)
        if (!CoffeePeekNativeSearchField(query, onQueryChange, placeholder, {}, cancel,
                modifier.height(CoffeePeekDimens.buttonHeight))) {
            CoffeePeekSearchField(query, onQueryChange, placeholder, stringResource(Res.string.roaster_search_clear),
                modifier = modifier, clearButtonSize = 28.dp)
        }
    }
}

@Preview @Composable private fun RoasterSearchHeaderLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterSearchHeader("Roast", {}, {}, RoasterScreenAnalytics.None)
}
@Preview @Composable private fun RoasterSearchHeaderDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterSearchHeader("", {}, {}, RoasterScreenAnalytics.None)
}
