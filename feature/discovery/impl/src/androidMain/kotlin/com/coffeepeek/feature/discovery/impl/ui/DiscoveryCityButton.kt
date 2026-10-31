package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekTonalButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.R

@Composable
internal fun DiscoveryCityButton(cityName: String?, onClick: () -> Unit) {
    val label = cityName ?: stringResource(R.string.discovery_city_choose)
    val description = stringResource(R.string.discovery_city_change, label)
    CoffeePeekTonalButton(onClick, Modifier.heightIn(min = 48.dp).semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Icon(CoffeePeekIcons.Location, null, Modifier.size(18.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@PreviewLightDark @Composable private fun DiscoveryCityButtonPreview() = CoffeePeekTheme {
    DiscoveryCityButton("Минск", {})
}
