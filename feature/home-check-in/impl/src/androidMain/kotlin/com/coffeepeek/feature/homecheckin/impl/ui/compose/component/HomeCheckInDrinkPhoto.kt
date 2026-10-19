package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomeCheckInDrinkPhoto(
    url: String,
    enabled: Boolean,
    imageContent: @Composable (String, String, Modifier, Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    val openLabel = stringResource(R.string.home_check_in_open_drink_photo)
    val description = stringResource(R.string.home_check_in_drink_photo)
    Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        .clickable(enabled = enabled, role = Role.Button, onClickLabel = openLabel, onClick = onOpen)
        .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) imageContent(url, description, Modifier.fillMaxSize().padding(12.dp), true)
    }
}

@PreviewLightDark @Composable private fun HomeCheckInUnavailableDrinkPhotoPreview() = CoffeePeekTheme {
    HomeCheckInDrinkPhoto("", true, { _, _, _, _ -> }, {})
}

@PreviewLightDark @Composable private fun HomeCheckInDrinkPhotoPreview() = CoffeePeekTheme {
    HomeCheckInDrinkPhoto("fixture-cutout", true, { _, description, modifier, _ ->
        Box(modifier, contentAlignment = Alignment.Center) { Icon(CoffeePeekIcons.Coffee, description, Modifier.size(64.dp)) }
    }, {})
}
