package com.coffeepeek.feature.contributions.impl.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekPlatformCoffeeIcon
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val locationPinShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    moveTo(width / 2, height)
    cubicTo(width * 0.36f, height * 0.78f, 0f, height * 0.55f, 0f, height * 0.39f)
    cubicTo(0f, height * 0.17f, width * 0.22f, 0f, width / 2, 0f)
    cubicTo(width * 0.78f, 0f, width, height * 0.17f, width, height * 0.39f)
    cubicTo(width, height * 0.55f, width * 0.64f, height * 0.78f, width / 2, height)
    close()
}

@Composable
internal fun ContributionLocationPin(modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.contributions_map_pin)
    Box(modifier.offset(y = (-28).dp).size(44.dp, 56.dp).shadow(6.dp, locationPinShape)
        .background(MaterialTheme.colorScheme.primary, locationPinShape)
        .semantics { contentDescription = label }.testTag("shop_location_pin"), contentAlignment = Alignment.TopCenter) {
        CoffeePeekPlatformCoffeeIcon(null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(top = 9.dp).size(24.dp))
    }
}

@Composable private fun PinPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) { ContributionLocationPin() } }
}
@Preview @Composable private fun ContributionLocationPinLightPreview() = PinPreview(false)
@Preview @Composable private fun ContributionLocationPinDarkPreview() = PinPreview(true)
